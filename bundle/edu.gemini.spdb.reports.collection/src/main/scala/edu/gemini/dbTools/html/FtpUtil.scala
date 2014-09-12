package edu.gemini.dbTools.html

import java.io.File
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import edu.gemini.util.ssh.SftpSession

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object FtpUtil {
  /*
  class FileMapping(localFile: File) {
    def getLocalFile: File = { localFile }
  }
  object FileMapping {
    val EMPTY_ARRAY: Array[FileMapping] = {}
  }
  */

  def sendFile(log: Logger, localFile: File, props: FtpProps): Unit = {
    val mappings = new java.util.ArrayList[File](1)
    mappings.add(localFile)
    sendFiles(log, mappings, props)
  }

  def sendFiles(log: Logger, files: java.util.List[File], props: FtpProps): Unit = {
    def send(sftpSession: SftpSession): Try[Unit] = {
      val fs = files.asScala
      val success: Try[Unit] = Success(())
      (success/:fs) { (t,file) =>
        t.flatMap { _ =>
          for {
//            _ <- sftpSession.localCd(file.getParentFile)
            _ <- sftpSession.copyLocalToRemote(file, ".", overwrite = true)
          } yield ()
        }
      }
    }

    log.info("sftp %s session initiating".format(props.getConfig))
    val startTime: Long = System.currentTimeMillis
    val result = for {
      sftpSession <- SftpSession.connect(props.getConfig)
      _ <- sftpSession.remoteMkDir(props.dir, createParentDirs = true)
      _ <- sftpSession.remoteCd(props.dir)
      _ <- send(sftpSession)
    } yield sftpSession.disconnect()
    val endTime: Long = System.currentTimeMillis

    if (result.isSuccess) log.info("sftp %s transfer done: %d ms".format(props.getConfig, endTime - startTime))
    else {
      val ex: Exception = result.asInstanceOf[Failure[Unit]].exception.asInstanceOf[Exception]
      log.log(Level.SEVERE, "Error while transferring files:", ex)
      throw new IOException(ex)
    }
  }
}
