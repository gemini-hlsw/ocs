package edu.gemini.util.ssh

import java.io.File
import scala.util.Try

/**
 * An sftp session, comprising the connection and methods to copy files to and from remote servers.
 * @author sraaphor
 */
trait SftpSession {

  /**
   * Disconnect the session.
   * IMPORTANT: If disconnect is NOT called, the code MAY hang. Additionally, a call to any of the methods
   *            in this class after a call to disconnect WILL result in failure.
   */
  def disconnect(): Unit


  /**
   * Change directory on the remote server.
   * If the directory does not exist or is not accessible, an SshException is thrown.
   * @param remotePath The path on the remote server.
   */
  def remoteCd(remotePath: String): Try[Unit]

  /**
   * Change directory on the local server.
   * If the directory does not exist or is not accessible, an SshException is thrown.
   * @param localPath The path on the local server.
   */
  def localCd(localPath: File): Try[Unit]

  /**
   * Removes one or several files on the remote server.
   * @param remotePath A remote glob pattern of files to be removed relative to the current directory.
   */
  def remoteRm(remotePath: String): Try[Unit]

  /**
   * Removes one or several directories on the remote server.
   * @param remotePath A remote glob pattern of directories to be removed relative to the current directory.
   */
  def remoteRmdir(remotePath: String): Try[Unit]

  /**
   * Change the group of one or more remote files.
   * @param gid        Integer representing the group.
   * @param remotePath A remote glob pattern of files to be reowned relative to the current directory.
   */
  def remoteChgrp(gid: Int, remotePath: String): Try[Unit]

  /**
   * Change the owner of one or more remote files.
   * @param uid        Integer representing the owner.
   * @param remotePath A remote glob pattern of files to be reowned relative to the current directory.
   */
  def remoteChown(uid: Int, remotePath: String): Try[Unit]

  /**
   * Change the permissions of one or more remote files.
   * @param prm         A new permission pattern, given as an integer.
   * @param remotePath  A remote glob pattern of files whose permissions should be altered relative to the current directory.
   */
  def remoteChmod(prm: Int, remotePath: String): Try[Unit]

  /**
   * Upload a file from the local server to the remote server using overwrite mode.
   * @param localPath  Path to the file on the local server, absolute or relative to the current local directory.
   * @param remotePath The remote destination file name or directory, absolute or relative to the current remote directory.
   * @param overwrite whether to overwrite the remote file if it already exists (as opposed to failure)
   */
  def copyLocalToRemote(localPath: File, remotePath: String, overwrite: Boolean): Try[Unit]

  /**
   * Download a file from the remote server to the local server using overwrite mode.
   * @param remotePath The path to the file on the remote server, absolute or relative to the current remote directory.
   * @param localPath  The local destination file name or directory, absolute or relative to the current local directory.
   */
  def copyRemoteToLocal(remotePath: String, localPath: File): Try[Unit]

  /**
   * Rename a file or directory on the remote server.
   * @param oldPath The old path to the file, relative to the current remote directory.
   * @param newPath The new path to the file, relative to the current remote directory.
   * @param overwrite whether to overwrite the remote file if it already exists (as opposed to failure)
   */
  def remoteMv(oldPath: String, newPath: String, overwrite: Boolean): Try[Unit]

  /**
   * Return the absolute path to the current directory on the remote server.
   */
  def remotePwd(): Try[String]

  /**
   * Return the absolute path to the current directory on the local server.
   */
  def localPwd(): Try[File]

  def localMkDir(localPath: File, createParentDirs: Boolean = true): Try[Boolean]

  def localMv(oldPath: File, newPath: File): Try[Boolean]
  /**
   * Create a remote directory. If the method fails, an SshException is thrown. This can happen if the file does not contain
   * a valid path name, or if createParentDirs is false and one of the parent directories does not exist.
   * @param remotePath       The path (relative or absolute) of the remote directory to create.
   * @param createParentDirs A flag indicating whether or not the parent directories should be created if they do not exist.
   *                         Note that if this is true, it is akin to executing "mkdir -p".
   */
  def remoteMkDir(remotePath: String, createParentDirs: Boolean = true): Try[Unit]
}


object SftpSession {

  /**
   * Initialize and connect a Session, and then using that, a ChannelSftp, which we can then use to create the
   * SftpSession object.
   */
  def connect(config: SshConfig): Try[SftpSession] = {
    SshSession.connect(config).flatMap(JschSftpSession.connectChannel(config, _)).map(x => new JschSftpSession(config, x._1, x._2))
  }


  def withSession[T](config: SshConfig)(f: SftpSession => Try[T]): Try[T] = {
    val session = connect(config)
    try {
       session.flatMap(s => f(s))
    } finally {
      session.foreach { _.disconnect() }
    }
  }

  def copy(config: SshConfig, local: File, remotePath: String): Try[Unit] = {
    val tmpName = local.getName + ".tmp"
    withSession(config) { sftp =>
      for {
        _ <- sftp.remoteMkDir(remotePath, createParentDirs = true)
        _ <- sftp.remoteCd(remotePath)
        _ <- sftp.copyLocalToRemote(local, tmpName, overwrite = true)
        _ <- sftp.remoteMv(tmpName, local.getName, overwrite = true)
      } yield ()
    }
  }

  /**
   * A static method to establish an SftpSession and then transfer a file from a remote server to
   * a local server.
   * @param config     The sftp connection information.
   * @param remotePath The path to the file on the remote server (may be absolute or relative).
   * @param local      The path to the destination on the local server (may be a file or directory, absolute or relative).
   * @return           Success if successful, and Failure[exception] if failure.
   */
  def get(config: SshConfig, remotePath: String, local: File): Try[Unit] = {
    val localDir  = if (local.isDirectory) local.getAbsoluteFile else local.getAbsoluteFile.getParentFile
    val remoteFilename = new File(remotePath).getName
    val localFile = if (!(local.isDirectory)) local else new File(localDir, remoteFilename)
    val tmpLocalFile = new File(localDir, remoteFilename + ".tmp")

    withSession(config) { sftp =>
      for {
        // In this way, the parameter to localMkDir should NEVER be null.
        _ <- sftp.localMkDir(localDir)
        _ <- sftp.localCd(localDir)
        _ <- sftp.copyRemoteToLocal(remotePath, tmpLocalFile)
        _ <- sftp.localMv(tmpLocalFile, localFile)
      } yield()
    }
  }
}
