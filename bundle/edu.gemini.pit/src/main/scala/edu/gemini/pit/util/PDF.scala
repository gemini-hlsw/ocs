package edu.gemini.pit.util

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

import scalaz._
import Scalaz._
import edu.gemini.pit.ui._

/** Utility functions for handling PDF files. */
object PDF {

  private lazy val Magic = ByteBuffer.wrap(Array[Byte]('%', 'P', 'D', 'F', '-'))

  /* Returns true if this file, relative to an optional associated file if f isn't absolute. */
  def isPDF(assoc:Option[File], f: File): Boolean = if (f.isAbsolute)
      isPDF(f) || isThereRelativePDF(assoc, f)
    else
      isThereRelativePDF(assoc, f)

  /* Attempts to return the location of the PDF whether it is absolute or relative */
  def relocatedPdf(assoc:Option[File], f: Option[File]):Option[File] = {
    f flatMap {
      case p if p.isAbsolute() && isPDF(p) => f
      case p if isThereRelativePDF(assoc, p) => transformNameToRelative(assoc, new File(p.getName))
      case _ => f
    }
  }

  /* Converts the path of f to be in the same dir as assoc */
  private def transformNameToRelative(assoc: Option[File], f: File): Option[File] = {
    assoc.map(_.getParentFile).map(new File(_, f.getPath))
  }

  /* Returns true if this file appears to be a PDF file. */
  private def isPDF(f: File): Boolean = try {
    def checkIsPDF = {
      val buf = ByteBuffer.allocate(Magic.capacity)
      new FileInputStream(f).getChannel.read(Array(buf))
      buf.flip == Magic
    }
    f.exists() && checkIsPDF
  } catch {
    case _:Exception => false
  }

  /* Returns true if there is a pdf file at the same dir as the associated file */
  private def isThereRelativePDF(assoc:Option[File], f: File): Boolean = {
    ~transformNameToRelative(assoc, new File(f.getName)).map(isPDF)
  }

}