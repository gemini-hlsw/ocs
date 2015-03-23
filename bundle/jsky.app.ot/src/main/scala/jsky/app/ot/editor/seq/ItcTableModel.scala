package jsky.app.ot.editor.seq

import javax.swing.table.AbstractTableModel

import edu.gemini.itc.shared.{ItcImagingResult, ItcResult, ItcService}
import edu.gemini.shared.util.StringUtil
import edu.gemini.spModel.config2.ItemKey

import scala.concurrent.Future
import scala.util.Failure

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

  def getRowCount: Int = uniqueSteps.size

  def getColumnCount: Int = headers.size + keys.size + results.size

  def getKeyAt(col: Int): Option[ItemKey] = col match {
    case c if c >= headers.size && c < headers.size + keys.size => Some(key(col))
    case _ => None
  }

  def getValueAt(row: Int, col: Int): Object = col match {
    case c if c < headers.size => header(col).value(uniqueSteps(row), res(row))
    case c if c >= headers.size + keys.size => result(col).value(uniqueSteps(row), res(row))
    case c => uniqueSteps(row).config.getItemValue(key(col))
  }

  override def getColumnName(col: Int): String = col match {
    case c if c < headers.size => header(col).label
    case c if c >= headers.size + keys.size => result(col).label
    case c => StringUtil.toDisplayName(key(col).getName)
  }

  // Translate overall column index into the corresponding header, column or key value.
  private def header(col: Int) = headers(col)

  private def key(col: Int) = keys(col - headers.size)

  private def result(col: Int) = results(col - headers.size - keys.size)

  // Gets the imaging result from the service result future (if present)
  protected def imagingResult(f: Future[ItcService.Result]): Option[ItcImagingResult] =
    imagingCalcResult(f).flatMap { r =>
      r.ccd match {
        case img: ItcImagingResult => Some(img)
        case _                     => None
      }
    }

  // Gets the result from the service result future (if present)
  protected def imagingCalcResult(f: Future[ItcService.Result]): Option[ItcResult] =
    for {
      futureResult  <- f.value                // unwrap future
      serviceResult <- futureResult.toOption  // unwrap try (as option)
      calcResult    <- serviceResult.toOption // unwrap validation (as option)
    } yield calcResult

  // TODO: display errors/validation messages in an appropriate way in the UI, for now just print them
  protected def messages(f: Future[ItcService.Result]): String =
    if (!f.isCompleted) "Calculating..."
    else if (f.value.get.isFailure) {
      val msg = "Error Service Call: " + (f.value.get match { case Failure(t) => t.printStackTrace(); t.getMessage})
      System.out.println(msg)
      msg
    }
    else if (f.value.get.get.isFailure) {
      f.value.get.get match {case scalaz.Failure(s) => s.foreach(msg => System.out.println("Validation failed: " + msg))}
      "Error Validation"
    }
    else "OK"
}


/** Generic ITC imaging tables model. */
sealed trait ItcImagingTableModel extends ItcTableModel

class ItcGenericImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = ItcTableModel.headers
  val results = Seq(
    Column("PPF",             (c, r) => imagingResult(r).fold("")(_.peakPixelFlux.toString)),
    Column("S/N Single",      (c, r) => imagingResult(r).fold("")(_.singleSNRatio.toString)),
    Column("S/N Total",       (c, r) => imagingResult(r).fold("")(_.totalSNRatio.toString)),
    Column("Messages",        (c, r) => messages(r))
  )


}

/** GMOS specific ITC imaging table model. */
class ItcGmosImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = ItcTableModel.headers
  val results = Seq(
    Column("CCD1 PPF",        (c, r) => imagingResult(r).fold("")(x => f"${x.peakPixelFlux}%.2f")),
    Column("CCD1 S/N Single", (c, r) => imagingResult(r).fold("")(x => f"${x.singleSNRatio}%.2f")),
    Column("CCD1 S/N Total",  (c, r) => imagingResult(r).fold("")(x => f"${x.totalSNRatio}%.2f")),
    Column("CCD2 PPF",        (c, r) => ""),
    Column("CCD2 S/N Single", (c, r) => ""),
    Column("CCD2 S/N Total",  (c, r) => ""),
    Column("CCD3 PPF",        (c, r) => ""),
    Column("CCD3 S/N Single", (c, r) => ""),
    Column("CCD3 S/N Total",  (c, r) => ""),
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

