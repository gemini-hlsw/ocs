package jsky.app.ot.editor.seq

import javax.swing.Icon
import javax.swing.table.AbstractTableModel

import edu.gemini.itc.shared._
import edu.gemini.shared.util.StringUtil
import edu.gemini.spModel.config2.ItemKey

import scala.concurrent.Future
import scala.util.{Failure, Success}

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
    Column("Data Labels",     (c, r) => (resultIcon(r).orNull, c.labels)),
    Column("Images",          (c, r) => s"${c.count}",                      tooltip = "Number of exposures used in S/N calculation"),
    Column("Exposure Time",   (c, r) => f"${c.singleExposureTime}%.1f",     tooltip = "Exposure time of each image [s]"),
    Column("Total Exp. Time", (c, r) => f"${c.totalExposureTime}%.1f",      tooltip = "Total exposure time [s]")
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

  protected def resultIcon(f: Future[ItcService.Result]): Option[Icon] =
    f.value.fold {
      Some(ItcPanel.SpinnerIcon).asInstanceOf[Option[Icon]]
    } {
      case Failure(t)               => Some(ItcPanel.ErrorIcon)
      case Success(s) => s match {
        case scalaz.Failure(errs)   => Some(ItcPanel.ErrorIcon)
        case scalaz.Success(_)      => None
      }
    }

  protected def spcPeakElectrons(result: Future[ItcService.Result]) = spectroscopyResult(result).map(_.series(SignalChart, SignalData).yValues.max.toInt)

  protected def spcPeakSNSingle (result: Future[ItcService.Result]) = spectroscopyResult(result).map(_.series(S2NChart, SingleS2NData).yValues.max)

  protected def spcPeakSNFinal  (result: Future[ItcService.Result]) = spectroscopyResult(result).map(_.series(S2NChart, FinalS2NData).yValues.max)

  protected def spcSourceMag    (result: Future[ItcService.Result]) = spectroscopyResult(result).map(r => toMagString(r.source))

  // Gets the result from the service result future (if present)
  protected def serviceResult(f: Future[ItcService.Result]): Option[ItcResult] =
    for {
      futureResult  <- f.value                // unwrap future
      serviceResult <- futureResult.toOption  // unwrap try
      calcResult    <- serviceResult.toOption // unwrap validation
    } yield calcResult


  protected def imgPeakPixelFlux(result: Future[ItcService.Result], ccd: Int = 0) = imagingResult(result).map(_.ccd(ccd).peakPixelFlux.toInt)

  protected def imgSingleSNRatio(result: Future[ItcService.Result], ccd: Int = 0) = imagingResult(result).map(_.ccd(ccd).singleSNRatio)

  protected def imgTotalSNRatio (result: Future[ItcService.Result], ccd: Int = 0) = imagingResult(result).map(_.ccd(ccd).totalSNRatio)

  protected def imgSourceMag    (result: Future[ItcService.Result]) = imagingResult(result).map(r => toMagString(r.source))

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

  private def toMagString(s: SourceDefinition) = f"${s.norm}%.2f ${s.getNormBand.name}"

}


/** Generic ITC imaging tables model. */
sealed trait ItcImagingTableModel extends ItcTableModel

class ItcGenericImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = Headers ++ Seq(
    Column("Source Mag",      (c, r) => imgSourceMag(r),              tooltip = "Source magnitude [mag]")
    )
  val results = Seq(
    Column("Peak",            (c, r) => imgPeakPixelFlux(r),          tooltip = ItcTableModel.PeakPixelTooltip),
    Column("S/N Single",      (c, r) => imgSingleSNRatio(r)),
    Column("S/N Total",       (c, r) => imgTotalSNRatio (r))
  )
}

/** GMOS specific ITC imaging table model. */
class ItcGmosImagingTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcImagingTableModel {
  val headers = Headers ++ Seq(
    Column("Source Mag",      (c, r) => imgSourceMag(r),              tooltip = "Source magnitude [mag]")
  )
  val results = Seq(
    Column("CCD1 Peak",       (c, r) => imgPeakPixelFlux(r, ccd=0),   tooltip = ItcTableModel.PeakPixelTooltip + " for CCD 1"),
    Column("CCD1 S/N Single", (c, r) => imgSingleSNRatio(r, ccd=0)),
    Column("CCD1 S/N Total",  (c, r) => imgTotalSNRatio (r, ccd=0)),
    Column("CCD2 Peak",       (c, r) => imgPeakPixelFlux(r, ccd=1),   tooltip = ItcTableModel.PeakPixelTooltip + " for CCD 2"),
    Column("CCD2 S/N Single", (c, r) => imgSingleSNRatio(r, ccd=1)),
    Column("CCD2 S/N Total",  (c, r) => imgTotalSNRatio (r, ccd=1)),
    Column("CCD3 Peak",       (c, r) => imgPeakPixelFlux(r, ccd=2),   tooltip = ItcTableModel.PeakPixelTooltip + " for CCD 3"),
    Column("CCD3 S/N Single", (c, r) => imgSingleSNRatio(r, ccd=2)),
    Column("CCD3 S/N Total",  (c, r) => imgTotalSNRatio (r, ccd=2))
  )
}


/** Generic ITC spectroscopy table model. */
sealed trait ItcSpectroscopyTableModel extends ItcTableModel

class ItcGenericSpectroscopyTableModel(val keys: Seq[ItemKey], val uniqueSteps: Seq[ItcUniqueConfig], val res: Seq[Future[ItcService.Result]]) extends ItcSpectroscopyTableModel {
  val headers = Headers ++ Seq(
    Column("Source Mag",      (c, r) => spcSourceMag(r),              tooltip = "Source magnitude [mag]")
  )
  val results = Seq(
    Column("Peak",            (c, r) => spcPeakElectrons(r),          tooltip = "Peak e- per exposure"),
    Column("S/N Single",      (c, r) => spcPeakSNSingle(r)),
    Column("S/N Total",       (c, r) => spcPeakSNFinal(r))
  )

}