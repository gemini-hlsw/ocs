package edu.gemini.p1monitor

import config.MonitoredDirectory
import java.io.File
import collection.mutable
import actors.Actor._
import actors.{TIMEOUT, Actor}
import java.util.logging.Logger
import scala.sys.process._

class DirScanner(dir: MonitoredDirectory) {
  val LOG = Logger.getLogger(classOf[DirScanner].getName)

  val files: mutable.Map[String, FileRecord] = new mutable.HashMap[String, FileRecord]()
  var updater: Actor = _

  case class Stop(listener: DirListener)

  def startMonitoring(listener: DirListener) {
    fullScan(listener)
    updater = actor {
      var keepGoing = true
      while (keepGoing) {
        update(listener)
        receiveWithin(10000) {
          case Stop => keepGoing = false
          case TIMEOUT =>
        }
      }
    }
  }

  def stopMonitoring() {
    updater ! Stop
  }

  private def fullScan(listener: DirListener) {
    createDirIfNeeded()
    LOG.info("Run a full scan on directory %s".format(dir.dir))
    files.clear()
    dir.dir.listFiles() foreach {
      file => {
        files += ((file.getName, new FileRecord(file, file.lastModified())))
      }
    }
  }

  private def executeAction(cmd: Seq[String], errorMsg: => String) {
    Some(cmd.mkString(" ").!).filter(_ != 0).foreach(_ => LOG.warning(errorMsg))
  }

  def createDirIfNeeded() {
    if (!dir.dir.exists()) {
      LOG.info("Directory %s is not present, attempt to create and set permissions".format(dir.dir))
      if (!dir.dir.mkdirs()) {
        LOG.warning("Cannot create directory %s".format(dir.dir))
      } else {
        LOG.info("Setting permissions and ownership of %s".format(dir.dir))
        Some(dir.dir).
          map(d => executeAction(Seq("chmod", "ag+rw", d.getAbsolutePath), "Cannot set proper permissions on %s".format(dir.dir)))
        dir.username.
          map(u => executeAction(Seq("chown", u, dir.dir.getAbsolutePath), "Failed to set user %s to dir %s".format(dir.username.get, dir.dir.getAbsolutePath)))
        dir.group.
          map(g => executeAction(Seq("chgrp", g, dir.dir.getAbsolutePath), "Failed to set user %s to dir %s".format(dir.group.get, dir.dir.getAbsolutePath)))
      }
    }
  }

  private def update(listener: DirListener) {
    var updatedFiles: List[File] = Nil
    var deletedFiles: List[File] = Nil
    var newFiles: List[File] = Nil

    dir.dir.listFiles() foreach {
      file => {
        files.get(file.getName) foreach {
          //if file is updated more recently than info we had, add to updatedFiles
          case f: FileRecord if f.lastUpdated < file.lastModified() => {
            files += ((file.getName, new FileRecord(file, file.lastModified()))) //update our copy
            updatedFiles = updatedFiles :+ file
          }
          case _ =>
        }
        //if file wasn't stored, add it to newFiled
        if (!files.get(file.getName).isDefined) {
          files += ((file.getName, new FileRecord(file, file.lastModified()))) //update our copy
          newFiles = newFiles :+ file
        }
      }
    }
    val removed = files.keySet -- (dir.dir.listFiles() map {
      f => f.getName
    })
    //if file is stored, but not on new list, add it to deletedFiles
    if (!removed.isEmpty) {
      deletedFiles = removed.map {
        fileName => files.remove(fileName).get.file //remove file and add to deleted
      }.toList
    }
    if (!newFiles.isEmpty || !updatedFiles.isEmpty || !deletedFiles.isEmpty) {
      listener.dirChanged(new DirEvent(dir, newFiles, deletedFiles, updatedFiles))
    }
    LOG.fine("Dir scan produced: newFiles: %s, updatedFiles: %s, deletedFiles: %s".format(newFiles.toString(), updatedFiles.toString(), deletedFiles.toString()))
  }
}


case class FileRecord(file: File, lastUpdated: Long)