package edu.gemini.p1monitor

import config.MonitoredDirectory
import java.io.File

trait DirListener {
  def dirChanged(evt: DirEvent)
}

case class DirEvent(dir: MonitoredDirectory, newFiles: Traversable[File], deletedFiles: Traversable[File], modifiedFiles: Traversable[File])

