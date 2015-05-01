package jsky.app.ot.editor.seq

import javax.swing.table.AbstractTableModel

import edu.gemini.itc.shared.{ItcSpectroscopyResult, ItcImagingResult, ItcResult, ItcService}
import edu.gemini.shared.util.StringUtil
import edu.gemini.spModel.config2.ItemKey

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz.Scalaz._

/** Columns in the table are defined by their header label and a function on the unique config of the row. */
case class Column(label: String, value: (ItcUniqueConfig, Future[ItcService.Result]) => AnyRef, tooltip: String = "")

object ItcTableModel {
  val PeakPixelTooltip = "Peak pixel value = signal + background"
}

/** ITC tables have three types of columns: a series of header columns, then all the values that change and are
  * relevant for the different unique configs (denoted by their {{{ItemKey}}} values) and finally the ITC calculation
  * results. The static columns (headers and results) are represented by a {{{Column}}} object.
  */
sealed trait ItcTableModel extends AbstractTableModel {

  /** Defines a set of header columns for all tables. */
  val Headers = Seq(
    Column("Data Labels",     (c, r) => c.labels),
    Column("Images",          (c, r) => new java.lang.Integer(c.count),             tooltip = "Number of exposures used in S/N calculation"),
    Column("Exposure Time",   (c, r) => new java.lang.Double(c.singleExposureTime), tooltip = "Exposure time of each image [s]"),
    Column("Total Exp. Time", (c, r) => new java.lang.Double(c.totalExposureTime),  tooltip = "Total exposure time [s]")
  )

  val headers:      Seq[Column]
  val keys:         Seq[ItemKey]
  val results:      Seq[Column]

  val uniqueSteps:  Seq[ItcUniqueConfig]
  val res:          Seq[Future[ItcService.Result]]


  // Gets the imaging result from the service result future (if present).
  protected def imagingResult(f: Future[ItcService.Result]): Option[ItcImagingResult] =
    serviceResult(f).flatMap {
      case img: ItcImagingResult      => Some(img)
      case _                          => None
    }

  // Gets the spectroscopy result from the service result future (if present).
  protected def spectroscopyResult(f: Future[ItcService.Result]): Option[ItcSpectroscopyResult] =
    serviceResult(f).flatMap {
      case spc: ItcSpectroscopyResult => Some(spc)
      case _                          => None
    }

  protected def spcSourceMag    (result: Future[ItcService.Result]) = spectroscopyResult(result).map(_.source.profile.norm)

  protected def spcSourceBand   (result: Future[ItcService.Result]) = spectroscopyResult(result).map(_.source.getNormBand.name)

  // TODO: the way the values in the charts are accessed is not safe, right now I know how the data looks like, but this needs to be improved
  // TODO: how to identify the different charts and data series? labels? This works for GMOSN/S, F2 and NIRI
  protected def spcPeakElectrons(result: Future[ItcService.Result]) = spectroscopyResult(result).map(_.dataSets(0).series(0).data(1).max)
  protected def spcPeakSNSingle (result: Future[ItcService.Result]) = spectroscopyResult(result).map(_.dataSets(1).series(0).data(1).max)
  protected def spcPeakSNFinal  (result: Future[ItcService.Result]) = spectroscopyResult(result).map(_.dataSets(1).series(1).data(1).max)

  // Gets the result from the service result future (if present)
  protected def serviceResult(f: Future[ItcService.Result]): Option[ItcResult] =
    for {
      futureResult  <- f.value                // unwrap future
      serviceResult <- futureResult.toOption  // unwrap try
      calcResult    <- serviceResult.toOption // unwrap validation
    } yield calcResult


  protected def peakPixelFlux(result: Future[ItcService.Result], ccd: Int = 0) = imagingResult(result).map(_.ccd(ccd).peakPixelFlux.toInt)

  protected def singleSNRatio(result: Future[ItcService.Result], ccd: Int = 0) = imagingResult(result).map(_.ccd(ccd).singleSNRatio)

  protected def totalSNRatio (result: Future[ItcService.Result], ccd: Int = 0) = imagingResult(result).map(_.ccd(ccd).totalSNRatio)

  protected def sourceMag    (result: Future[ItcService.Result]) = imagingResult(result).map(_.source.profile.norm)

  protected def sourceBand   (result: Future[ItcService.Result]) = imagingResult(result).map(_.source.getNormBand.name)

  // ===

  override def getRowCount: Int = uniqueSteps.size

  override def getColumnCount: Int = headers.size + keys.size + results.size

  override def getValueAt(row: Int, col: Int): Object = column(col) match {
    case Some(c) => c.value(uniqueSteps(row), res(row))
    case None    => uniqueSteps(row).config.getItemValue(toKey(col))
  }

  override def getColumnName(col: Int): String = column(col) match {
    case Some(c) => c.label
    case None    => StringUtil.toDisplayName(toKey(col).getName) // create column name for key columns
  }

  def tooltip(col: Int): String = column(col) match {
    case Some(c) => c.tooltip
    case None    => ""                    // no tooltip for key columns
  }

  /** Gets the column description for the given {{{col}}} index. Returns {{{None}}} for dynamic key columns. */
  def column(col: Int): Option[Column] = col match {
    case c if c <  headers.size             => Some(toHeader(col))
    case c if c >= headers.size + keys.size => Some(toResult(col))
    case _                                  => None
  }

  /** Gets the ItemKey of a column (if any), this is used by the table to color code the columns. */
  def key(col: Int): Option[ItemKey] = col match {
    case c if c >= headers.size && c < headers.size + keys.size => Some(toKey(col))
    case _                                                      => None
  }

  def result(row: Int): Option[ItcSpectroscopyResult] = spectroscopyResult(res(row))

  // Translate overall column index into the corresponding header, column or key value.
  private def toHeader(col: Int) = headers(col)

  private def toKey   (col: Int) = keys(col - headers.size)

  private def toResult(col: Int) = results(col - headers.size - keys.size)

  // TODO: display errors/validation messages in an appropriate way in the UI, for now also print them to console
  protected def messages(f: Future[ItcService.Result]): String =
    f.value.fold("Calculating...") {
      case Failure(t) => t <| (_.printStackTrace()) |> (_.getMessage)  // "Look mummy, there's a spaceship up in the sky!"
      case Success(s) => s match {
        case scalaz.Failure(errs) => errs.mkString(", ")
        case scalaz.Success(_)    => "OK"
      }
    }
}


/** Generic ITC imaging tables model. */
sealed trait ItcImagingTableModel extends ItcTableModel

class ItcGenericImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = Headers ++ Seq(
    Column("Source Mag",      (c, r) => sourceMag(r),              tooltip = "Source magnitude [mag]"),
    Column("Source Band",     (c, r) => sourceBand(r),             tooltip = "Source band")
    )
  val results = Seq(
    Column("Peak",            (c, r) => peakPixelFlux(r),          tooltip = ItcTableModel.PeakPixelTooltip),
    Column("S/N Single",      (c, r) => singleSNRatio(r)),
    Column("S/N Total",       (c, r) => totalSNRatio (r)),
    Column("Messages",        (c, r) => messages(r))
  )
}

/** GMOS specific ITC imaging table model. */
class ItcGmosImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = Headers ++ Seq(
    Column("Source Mag",      (c, r) => sourceMag(r),              tooltip = "Source magnitude [mag]"),
    Column("Source Band",     (c, r) => sourceBand(r),             tooltip = "Source band")
  )
  val results = Seq(
    Column("CCD1 Peak",       (c, r) => peakPixelFlux(r, ccd=0),   tooltip = ItcTableModel.PeakPixelTooltip + " for CCD 1"),
    Column("CCD1 S/N Single", (c, r) => singleSNRatio(r, ccd=0)),
    Column("CCD1 S/N Total",  (c, r) => totalSNRatio (r, ccd=0)),
    Column("CCD2 Peak",       (c, r) => peakPixelFlux(r, ccd=1),   tooltip = ItcTableModel.PeakPixelTooltip + " for CCD 2"),
    Column("CCD2 S/N Single", (c, r) => singleSNRatio(r, ccd=1)),
    Column("CCD2 S/N Total",  (c, r) => totalSNRatio (r, ccd=1)),
    Column("CCD3 Peak",       (c, r) => peakPixelFlux(r, ccd=2),   tooltip = ItcTableModel.PeakPixelTooltip + " for CCD 3"),
    Column("CCD3 S/N Single", (c, r) => singleSNRatio(r, ccd=2)),
    Column("CCD3 S/N Total",  (c, r) => totalSNRatio (r, ccd=2)),
    Column("Messages",        (c, r) => messages(r))
  )
}


/** Generic ITC spectroscopy table model. */
sealed trait ItcSpectroscopyTableModel extends ItcTableModel

class ItcGenericSpectroscopyTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcSpectroscopyTableModel {
  val headers = Headers ++ Seq(
    Column("Source Mag",      (c, r) => spcSourceMag(r),           tooltip = "Source magnitude [mag]"),
    Column("Source Band",     (c, r) => spcSourceBand(r),          tooltip = "Source band"),
    Column("Peak",            (c, r) => spcPeakElectrons(r),       tooltip = "Peak e- per exposure"),
    Column("S/N Single",      (c, r) => spcPeakSNSingle(r)),
    Column("S/N Total",       (c, r) => spcPeakSNFinal(r))
  )
  val results = Seq(
    Column("Messages",        (c, r) => messages(r))
  )

}