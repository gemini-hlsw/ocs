package edu.gemini.model.p1.targetio

import api._
import uk.ac.starlink.table.{TableBuilder, StarTableWriter}
import uk.ac.starlink.fits.{FitsTableBuilder, FitsTableWriter}
import uk.ac.starlink.table.formats._
import uk.ac.starlink.votable.{VOTableBuilder, VOTableWriter}

import edu.gemini.model.p1.targetio.api.FileType._

package object table {
  def stilWriter(ftype: FileType): StarTableWriter =
    ftype match {
      case Csv   => new CsvTableWriter()
      case Fits  => new FitsTableWriter()
      case Tst   => new TstTableWriter()
      case Vo    => new VOTableWriter()
    }

  def stilBuilder(ftype: FileType): TableBuilder =
    ftype match {
      case Csv   => new CsvTableBuilder()
      case Fits  => new FitsTableBuilder()
      case Tst   => new TstTableBuilder()
      case Vo    => new VOTableBuilder()
    }
}