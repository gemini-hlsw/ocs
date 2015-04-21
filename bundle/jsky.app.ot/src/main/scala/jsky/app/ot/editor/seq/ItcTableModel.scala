package jsky.app.ot.editor.seq

import javax.swing.table.AbstractTableModel

import edu.gemini.itc.shared.{ItcImagingResult, ItcResult, ItcService}
import edu.gemini.shared.util.StringUtil
import edu.gemini.spModel.config2.ItemKey

import scala.concurrent.Future
import scala.swing.Table.LabelRenderer
import scala.util.{Failure, Success}
import scalaz.Scalaz._

/** Columns in the table are defined by their header label and a function on the unique config of the row. */
case class Column[A](label: String, value: (ItcUniqueConfig, Future[ItcService.Result]) => Object, renderer: LabelRenderer[AnyRef] = ItcTable.AnyRenderer, tooltip: String = "")

object ItcTableModel {
  val PeakPixelTooltip = "Peak pixel value = signal + background [ADU]"

  /** Defines a set of header columns for all tables. */
  val Headers = Seq(
    Column("Data Labels",     (c, r) => c.labels),
    Column("Images",          (c, r) => new java.lang.Integer(c.count),             tooltip = "Number of exposures used in S/N calculation"),
    Column("Exposure Time",   (c, r) => new java.lang.Double(c.singleExposureTime), tooltip = "Exposure time of each image [s]"),
    Column("Total Exp. Time", (c, r) => new java.lang.Double(c.totalExposureTime),  tooltip = "Total exposure time [s]")
  )
}

/**
 * ITC tables have three types of columns: a series of header columns, then all the values that change and are
 * relevant for the different unique configs (denoted by their ItemKey values) and finally the ITC calculation
 * results.
 */
sealed trait ItcTableModel extends AbstractTableModel {

  val headers: Seq[Column[_]]
  val keys: Seq[ItemKey]
  val results: Seq[Column[_]]

  val uniqueSteps: Seq[ItcUniqueConfig]
  val res: Seq[Future[ItcService.Result]]

  override def getRowCount: Int = uniqueSteps.size

  override def getColumnCount: Int = headers.size + keys.size + results.size

  override def getValueAt(row: Int, col: Int): Object = col match {
    case c if c <  headers.size             => header(col).value(uniqueSteps(row), res(row))
    case c if c >= headers.size + keys.size => result(col).value(uniqueSteps(row), res(row))
    case _                                  => uniqueSteps(row).config.getItemValue(key(col))
  }

  override def getColumnName(col: Int): String = col match {
    case c if c <  headers.size             => header(col).label
    case c if c >= headers.size + keys.size => result(col).label
    case _                                  => StringUtil.toDisplayName(key(col).getName)
  }

  def renderer(col: Int): LabelRenderer[AnyRef] = col match {
    case c if c <  headers.size             => header(col).renderer
    case c if c >= headers.size + keys.size => result(col).renderer
    case _                                  => ItcTable.AnyRenderer
  }

  def tooltip(col: Int): String = col match {
    case c if c <  headers.size             => header(col).tooltip
    case c if c >= headers.size + keys.size => result(col).tooltip
    case _                                  => key(col).getPath
  }

  // Gets the ItemKey of a column (if any), this is used by the table to color code the columns.
  def getKeyAt(col: Int): Option[ItemKey] = col match {
    case c if c >= headers.size && c < headers.size + keys.size => Some(key(col))
    case _                                                      => None
  }

  // Translate overall column index into the corresponding header, column or key value.
  private def header(col: Int) = headers(col)

  private def key(col: Int) = keys(col - headers.size)

  private def result(col: Int) = results(col - headers.size - keys.size)

  // Gets the result from the service result future (if present)
  protected def imagingCalcResult(f: Future[ItcService.Result]): Option[ItcResult] =
    for {
      futureResult  <- f.value                // unwrap future
      serviceResult <- futureResult.toOption  // unwrap try
      calcResult    <- serviceResult.toOption // unwrap validation
    } yield calcResult

  // TODO: display errors/validation messages in an appropriate way in the UI, for now also print them to console
  protected def messages(f: Future[ItcService.Result]): String =
    f.value.fold("Calculating...") {
      case Failure(t) => t <| (_.printStackTrace()) |> (_.getMessage)  // "Look mummy, there's a spaceship up in the sky!"
      case Success(s) => s match {
        case scalaz.Failure(errs) => errs.mkString(", ") <| System.out.println
        case scalaz.Success(_)    => "OK"
      }
    }
}


/** Generic ITC imaging tables model. */
sealed trait ItcImagingTableModel extends ItcTableModel {

  // Gets the imaging result from the service result future (if present).
  // Note that in most cases (except for GMOS) there is only one CCD in the result, but for GMOS there can be
  // 1 or 3 CCDs depending on the selected CCD manufacturer.
  protected def imagingResult(f: Future[ItcService.Result], n: Int = 0): Option[ItcImagingResult] =
    imagingCalcResult(f).flatMap { r =>
      // For GMOS ITC returns 1 or 3 different CCD results depending on the manufacturer, the simplest way to deal
      // with this is by just using n % #CCDs here, which means that if there is only one result it is repeated three
      // times, and if there are 3 results, they are shown individually as expected. All instruments other than GMOS
      // use this method with ccd index = 0.
      r.ccds(n % r.ccds.length) match {
        case img: ItcImagingResult => Some(img)
        case _                     => None
      }
    }

  protected def peakPixelFlux(result: Future[ItcService.Result], ccd: Int = 0) = imagingResult(result, ccd).map(_.peakPixelFlux)

  protected def singleSNRatio(result: Future[ItcService.Result], ccd: Int = 0) = imagingResult(result, ccd).map(_.singleSNRatio)

  protected def totalSNRatio (result: Future[ItcService.Result], ccd: Int = 0) = imagingResult(result, ccd).map(_.totalSNRatio)
  
}

class ItcGenericImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = ItcTableModel.Headers
  val results = Seq(
    Column("Peak",            (c, r) => peakPixelFlux(r),         ItcTable.IntRenderer,       tooltip = ItcTableModel.PeakPixelTooltip),
    Column("S/N Single",      (c, r) => singleSNRatio(r),         ItcTable.DoubleRenderer),
    Column("S/N Total",       (c, r) => totalSNRatio (r),         ItcTable.DoubleRenderer),
    Column("Messages",        (c, r) => messages(r))
  )
}

/** GMOS specific ITC imaging table model. */
class ItcGmosImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = ItcTableModel.Headers
  val results = Seq(
    Column("CCD1 Peak",       (c, r) => peakPixelFlux(r, ccd=0),   ItcTable.IntRenderer,      tooltip = ItcTableModel.PeakPixelTooltip + " for CCD 1"),
    Column("CCD1 S/N Single", (c, r) => singleSNRatio(r, ccd=0),   ItcTable.DoubleRenderer),
    Column("CCD1 S/N Total",  (c, r) => totalSNRatio (r, ccd=0),   ItcTable.DoubleRenderer),
    Column("CCD2 Peak",       (c, r) => peakPixelFlux(r, ccd=1),   ItcTable.IntRenderer,      tooltip = ItcTableModel.PeakPixelTooltip + " for CCD 2"),
    Column("CCD2 S/N Single", (c, r) => singleSNRatio(r, ccd=1),   ItcTable.DoubleRenderer),
    Column("CCD2 S/N Total",  (c, r) => totalSNRatio (r, ccd=1),   ItcTable.DoubleRenderer),
    Column("CCD3 Peak",       (c, r) => peakPixelFlux(r, ccd=2),   ItcTable.IntRenderer,      tooltip = ItcTableModel.PeakPixelTooltip + " for CCD 3"),
    Column("CCD3 S/N Single", (c, r) => singleSNRatio(r, ccd=2),   ItcTable.DoubleRenderer),
    Column("CCD3 S/N Total",  (c, r) => totalSNRatio (r, ccd=2),   ItcTable.DoubleRenderer),
    Column("Messages",        (c, r) => messages(r))
  )
}


/** Generic ITC spectroscopy table model. */
sealed trait ItcSpectroscopyTableModel extends ItcTableModel

class ItcGenericSpectroscopyTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcSpectroscopyTableModel {
  val headers = ItcTableModel.Headers
  val results = Seq(
    Column("Messages",        (c, r) => messages(r))
  )
}