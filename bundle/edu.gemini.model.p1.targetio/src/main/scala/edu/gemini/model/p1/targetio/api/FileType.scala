package edu.gemini.model.p1.targetio.api

/**
 * File types supported by the targetio library.
 */
sealed trait FileType {
  def extension: String
  val format: FileFormat = Text
  def value() = "%s (%s)".format(extension, description)
  def description: String
}


object FileType {
    /** ASCII table. */
  // UNSUPPORTED FOR NOW
  //case object Ascii extends FileType {
  //  val extension = "txt"
  //}

  /** Comma-separated values. */
  case object Csv extends FileType {
    val extension = "csv"
    val description = "Comma Separated Values"
  }

  /** FITS table. */
  case object Fits extends FileType {
    val extension = "fits"
    override val format = Binary
    val description = "FITS table"
  }

  /** Tab-separated table. */
  case object Tst extends FileType {
    val extension = "tst"
    val description = "Tab Separated Text"
  }

  /** VO Table. */
  case object Vo extends FileType {
    val extension = "xml"
    val description = "VO Table"
  }

  val values: List[FileType] = List(Csv, Fits, Tst, Vo)

  def fromExtension(ext: String) = values.find(_.extension == ext)
}




