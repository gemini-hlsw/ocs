package edu.gemini.phase2.skeleton.auxfile

import java.io.File

object FileError {
  def apply(file: File, msg: String): FileError =
    FileError(file, new RuntimeException(msg))
}

case class FileError(file: File, exception: Exception)