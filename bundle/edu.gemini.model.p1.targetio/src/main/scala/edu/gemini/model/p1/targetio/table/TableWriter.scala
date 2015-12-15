package edu.gemini.model.p1.targetio.table

import edu.gemini.model.p1.targetio.api._
import uk.ac.starlink.table.{StarTableOutput, RowListStarTable, StarTable}
import java.io.{FileOutputStream, BufferedOutputStream, OutputStream, File}

object TableWriter {
  def toStarTable[R](rows: Iterable[R], cols: List[Column[R,_]], format: FileFormat): StarTable = {
    val infos   = cols map { _.info(format) }
    val tab     = new RowListStarTable(infos.toArray)
    val sers    = cols map { col => col.writer(format) }
    val tabList = sers map { ser => rows map { row => ser(row).asInstanceOf[AnyRef] } }
    tabList.transpose foreach { row => tab.addRow(row.toArray) }
    tab
  }

  private def withOutputStream[T](os: OutputStream)(f: OutputStream => T): T = {
    try {
      f(os)
    } finally {
      if (os != null) os.close()
    }
  }

  def write[R](rows: Iterable[R], cols: List[Column[R,_]], file: File, ftype: FileType): Either[DataSourceError, Unit] =
    for {
      fos <- open(file).right
      res <- withOutputStream(new BufferedOutputStream(fos)) { os => write(rows, cols, os, ftype) }.right
    } yield res

  private def open(file: File): Either[DataSourceError, FileOutputStream] =
    try {
      Right(new FileOutputStream(file))
    } catch {
      case ex: Exception => Left(DataSourceError(s"Could not open ${file.getName}' for writing."))
    }

  def write[R](rows: Iterable[R], cols: List[Column[R,_]], outs: OutputStream, ftype: FileType): Either[DataSourceError, Unit] = {
    val starTab = toStarTable(rows, cols, ftype.format)
    try {
      Right(new StarTableOutput().writeStarTable(starTab, outs, stilWriter(ftype)))
    } catch {
      case ex: Exception =>
        val base = "There was an unexpected problem while writing targets"
        val msg  = Option(ex.getMessage) map { m => base + ":\n" + m } getOrElse base + "."
        Left(DataSourceError(msg))
    }
  }
}