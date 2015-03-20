package jsky.app.ot.editor.seq

import javax.swing.table.AbstractTableModel

import edu.gemini.shared.util.StringUtil
import edu.gemini.spModel.config2.ItemKey

/** Columns in the table are defined by their header label and a function on the unique config of the row. */
case class Column(label: String, value: ItcUniqueConfig => Object)

object ItcTableModel {
  /** Defines a set of header columns for all tables. */
  val headers = Seq(
    Column("Data Labels",     c => c.labels),
    Column("Images",          c => new java.lang.Integer(c.count)),             // must be an object for JTable
    Column("Exposure Time",   c => new java.lang.Double(c.singleExposureTime)), // must be an object for JTable
    Column("Total Exp. Time", c => new java.lang.Double(c.totalExposureTime))   // must be an object for JTable
  )
}

/**
 * ITC tables have three types of columns: a series of header columns, then all the values that change and are
 * relevant for the different unique configs (denoted by their ItemKey values) and finally the ITC calculation
 * results.
 */
sealed trait ItcTableModel extends AbstractTableModel {

  val headers: Seq[Column]
  val keys:    Seq[ItemKey]
  val results: Seq[Column]
  val uniqueSteps: Seq[ItcUniqueConfig]

  def getRowCount: Int = uniqueSteps.size

  def getColumnCount: Int = headers.size + keys.size + results.size

  def getKeyAt(col: Int): Option[ItemKey] = col match {
    case c if c >= headers.size && c < headers.size + keys.size => Some(key(col))
    case _                                                      => None
  }

  def getValueAt(row: Int, col: Int): Object = col match {
    case c if c <  headers.size               => header(col).value(uniqueSteps(row))
    case c if c >= headers.size + keys.size   => result(col).value(uniqueSteps(row))
    case c                                    => uniqueSteps(row).config.getItemValue(key(col))
  }

  override def getColumnName(col: Int): String = col match {
    case c if c <  headers.size               => header(col).label
    case c if c >= headers.size + keys.size   => result(col).label
    case c                                    => StringUtil.toDisplayName(key(col).getName)
  }

  // Translate overall column index into the corresponding header, column or key value.
  private def header(col: Int) = headers(col)
  private def key   (col: Int) = keys   (col - headers.size)
  private def result(col: Int) = results(col - headers.size - keys.size)

 }

/** Generic ITC imaging tables model. */
sealed trait ItcImagingTableModel extends ItcTableModel

class ItcGenericImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig]) extends ItcImagingTableModel {
  val headers = ItcTableModel.headers
  val results = Seq(
    Column("PPF",             c => ""),
    Column("S/N Single",      c => ""),
    Column("S/N Total",       c => ""),
    Column("Messages",        c => "OK")
  )
}

/** GMOS specific ITC imaging table model. */
class ItcGmosImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig]) extends ItcImagingTableModel {
  val headers = ItcTableModel.headers
  val results = Seq(
    Column("CCD1 PPF",        c => ""),
    Column("CCD1 S/N Single", c => ""),
    Column("CCD1 S/N Total",  c => ""),
    Column("CCD2 PPF",        c => ""),
    Column("CCD2 S/N Single", c => ""),
    Column("CCD2 S/N Total",  c => ""),
    Column("CCD3 PPF",        c => ""),
    Column("CCD3 S/N Single", c => ""),
    Column("CCD3 S/N Total",  c => ""),
    Column("Messages",        c => "OK")
  )
}


/** Generic ITC spectroscopy table model. */
sealed trait ItcSpectroscopyTableModel extends ItcTableModel

class ItcGenericSpectroscopyTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig]) extends ItcSpectroscopyTableModel {
  val headers = ItcTableModel.headers
  val results = Seq(
    Column("Messages",        c => "OK")
  )
}

