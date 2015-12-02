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
case class Column(label: String, value: (ItcUniqueConfig, String\/ItcParameters, Future[ItcService.Result]) => AnyRef, tooltip: String = "")

object ItcTableModel {
  val PeakPixelETooltip   = "Peak (e): Signal + Background in electrons"
  val PeakPixelAduTooltip = "Peak (ADU): Signal + Background in ADU"
  val PeakPixelFWTooltip  = "Peak (%FW): Signal + Background in percent of the detector full well"
}

/** ITC tables have three types of columns: a series of header columns, then all the dynamic values that change and are
  * relevant for the different unique configs (denoted by their {{{ItemKey}}} values) and finally the ITC calculation
  * results. The static columns (headers and results) are represented by a {{{Column}}} object.
  */
sealed trait ItcTableModel extends AbstractTableModel {

  /// Define some generic columns. Values are rendered as strings in order to have them left aligned, similar to other sequence tables.
  val LabelsColumn  = Column("Data\nLabels",          (c, i, r) => (resultIcon(r).orNull, c.labels))
  val ImagesColumn  = Column("Images",                (c, i, r) => s"${c.count}",                     tooltip = "Number of exposures used in S/N calculation")
  val CoaddsColumn  = Column("Coadds",                (c, i, r) => s"${c.coadds.getOrElse(1.0)}",     tooltip = "Number of coadds")
  val ExpTimeColumn = Column("Exposure\nTime (s)",    (c, i, r) => f"${c.singleExposureTime}%.1f",    tooltip = "Exposure time of each image")
  val TotTimeColumn = Column("Total Exp.\nTime (s)",  (c, i, r) => f"${c.totalExposureTime}%.1f",     tooltip = "Total exposure time")
  val SrcMagColumn  = Column("Source\nMag",           (c, i, r) => i.map(sourceMag).toOption,         tooltip = "Source magnitude (mag)")
  val SrcFracColumn = Column("Source\nFraction",      (c, i, r) => i.map(sourceFraction).toOption,    tooltip = "Fraction of images on source")

  val PeakPixelColumn     = Column("Peak\n(e-)",      (c, i, r) => peakPixelFlux(r),                  tooltip = ItcTableModel.PeakPixelETooltip)
  val PeakADUColumn       = Column("Peak\n(ADU)",     (c, i, r) => imgAdu(r),                         tooltip = ItcTableModel.PeakPixelAduTooltip)
  val PeakFullWellColumn  = Column("Peak\n(%FW)",     (c, i, r) => imgPercentWell(r),                 tooltip = ItcTableModel.PeakPixelFWTooltip)
  val SNSingleColumn      = Column("S/N Single",      (c, i, r) => singleSNRatio(r))
  val SNTotalColumn       = Column("S/N Total",       (c, i, r) => totalSNRatio (r))

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

  // Gets an icon to represen the state of this result (none if all is ok)
  protected def resultIcon(f: Future[ItcService.Result]): Option[Icon] =
    f.value.fold {
      Some(ItcPanel.SpinnerIcon).asInstanceOf[Option[Icon]]
    } {
      case Failure(t)                       => Some(ItcPanel.ErrorIcon)
      case Success(s) => s match {
        case -\/(_)                         => Some(ItcPanel.ErrorIcon)
        case \/-(r) if r.warnings.nonEmpty  => Some(ItcPanel.WarningIcon)
        case _                              => None
      }
    }

  protected def sourceMag       (i: ItcParameters) = f"${i.source.norm}%.2f ${i.source.normBand.name}"

  protected def sourceFraction  (i: ItcParameters) = f"${i.observation.sourceFraction}%.2f"

  protected def peakPixelFlux(result: Future[ItcService.Result], ccd: Int = 0) = serviceResult(result).map(_.peakPixelFlux(ccd))

  protected def imgPercentWell  (result: Future[ItcService.Result], ccd: Int = 0) = serviceResult(result).map(_.ccd(ccd).percentFullWell)
  protected def imgAdu          (result: Future[ItcService.Result], ccd: Int = 0) = serviceResult(result).map(_.ccd(ccd).adu)
  protected def singleSNRatio   (result: Future[ItcService.Result], ccd: Int = 0) = serviceResult(result).map(_.ccd(ccd).singleSNRatio)
  protected def totalSNRatio    (result: Future[ItcService.Result], ccd: Int = 0) = serviceResult(result).map(_.ccd(ccd).totalSNRatio)

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

    column(col) match {
      case Some(c) => multiLineHeader(c.label, "\n")
      case None    => multiLineHeader(StringUtil.toDisplayName(toKey(col).getName), " ") // create column name for key columns
    }
  }

  def tooltip(col: Int): String = column(col) match {
    case Some(c) => c.tooltip
    case None    => null  // no tooltip for key columns
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

}


/** Generic ITC imaging tables model. */
sealed trait ItcImagingTableModel extends ItcTableModel

class ItcGenericImagingTableModel(val keys: List[ItemKey], val uniqueSteps: List[ItcUniqueConfig], val inputs: List[String\/ItcParameters], val res: List[Future[ItcService.Result]], showCoadds: Boolean = false) extends ItcImagingTableModel {
  val headers = if (showCoadds) HeadersWithCoadds else Headers
  val results = PeakColumns ++ SNColumns
}

/** GMOS specific ITC imaging table model. */
class ItcGmosImagingTableModel(val keys: List[ItemKey], val uniqueSteps: List[ItcUniqueConfig], val inputs: List[String\/ItcParameters], val res: List[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = Headers
  val results = List(
    Column("CCD1 Peak\n(e-)",           (c, i, r) => peakPixelFlux  (r, ccd=0),   tooltip = ItcTableModel.PeakPixelETooltip),
    Column("CCD1 Peak\n(ADU)",          (c, i, r) => imgAdu         (r, ccd=0),   tooltip = ItcTableModel.PeakPixelAduTooltip),
    Column("CCD1 Peak\n(% Full Well)",  (c, i, r) => imgPercentWell (r, ccd=0),   tooltip = ItcTableModel.PeakPixelFWTooltip),
    Column("CCD1\nS/N Single",          (c, i, r) => singleSNRatio  (r, ccd=0)),
    Column("CCD1\nS/N Total",           (c, i, r) => totalSNRatio   (r, ccd=0)),

    Column("CCD2 Peak\n(e-)",           (c, i, r) => peakPixelFlux  (r, ccd=1),   tooltip = ItcTableModel.PeakPixelETooltip),
    Column("CCD2 Peak\n(ADU)",          (c, i, r) => imgAdu         (r, ccd=1),   tooltip = ItcTableModel.PeakPixelAduTooltip),
    Column("CCD2 Peak\n(% Full Well)",  (c, i, r) => imgPercentWell (r, ccd=1),   tooltip = ItcTableModel.PeakPixelFWTooltip),
    Column("CCD2\nS/N Single",          (c, i, r) => singleSNRatio  (r, ccd=1)),
    Column("CCD2\nS/N Total",           (c, i, r) => totalSNRatio   (r, ccd=1)),

    Column("CCD3 Peak\n(e-)",           (c, i, r) => peakPixelFlux  (r, ccd=2),   tooltip = ItcTableModel.PeakPixelETooltip),
    Column("CCD3 Peak\n(ADU)",          (c, i, r) => imgAdu         (r, ccd=2),   tooltip = ItcTableModel.PeakPixelAduTooltip),
    Column("CCD3 Peak\n(% Full Well)",  (c, i, r) => imgPercentWell (r, ccd=2),   tooltip = ItcTableModel.PeakPixelFWTooltip),
    Column("CCD3\nS/N Single",          (c, i, r) => singleSNRatio  (r, ccd=2)),
    Column("CCD3\nS/N Total",           (c, i, r) => totalSNRatio   (r, ccd=2))
  )
}

class ItcGsaoiImagingTableModel(val keys: List[ItemKey], val uniqueSteps: List[ItcUniqueConfig], val inputs: List[String\/ItcParameters], val res: List[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = HeadersWithCoadds ++ List(
    Column("Strehl",          (c, i, r) => gems(i),                    tooltip = "Estimated Strehl and band")
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