package edu.gemini.spdb.cron

import java.io.File

/**
 * ADT to differentiate temporary vs permanent storage roots
 */
sealed trait Storage extends Product with Serializable {
  def dir: File

  def newFile(name: String): File =
    new File(dir, name)
}

object Storage {
  final case class Temp(dir: File) extends Storage
  final case class Perm(dir: File) extends Storage
}