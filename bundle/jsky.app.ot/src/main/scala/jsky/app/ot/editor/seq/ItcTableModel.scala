package jsky.app.ot.editor.seq

import javax.swing.Icon
import javax.swing.table.AbstractTableModel

import edu.gemini.itc.shared._
import edu.gemini.shared.util.StringUtil
import edu.gemini.spModel.config2.ItemKey

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz._

/** Columns in the table are defined by their header label and a function on the unique config of the row. */
case class Column(label: String, value: (ItcUniqueConfig, String \/ ItcParameters, Future[ItcService.Result]) => AnyRef, tooltip: String = "")

object ItcTableModel {
  val PeakPixelETooltip   = "Peak Signal + Background in electrons / coadd"
  val PeakPixelAduTooltip = "Peak Signal + Background in ADU / coadd"
  val PeakPixelFWTooltip  = "Peak Signal + Background in percent of the detector full well"
}

/** ITC tables have three types of columns: a series of header columns, then all the dynamic values that change and are
  * relevant for the different unique configs (denoted by their {{{ItemKey}}} values) and finally the ITC calculation
  * results. The static columns (headers and results) are represented by a {{{Column}}} object.
  */
sealed trait ItcTableModel extends AbstractTableModel {

  /// Define some generic columns. Values are rendered as strings in order to have them left aligned, similar to other sequence tables.
  val LabelsColumn  = Column("Data\nLabels",           (c, _, r) => (resultIcon(r).orNull, c.labels))
  val ImagesColumn  = Column("Images",                 (c, _, _) => s"${c.count}",                  tooltip = "Number of exposures used in S/N calculation")
  val CoaddsColumn  = Column("Coadds",                 (c, _, _) => s"${c.coadds.getOrElse(1.0)}",  tooltip = "Number of coadds")
  val ExpTimeColumn = Column("Exposure\nTime (s)",     (c, _, _) => f"${c.singleExposureTime}%.1f", tooltip = "Exposure time of each image")
  val TotTimeColumn = Column("Total Exp.\nTime (s)",   (c, _, _) => f"${c.totalExposureTime}%.1f",  tooltip = "Total exposure time")
  val SrcMagColumn  = Column("Source\nMag",            (_, i, _) => i.map(sourceMag).toOption,      tooltip = "Source magnitude (mag)")
  val SrcFracColumn = Column("Source\nFraction",       (_, i, _) => i.map(sourceFraction).toOption, tooltip = "Fraction of images on source")

  val PeakPixelColumn     = Column("Peak\n(e-)",       (_, _, r) => maxPeakPixelFlux(r),          tooltip = ItcTableModel.PeakPixelETooltip)
  val PeakADUColumn       = Column("Peak\n(ADU)",      (_, _, r) => maxImgAdu(r),                 tooltip = ItcTableModel.PeakPixelAduTooltip)
  val PeakFullWellColumn  = Column("Peak\n(%FW)",      (_, _, r) => maxImgPercentWell(r),         tooltip = ItcTableModel.PeakPixelFWTooltip)
  val SNSingleColumn      = Column("S/N Single Coadd", (_, _, r) => maxSingleSNRatio(r),            tooltip = "Signal / Noise for one exposure with one coadd")
  val SNTotalColumn       = Column("S/N Total",        (_, _, r) => maxTotalSNRatio(r),            tooltip = "Total Signal / Noise for all exposures and coadds")

  // Define different sets of columns as headers
  val PeakColumns       = List(PeakPixelColumn, PeakADUColumn, PeakFullWellColumn)
  val SNColumns         = List(SNSingleColumn, SNTotalColumn)
  val Headers           = List(LabelsColumn, ImagesColumn, ExpTimeColumn, TotTimeColumn, SrcMagColumn, SrcFracColumn)
  val HeadersWithCoadds = List(LabelsColumn, ImagesColumn, CoaddsColumn, ExpTimeColumn, TotTimeColumn, SrcMagColumn, SrcFracColumn)

  val headers:      List[Column]
  val keys:         List[ItemKey]
  val results:      List[Column]

  val uniqueSteps:  List[ItcUniqueConfig]
  val res:          List[Future[ItcService.Result]]
  val inputs:       List[String\/ItcParameters]


  // Gets the spectroscopy result from the service result future (if present).
  protected def spectroscopyResult(f: Future[ItcService.Result]): Option[ItcSpectroscopyResult] =
    serviceResult(f).flatMap {
      case spc: ItcSpectroscopyResult => Some(spc)
      case _                          => None
    }

  // Gets the result from the service result future (if present)
  protected def serviceResult(f: Future[ItcService.Result]): Option[ItcResult] =
    for {
      futureResult  <- f.value                // unwrap future
      serviceResult <- futureResult.toOption  // unwrap try
      calcResult    <- serviceResult.toOption // unwrap validation
    } yield calcResult

  // Gets an icon to represent the state of this result (none if all is ok)
  protected def resultIcon(f: Future[ItcService.Result]): Option[Icon] =
    f.value.fold {
      Some(ItcPanel.SpinnerIcon).asInstanceOf[Option[Icon]]
    } {
      case Failure(_)                       => Some(ItcPanel.ErrorIcon)
      case Success(s) => s match {
        case -\/(_)                         => Some(ItcPanel.ErrorIcon)
        case \/-(r) if r.warnings.nonEmpty  => Some(ItcPanel.WarningIcon)
        case _                              => None
      }
    }

  protected def sourceMag(i: ItcParameters) = f"${i.source.norm}%.2f ${i.source.normBand.name}"

  protected def sourceFraction(i: ItcParameters) = f"${i.observation.sourceFraction}%.2f"

  private def maxR[A](result: Future[ItcService.Result], f: ItcResult => A): Option[A] =
    serviceResult(result).map(f)

  private def maxR[A](result: Future[ItcService.Result], ccd: Int, f: ItcCcd => A): Option[A] =
    serviceResult(result).flatMap(_.ccd(ccd)).map(f)

  protected def peakPixelFlux(result: Future[ItcService.Result], ccd: Int): Option[Int]       = serviceResult(result).flatMap(_.peakPixelFlux(ccd))
  protected def maxPeakPixelFlux(result: Future[ItcService.Result]): Option[Int]              = maxR(result, _.maxPeakPixelFlux)

  protected def imgPercentWell(result: Future[ItcService.Result], ccd: Int): Option[Double]   = maxR(result, ccd, _.percentFullWell)
  protected def maxImgPercentWell(result: Future[ItcService.Result]): Option[Double]          = maxR(result, _.maxPercentFullWell)
  protected def imgAdu(result: Future[ItcService.Result], ccd: Int): Option[Int]              = maxR(result, ccd, _.adu)
  protected def maxImgAdu(result: Future[ItcService.Result]): Option[Int]                     = maxR(result, _.maxAdu)
  protected def singleSNRatio(result: Future[ItcService.Result], ccd: Int): Option[Double]    = maxR(result, ccd, _.singleSNRatio)
  protected def maxSingleSNRatio(result: Future[ItcService.Result]): Option[Double]           = maxR(result, _.maxSingleSNRatio)
  protected def totalSNRatio  (result: Future[ItcService.Result], ccd: Int): Option[Double]   = maxR(result, ccd, _.totalSNRatio)
  protected def maxTotalSNRatio(result: Future[ItcService.Result]): Option[Double]            = maxR(result, _.maxTotalSNRatio)

  // ===

  override def getRowCount: Int = uniqueSteps.size

  override def getColumnCount: Int = headers.size + keys.size + results.size

  override def getValueAt(row: Int, col: Int): Object = column(col) match {
    case Some(c) => c.value(uniqueSteps(row), inputs(row), res(row))
    case None    => uniqueSteps(row).config.getItemValue(toKey(col))
  }

  override def getColumnName(col: Int): String = {
    def multiLineHeader(label: String, separator: String): String =
      // returning an html snippet allows for column headers with multiple lines
      "<html>" + label.replaceFirst(separator, "<br/>") + "</html>"

    column(col).map(c => multiLineHeader(c.label, "\n"))
        .getOrElse(multiLineHeader(StringUtil.toDisplayName(toKey(col).getName), " "))
  }

  def tooltip(col: Int): String =
    column(col).map(_.tooltip).orNull

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

}


/** Generic ITC imaging tables model. */
sealed trait ItcImagingTableModel extends ItcTableModel

case class ItcGenericImagingTableModel(keys: List[ItemKey], uniqueSteps: List[ItcUniqueConfig], inputs: List[String\/ItcParameters], res: List[Future[ItcService.Result]], showCoadds: Boolean = false) extends ItcImagingTableModel {
  val headers = if (showCoadds) HeadersWithCoadds else Headers
  val results = PeakColumns ++ SNColumns
}

/** GMOS specific ITC imaging table model. */
case class ItcGmosImagingTableModel(keys: List[ItemKey], uniqueSteps: List[ItcUniqueConfig], inputs: List[String\/ItcParameters], res: List[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = Headers
  val results = List(
    Column("CCD1 Peak\n(e-)",          (_, _, r) => peakPixelFlux  (r, ccd = 0),   tooltip = ItcTableModel.PeakPixelETooltip),
    Column("CCD1 Peak\n(ADU)",         (_, _, r) => imgAdu         (r, ccd = 0),   tooltip = ItcTableModel.PeakPixelAduTooltip),
    Column("CCD1 Peak\n(% Full Well)", (_, _, r) => imgPercentWell (r, ccd = 0),   tooltip = ItcTableModel.PeakPixelFWTooltip),
    Column("CCD1\nS/N Single",         (_, _, r) => singleSNRatio  (r, ccd = 0)),
    Column("CCD1\nS/N Total",          (_, _, r) => totalSNRatio   (r, ccd = 0)),

    Column("CCD2 Peak\n(e-)",          (_, _, r) => peakPixelFlux  (r, ccd = 1),   tooltip = ItcTableModel.PeakPixelETooltip),
    Column("CCD2 Peak\n(ADU)",         (_, _, r) => imgAdu         (r, ccd = 1),   tooltip = ItcTableModel.PeakPixelAduTooltip),
    Column("CCD2 Peak\n(% Full Well)", (_, _, r) => imgPercentWell (r, ccd = 1),   tooltip = ItcTableModel.PeakPixelFWTooltip),
    Column("CCD2\nS/N Single",         (_, _, r) => singleSNRatio  (r, ccd = 1)),
    Column("CCD2\nS/N Total",          (_, _, r) => totalSNRatio   (r, ccd = 1)),

    Column("CCD3 Peak\n(e-)",          (_, _, r) => peakPixelFlux  (r, ccd = 2),   tooltip = ItcTableModel.PeakPixelETooltip),
    Column("CCD3 Peak\n(ADU)",         (_, _, r) => imgAdu         (r, ccd = 2),   tooltip = ItcTableModel.PeakPixelAduTooltip),
    Column("CCD3 Peak\n(% Full Well)", (_, _, r) => imgPercentWell (r, ccd = 2),   tooltip = ItcTableModel.PeakPixelFWTooltip),
    Column("CCD3\nS/N Single",         (_, _, r) => singleSNRatio  (r, ccd = 2)),
    Column("CCD3\nS/N Total",          (_, _, r) => totalSNRatio   (r, ccd = 2))
  )
}

case class ItcGsaoiImagingTableModel(keys: List[ItemKey], uniqueSteps: List[ItcUniqueConfig], inputs: List[String\/ItcParameters], res: List[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = HeadersWithCoadds ++ List(
    Column("Strehl", (_, i, _) => gems(i), tooltip = "Estimated Strehl and band")
  )
  val results = PeakColumns ++ SNColumns

  def gems(i: String \/ ItcParameters): Option[String] = i.toOption.map { inputs =>
    val gems = inputs.instrument.asInstanceOf[GsaoiParameters].gems
    f"${gems.avgStrehl}%.2f ${gems.strehlBand}"
  }

}


/** Generic ITC spectroscopy table model. */
sealed trait ItcSpectroscopyTableModel extends ItcTableModel

class ItcGenericSpectroscopyTableModel(val keys: List[ItemKey], val uniqueSteps: List[ItcUniqueConfig], val inputs: List[String\/ItcParameters], val res: List[Future[ItcService.Result]], showCoadds: Boolean = false) extends ItcSpectroscopyTableModel {
  val headers = if (showCoadds) HeadersWithCoadds else Headers
  val results = PeakColumns ++ SNColumns
}

class ItcGnirsSpectroscopyTableModel(val keys: List[ItemKey], val uniqueSteps: List[ItcUniqueConfig], val inputs: List[String\/ItcParameters], val res: List[Future[ItcService.Result]], xDisp: Boolean) extends ItcSpectroscopyTableModel {
  val headers = HeadersWithCoadds
  // ITC does not provide S/N Single values for cross dispersion
  val results = if (xDisp) PeakColumns :+ SNTotalColumn else PeakColumns ++ SNColumns
}
