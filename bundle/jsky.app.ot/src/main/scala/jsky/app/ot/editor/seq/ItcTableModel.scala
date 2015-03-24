package jsky.app.ot.editor.seq

import javax.swing.table.AbstractTableModel

import edu.gemini.itc.shared.{ItcImagingResult, ItcResult, ItcService}
import edu.gemini.shared.util.StringUtil
import edu.gemini.spModel.config2.ItemKey

import scala.concurrent.Future
import scala.util.{Success, Failure}

import scalaz.Scalaz._

/** Columns in the table are defined by their header label and a function on the unique config of the row. */
case class Column(label: String, value: (ItcUniqueConfig, Future[ItcService.Result]) => Object)

object ItcTableModel {
  /** Defines a set of header columns for all tables. */
  val headers = Seq(
    Column("Data Labels",     (c, r) => c.labels),
    Column("Images",          (c, r) => new java.lang.Integer(c.count)),             // must be an object for JTable
    Column("Exposure Time",   (c, r) => new java.lang.Double(c.singleExposureTime)), // must be an object for JTable
    Column("Total Exp. Time", (c, r) => new java.lang.Double(c.totalExposureTime))   // must be an object for JTable
  )
}

/**
 * ITC tables have three types of columns: a series of header columns, then all the values that change and are
 * relevant for the different unique configs (denoted by their ItemKey values) and finally the ITC calculation
 * results.
 */
sealed trait ItcTableModel extends AbstractTableModel {

  val headers: Seq[Column]
  val keys: Seq[ItemKey]
  val results: Seq[Column]

  val uniqueSteps: Seq[ItcUniqueConfig]
  val res: Seq[Future[ItcService.Result]]

  override def getRowCount: Int = uniqueSteps.size

  override def getColumnCount: Int = headers.size + keys.size + results.size

  override def getValueAt(row: Int, col: Int): Object = col match {
    case c if c <  headers.size             => header(col).value(uniqueSteps(row), res(row))
    case c if c >= headers.size + keys.size => result(col).value(uniqueSteps(row), res(row))
    case c                                  => uniqueSteps(row).config.getItemValue(key(col))
  }

  override def getColumnName(col: Int): String = col match {
    case c if c <  headers.size             => header(col).label
    case c if c >= headers.size + keys.size => result(col).label
    case c                                  => StringUtil.toDisplayName(key(col).getName)
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

  protected def peakPixelFlux(f: Future[ItcService.Result], n: Int = 0) = prettyPrint(f, n, r => r.peakPixelFlux)

  protected def singleSNRatio(f: Future[ItcService.Result], n: Int = 0) = prettyPrint(f, n, r => r.singleSNRatio)

  protected def totalSNRatio (f: Future[ItcService.Result], n: Int = 0) = prettyPrint(f, n, r => r.totalSNRatio)

  private def prettyPrint(f: Future[ItcService.Result], n: Int, v: ItcImagingResult => Double) =
    imagingResult(f, n).fold("")(x => f"${v(x)}%.2f")

}

class ItcGenericImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = ItcTableModel.headers
  val results = Seq(
    Column("PPF",             (c, r) => peakPixelFlux(r)),
    Column("S/N Single",      (c, r) => singleSNRatio(r)),
    Column("S/N Total",       (c, r) => totalSNRatio (r)),
    Column("Messages",        (c, r) => messages(r))
  )
}

/** GMOS specific ITC imaging table model. */
class ItcGmosImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = ItcTableModel.headers
  val results = Seq(
    Column("CCD1 PPF",        (c, r) => peakPixelFlux(r, 0)),
    Column("CCD1 S/N Single", (c, r) => singleSNRatio(r, 0)),
    Column("CCD1 S/N Total",  (c, r) => totalSNRatio (r, 0)),
    Column("CCD2 PPF",        (c, r) => peakPixelFlux(r, 1)),
    Column("CCD2 S/N Single", (c, r) => singleSNRatio(r, 1)),
    Column("CCD2 S/N Total",  (c, r) => totalSNRatio (r, 1)),
    Column("CCD3 PPF",        (c, r) => peakPixelFlux(r, 2)),
    Column("CCD3 S/N Single", (c, r) => singleSNRatio(r, 2)),
    Column("CCD3 S/N Total",  (c, r) => totalSNRatio (r, 2)),
    Column("Messages",        (c, r) => messages(r))
  )
}


/** Generic ITC spectroscopy table model. */
sealed trait ItcSpectroscopyTableModel extends ItcTableModel

class ItcGenericSpectroscopyTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcSpectroscopyTableModel {
  val headers = ItcTableModel.headers
  val results = Seq(
    Column("Messages",        (c, r) => messages(r))
  )
}

