package edu.gemini.p1monitor

import java.util.{TimerTask, Timer}

import config.MonitoredDirectory
import java.io.File
import collection.mutable
import java.util.logging.Logger
import scala.sys.process._
import scala.util.Try

class DirScanner(dir: MonitoredDirectory) {
  val LOG = Logger.getLogger(classOf[DirScanner].getName)

  val files: mutable.Map[String, FileRecord] = new mutable.HashMap[String, FileRecord]()

  case class Stop(listener: DirListener)

  val timer = new Timer

  def startMonitoring(listener: DirListener) {
    fullScan(listener)
    timer.schedule(new TimerTask {
      override def run(): Unit = {
        update(listener)
      }
    }, 5000, 10000)
  }

  def stopMonitoring() {
    timer.cancel()
  }

  private def fullScan(listener: DirListener) {
    createDirIfNeeded()
    LOG.info(s"Run a full scan on directory ${dir.dir}")
    files.clear()
    Option(dir.dir.listFiles()) match {
      case Some(list) =>
        list.foreach {
          file => {
            files += ((file.getName, new FileRecord(file, file.lastModified())))
          }
        }
      case None =>
        // This may happen if e.g. the permissions of the monitored dirs aren't correct
        // we'll consider this a fatal error
        LOG.severe(s"Cannot read directory ${dir.dir}")
        sys.exit(1)
    }
  }

  private def executeAction(cmd: Seq[String], errorMsg: => String): Unit = {
    Some(cmd.mkString(" ").!).filter(_ != 0).foreach(_ => LOG.warning(errorMsg))
  }

  def createDirIfNeeded() {
    if (!dir.dir.exists()) {
      LOG.info(s"Directory ${dir.dir} is not present, attempt to create and set permissions")
      if (!dir.dir.mkdirs()) {
        LOG.warning(s"Cannot create directory ${dir.dir}")
      } else {
        LOG.info(s"Setting permissions and ownership of ${dir.dir}")
        Some(dir.dir).foreach(d => executeAction(Seq("chmod", "ag+rw", d.getAbsolutePath), s"Cannot set proper permissions on ${dir.dir}"))
        dir.username.foreach(u => executeAction(Seq("chown", u, dir.dir.getAbsolutePath), s"Failed to set user $u to dir ${dir.dir.getAbsolutePath}"))
        dir.group.foreach(g => executeAction(Seq("chgrp", g, dir.dir.getAbsolutePath), s"Failed to set user $g to dir ${dir.dir.getAbsolutePath}"))
      }
    }
  }

  private def update(listener: DirListener) {
    var updatedFiles: List[File] = Nil
    var deletedFiles: List[File] = Nil
    var newFiles: List[File] = Nil

    Option(dir.dir.listFiles()) match {
      case Some(list) =>
        list.foreach {
          file => {
            files.get(file.getName).foreach {
              //if file is updated more recently than info we had, add to updatedFiles
              case f: FileRecord if f.lastUpdated < file.lastModified() =>
                files += ((file.getName, new FileRecord(file, file.lastModified()))) //update our copy
                updatedFiles = updatedFiles :+ file
              case _ =>
            }
            //if file wasn't stored, add it to newFiled
            if (files.get(file.getName).isEmpty) {
              files += ((file.getName, new FileRecord(file, file.lastModified()))) //update our copy
              newFiles = newFiles :+ file
            }
          }
        }
      case None       =>
        // This may happen if e.g. the permissions of the monitored dirs aren't correct
        // we'll consider this a fatal error
        LOG.severe(s"Cannot read directory ${dir.dir}")
        sys.exit(1)
    }
    val removed = files.keySet -- (dir.dir.listFiles() map {
      f => f.getName
    })
    //if file is stored, but not on new list, add it to deletedFiles
    if (removed.nonEmpty) {
      deletedFiles = removed.map {
        fileName => files.remove(fileName).get.file //remove file and add to deleted
      }.toList
    }
    if (newFiles.nonEmpty || updatedFiles.nonEmpty || deletedFiles.nonEmpty) {
      listener.dirChanged(new DirEvent(dir, newFiles, deletedFiles, updatedFiles))
    }
    LOG.fine(s"Dir scan produced: newFiles: $newFiles, updatedFiles: $updatedFiles, deletedFiles: $deletedFiles")
  }
}


case class FileRecord(file: File, lastUpdated: Long)
