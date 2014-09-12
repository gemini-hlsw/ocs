package edu.gemini.util.ssh

import com.jcraft.jsch.{SftpException, ChannelSftp, Session}
import scala.util.{Try, Success, Failure}
import java.io.File
import java.util.logging.Logger


private[ssh] object JschSftpSession {
  val LOG = Logger.getLogger(classOf[JschSftpSession].getName)
  /**
   * Create and connect the ChannelSftp to the remote machine.
   * @param config  The configuration object for logging purposes.
   * @param session The com.jcraft.jsch.Session object that was created by the SshSession connect method.
   * @return        A pair containing the connected Session and ChannelSftp objects, for use with the SftpSession
   *                constructor.
   */
    def connectChannel(config: SshConfig, session: Session): Try[Pair[Session,ChannelSftp]] = {
      val tryChannel = Try {
        val channel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
        channel.connect()
        (session, channel)
      }

      // Log the results.
      tryChannel match {
        case Success(_) =>
          LOG.fine("SftpSession (%s) connection established".format(config))
        case Failure(ex) =>
          LOG.warning("SftpSession (%s) could not establish connection (reason: %s)".format(config, ex.getMessage))
      }

      tryChannel
    }


}

import JschSftpSession.LOG

/**
 * An sftp session, comprising the connection and methods to copy files to and from remote servers.
 * @author sraaphor
 * @param config The host configuration for establishing the ssh connection.
 */
private[ssh] class JschSftpSession (config: SshConfig, session: Session, channel: ChannelSftp) extends SftpSession  {
  assert(session.isConnected, "Session (%s) is not connected".format(config))
  assert(channel.isConnected, "Channel (%s) is not connected".format(config))

  /**
   * Disconnect the session.
   * IMPORTANT: If disconnect is NOT called, the code MAY hang. Additionally, a call to any of the methods
   *            in this class after a call to disconnect WILL result in failure.
   */
  def disconnect(): Unit = {
    channel.disconnect()
    SshSession.disconnect(config, session)
  }

  /**
   * A general framework for executing sftp commands that perform error handling and logging.
   * Takes two parameters, T (the type of the function parameters), and U, the function return type.
   * These are generally inferred, though, and do not require explicit definition.
   * For a single parameter function f: T => U, call runCommand(f)(t)(success, failure).
   * For a multiple parameter function f: T1,...,Tn => U, call runCommand((f _).tupled)((t1,...,tu))(success, failure).
   * @param u       The function with parameters of type T = [T1,...,Tn] and return type U to be executed.
   * @param message An informational string describing the operation attempted, used to build success / failure
   *                messages.
   * @tparam U      The return type of the function f.
   * @return        A Try[U] containing Success(f(params)) on success, and Failure(ex) with exception ex on failure.
   */
  private def execCommand[U](message: String)(u: => U): Try[U] = {
    Try {
      // We still put everything in a try ... catch block to convert the exceptions thrown from exceptions in the
      // JSch library to exceptions in this bundle so as to not expose JSch externally.
      try {
        val value = u
        LOG.fine("SftpSession (%s) %s successful".format(config, message))
        value
      } catch {
        case ex: SftpException => {
          LOG.warning("SftpSession (%s) %s failure (reason: %s)".format(config, message, ex.getMessage))
          throw new SshException("SftpSession (%s) %s failure (reason: %s)".format(config, message, ex.getMessage), ex, ex.id)
        }
        case ex: Exception => {
          LOG.warning("SftpSession (%s) %s failure (reason: %s)".format(config, message, ex.getMessage))
          throw new SshException("SftpSession (%s) %s failure (reason: %s)".format(config, message, ex.getMessage), ex, -1)
        }
      }
    }
  }

  /**
   * Change directory on the remote server.
   * If the directory does not exist or is not accessible, an SshException is thrown.
   * @param remotePath The path on the remote server.
   */
  def remoteCd(remotePath: String) = execCommand("remote cd %s".format(remotePath)) { channel.cd(remotePath) }

  /**
   * Change directory on the local server.
   * If the directory does not exist or is not accessible, an SshException is thrown.
   * @param localPath The path on the local server.
   */
  def localCd(localPath: File) = execCommand("local cd %s".format(localPath)) { channel.lcd(localPath.getPath) }

  /**
   * Removes one or several files on the remote server.
   * @param remotePath A remote glob pattern of files to be removed relative to the current directory.
   */
  def remoteRm(remotePath: String) =
    execCommand("remote rm %s".format(remotePath))(channel.rm(remotePath)).recover {
      case x: SshException if x.jschID == ChannelSftp.SSH_FX_NO_SUCH_FILE => ()
    }

  /**
   * Removes one or several directories on the remote server.
   * @param remotePath A remote glob pattern of directories to be removed relative to the current directory.
   */
  def remoteRmdir(remotePath: String) = {
    /* === ChannelSftp.rmdir ===
     * (Notes extending the utterly useless JSch API documentation.)
     * Generates SftpExceptions containing the following id values:
     * SSH_FX_NO_SUCH_FILE: The directory does not exist.
     * SSH_FX_PERMISSION_DENIED: User does not have permission to rmdir.
     * SSH_FX_FAILURE: The directory is nonempty.
     */
    execCommand("remote rmdir %s".format(remotePath))(channel.rmdir(remotePath)).recover {
      case x: SshException if x.jschID == ChannelSftp.SSH_FX_NO_SUCH_FILE => ()
    }
  }

  /**
   * Delete a local file.
   * @param localPath A path (relative or absolute) to the local file to delete.
   * @return Returns Success if the delete was successful, and Failure if permissions prohibited the deletion.
   *         The only exceptions generated by File.delete are SecurityExceptions, and we cannot recover from these.
   */
  private def localRm(localPath: File) = execCommand("local rm %s".format(localPath.getPath)){ localPath.delete() }

  def localMv(oldPath: File, newPath: File) =
    execCommand("local mv %s -> %s".format(oldPath.getPath, newPath.getPath)){ oldPath.renameTo(newPath) }

  /**
   * Change the group of one or more remote files.
   * @param gid        Integer representing the group.
   * @param remotePath A remote glob pattern of files to be reowned relative to the current directory.
   */
  def remoteChgrp(gid: Int, remotePath: String) = // execCommand((channel.chgrp _).tupled)((gid,remotePath))("remote chgrp file %s -> %d".format(remotePath, gid))
    execCommand("remote chgrp file %s -> %d".format(remotePath, gid))  { channel.chgrp(gid, remotePath) }

  /**
   * Change the owner of one or more remote files.
   * @param uid        Integer representing the owner.
   * @param remotePath A remote glob pattern of files to be reowned relative to the current directory.
   */
  def remoteChown(uid: Int, remotePath: String) =
    execCommand("remote chown file %s -> %d".format(remotePath, uid)){ channel.chown(uid, remotePath) }

  /**
   * Change the permissions of one or more remote files.
   * @param prm         A new permission pattern, given as an integer.
   * @param remotePath  A remote glob pattern of files whose permissions should be altered relative to the current directory.
   */
  def remoteChmod(prm: Int, remotePath: String) =
    execCommand("remote chmod file %s -> %d".format(remotePath, prm))  { channel.chmod(prm, remotePath) }

  private def rmBeforeWrite(fileName: String, remotePath: String, overwrite: Boolean): Unit =
    if (overwrite) {
      try {
        val stat = channel.stat(remotePath)
        if (stat.isDir && !fileName.isEmpty) {
          rmBeforeWrite(fileName, remotePath.stripSuffix("/") + "/" + fileName, overwrite)
        } else if (stat.isReg) {
          channel.rm(remotePath)
        }
      } catch {
        case ex: SftpException => // ignore, it will fail later if the file exists and cannot be overwritten
      }
    }

  /**
   * Upload a file from the local server to the remote server using overwrite mode.
   * @param localPath  Path to the file on the local server, absolute or relative to the current local directory.
   * @param remotePath The remote destination file name or directory, absolute or relative to the current remote directory.
   */
  def copyLocalToRemote(localPath: File, remotePath: String, overwrite: Boolean) =
    execCommand("local-to-remote cp %s -> %s".format(localPath.getPath, remotePath)) {
      rmBeforeWrite(localPath.getName, remotePath, overwrite)
      channel.put(localPath.getPath,remotePath)
    }

  /**
   * Download a file from the remote server to the local server using overwrite mode.
   * @param remotePath The path to the file on the remote server, absolute or relative to the current remote directory.
   * @param localPath  The local destination file name or directory, absolute or relative to the current local directory.
   */
  def copyRemoteToLocal(remotePath: String, localPath: File) =
    execCommand("remote-to-local cp %s -> %s".format(remotePath, localPath.getPath)) { channel.get(remotePath,localPath.getPath) }

  /**
   * Rename a file or directory on the remote server.
   * @param oldPath The old path to the file, relative to the current remote directory.
   * @param newPath The new path to the file, relative to the current remote directory.
   */
  def remoteMv(oldPath: String, newPath: String, overwrite: Boolean): Try[Unit] =
    execCommand("remote mv %s -> %s".format(oldPath, newPath)) {
      rmBeforeWrite(oldPath.reverse.takeWhile(_ != '/').reverse, newPath, overwrite)
      channel.rename(oldPath,newPath)
    }

  /**
   * Return the absolute path to the current directory on the remote server.
   */
  def remotePwd(): Try[String] = execCommand("remote pwd") { channel.pwd }

  /**
   * Return the absolute path to the current directory on the local server.
   */
  def localPwd(): Try[File] = execCommand("local pwd") { new File(channel.lpwd) }

  /**
   * Try to create a directory on the local machine. Fails only if security settings disallow it.
   * @param localPath        The local directory. Can represent a nonexistent heirarchy.
   * @param createParentDirs A flag indicating whether or not the parent directories should be created if they do not
   *                         exist. If this is true, it is equivalent to executing "mkdir -p".
   */
  def localMkDir(localPath: File, createParentDirs: Boolean = true) =
    execCommand("local mkdir %s".format(localPath)){
      if (createParentDirs) localPath.mkdirs()
      else localPath.mkdir()
    }

  /**
   * Create a remote directory. If the method fails, an SshException is thrown. This can happen if the file does not contain
   * a valid path name, or if createParentDirs is false and one of the parent directories does not exist.
   * @param remotePath       The path (relative or absolute) of the remote directory to create.
   * @param createParentDirs A flag indicating whether or not the parent directories should be created if they do not exist.
   *                         Note that if this is true, it is akin to executing "mkdir -p".
   */
  def remoteMkDir(remotePath: String, createParentDirs: Boolean = true): Try[Unit] = {
    // Convert to a File for easy management and store the current directory.
    val remoteDir: File = new File(remotePath)
    val originalDir: String = channel.pwd

    // Recursive method to drill down through remoteDir to the root directory, and then
    // recursively create all the parent directories of remoteDir.
    def mkCd(channel: ChannelSftp, dir: File, originalDir: String) {
      if (dir == null) {
        // Base case: we had a relative directory and we are now in the base directory; simply ignore this case.
      } else if (dir.getParentFile == null && dir.getPath.startsWith("/")) {
        // Base case: we are at the root directory.
        channel.cd("/")
      } else {
        // Recursive case: call recursively to generate all parent directories, and then try to create this directory.
        try {
          // Call recursively on the parent directory to generate all parent directories.
          mkCd(channel, dir.getParentFile, originalDir)

          // Now try to make this directory.
          channel.mkdir(dir.getName)
        } catch {
          case ex: SftpException =>
          // Do nothing: this may happen if the directory already exists, and further problems
          // will be detected in the next try block.
        }

        // Try to cd into the directory. If we fail, we were unable to create it.
        try {
          channel.cd(dir.getName)
        } catch {
          case ex: SftpException =>
            remoteCd(originalDir)
            throw new SshException("SftpSession (%s) could not remote mkdir %s of %s (reason: %s)".format(config, dir.getName, remoteDir, ex.getMessage))
        }
      }
    }

    Try {
      // If we are creating parent directories, we use the recursive function mkCd to create all parent directories.
      if (createParentDirs) {
        mkCd(channel, remoteDir, originalDir)
      }
      else {
        // Try directly to create this directory. We anticipate failure if it already exists, and allow for this.
        try {
          channel.mkdir(remoteDir.getPath)
        } catch {
          case ex: SftpException =>
          // Do nothing: this may happen if the directory already exists, and further problems
          // will be detected in the next try block.
        }

        // Try to cd into the directory. If we fail, we were unable to create it.
        try {
          channel.cd(remoteDir.getPath)
        } catch {
          case ex: SftpException =>
            throw new SshException("SftpSession (%s) could not mkdir %s (reason: %s)".format(config, remoteDir.getPath, ex.getMessage))
        } finally {
          remoteCd(originalDir)
        }
      }
    }
  }
}