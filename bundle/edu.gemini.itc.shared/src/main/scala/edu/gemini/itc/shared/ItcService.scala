package edu.gemini.itc.shared

import edu.gemini.spModel.core.Peer
import edu.gemini.util.trpc.client.TrpcClient

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.Color

import scalaz._
import Scalaz._

/** The data structures here are an attempt to unify the results produced by the different instrument recipes.
  * Results are either a few simple numbers in case of imaging or some numbers and a set of charts made up by
  * data series with (x,y) value pairs for spectroscopy. Note that some of the results are given on a per CCD
  * basis. For now GMOS is the only instrument with more than one CCD, GHOST may be the next one to support
  * several CCDs.
  *
  * The main purpose of the classes here is to serve as data transfer objects and to decouple the internal ITC
  * result representation (which contains many data types which are only relevant to ITC) from the service interface.
  * The internal result representations (ImagingResult and SpectroscopyResult) can potentially be replaced with
  * the result objects here in the future.
  */

/** Representation of relevant ITC calculation results for an instrument's CCD.
  * In particular the well depth and amp gain are CCD specific and can be different for different CCDs in the
  * same instrument (e.g. GMOS).
  */
final case class ItcCcd(
    singleSNRatio:          Double,     // the final SN ratio for a single image
    totalSNRatio:           Double,     // the total SN ratio for all images
    peakPixelFlux:          Double,     // the highest e- count for all pixels on the CCD
    wellDepth:              Double,     // the well depth (max e- count per pixel) for this CCD
    ampGain:                Double,     // the amplifier gain for this CCD (used to calculate ADU)
    warnings: List[ItcWarning]          // the warnings provided by ITC for this CCD
) {
  val percentFullWell: Double = peakPixelFlux / wellDepth * 100.0   // the max percentage of the well saturation for peak pixel
  val adu: Int                = (peakPixelFlux / ampGain).toInt     // the ADU value
}

sealed trait ItcResult extends Serializable {
  def ccds: List[ItcCcd]

  /**
    * Max value for a property across the CCDs
    */
  private def maxP[A](f: ItcCcd => A)(implicit a: scala.Ordering[A]): A = ccds.map(f).max

  def ccd(i: Int = 0): Option[ItcCcd]            = ccds.index(i % ccds.length)
  def peakPixelFlux(ccdIx: Int = 0): Option[Int] = ccd(ccdIx).map(_.peakPixelFlux.toInt)
  def maxPeakPixelFlux: Int                      = maxP(_.peakPixelFlux).toInt
  def maxAdu: Int                                = maxP(_.adu)
  def maxPercentFullWell: Double                 = maxP(_.percentFullWell)
  def maxSingleSNRatio: Double                   = maxP(_.singleSNRatio)
  def maxTotalSNRatio: Double                    = maxP(_.totalSNRatio)

  val warnings: List[ItcWarning] = {
    def compositeWarnings(i: (ItcCcd, Int)): List[ItcWarning] = i._1.warnings.map(w => ItcWarning(s"CCD ${i._2}: ${w.msg}"))

    def concatWarnings =
      ccds.zipWithIndex >>= compositeWarnings

    if (ccds.length > 1) concatWarnings
    else ccds.head.warnings
  }

}

// === IMAGING RESULTS

final case class ItcImagingResult(ccds: List[ItcCcd]) extends ItcResult

// === SPECTROSCOPY RESULTS

// There are two different types of charts
sealed trait SpcChartType
case object SignalChart       extends SpcChartType { val instance: SpcChartType = this } // signal and background over wavelength [nm]
case object S2NChart          extends SpcChartType { val instance: SpcChartType = this } // single and final S2N over wavelength [nm]
case object SignalPixelChart  extends SpcChartType { val instance: SpcChartType = this } // single and final S2N over wavelength [nm]

// There are four different data sets
sealed trait SpcDataType
case object SignalData        extends SpcDataType { val instance: SpcDataType = this }  // signal over wavelength [nm]
case object BackgroundData    extends SpcDataType { val instance: SpcDataType = this }  // background over wavelength [nm]
case object SingleS2NData     extends SpcDataType { val instance: SpcDataType = this }  // single S2N over wavelength [nm]
case object FinalS2NData      extends SpcDataType { val instance: SpcDataType = this }  // final S2N over wavelength [nm]
case object PixSigData        extends SpcDataType { val instance: SpcDataType = this }  // signal over pixels
case object PixBackData       extends SpcDataType { val instance: SpcDataType = this }  // background over pixels

/** Series of (x,y) data points used to create charts and text data files. */
final case class SpcSeriesData(dataType: SpcDataType, title: String, data: Array[Array[Double]], color: Option[Color] = None) {
  def x(i: Int): Double      = xValues(i)
  def y(i: Int): Double      = yValues(i)
  def xValues: Array[Double] = data(0)
  def yValues: Array[Double] = data(1)

  var displayInLegend = true

  def withLegendVisibility(visibility: Boolean): SpcSeriesData = {
    val ssdCopy = this.copy()
    ssdCopy.displayInLegend = visibility
    ssdCopy
  }

  override def equals(other: Any): Boolean =
    other match {
      case SpcSeriesData(`dataType`, `title`, arr, `color`) =>
        (arr corresponds data)(_ sameElements _)
      case _ => false
    }

}

/** Companion object for SpcSeriesData, used to instantiate specialized cases */
object SpcSeriesData {
  /** returns a new instance of SpcSeriesData, setting the displayInLegend attribute */
  def withVisibility(visibility: Boolean, dataType: SpcDataType, title: String, data: Array[Array[Double]], color: Option[Color] = None): SpcSeriesData = {
    val ssd = new SpcSeriesData(dataType, title, data, color)
    ssd.withLegendVisibility(visibility)
  }
}

final case class ChartAxisRange(start: Double, end: Double)
final case class ChartAxis(label: String, inverted: Boolean = false, range: Option[ChartAxisRange] = None)

object ChartAxis {
  // Java helper
  def apply(label: String) = new ChartAxis(label)
}

/** Multiple charts can be grouped.
  * This is for example useful to stack IFU results for different offsets on top of each other in the OT.
  * (At a later stage we maybe also want to add group labels like IFU offsets etc instead of repeating that
  * information in every chart title.)*/
final case class SpcChartGroup(charts: List[SpcChartData])

/** Charts are made up of a set of data series which are all plotted in the same XY-plot. */
final case class SpcChartData(chartType: SpcChartType, title: String, xAxis: ChartAxis, yAxis: ChartAxis, series: List[SpcSeriesData], axes: List[ChartAxis] = List()) {
  // JFreeChart requires a unique name for each series
  require(series.map(_.title).distinct.size == series.size, "titles of series are not unique")

  /** Gets all data series for the given type. */
  def allSeries(t: SpcDataType): List[SpcSeriesData] = series.filter(_.dataType == t)

  /** Gets all data series for the given type as Java lists. */
  def allSeriesAsJava(t: SpcDataType): java.util.List[SpcSeriesData] = series.filter(_.dataType == t)
}

/** The result of a spectroscopy ITC calculation contains some numbers per CCD and a set of groups of charts.
  * Individual charts and data series can be referenced by their types and group index. For most instruments there
  * is only one chart and data series of each type, however for NIFS and GMOS there will be several charts
  * of each type for each IFU element. */
final case class ItcSpectroscopyResult(ccds: List[ItcCcd], chartGroups: List[SpcChartGroup]) extends ItcResult {

  /** Gets chart data by type and its group index.
    * This method will fail if the result you're looking for does not exist.
    */
  def chart(t: SpcChartType, i: Int = 0): SpcChartData = chartGroups(i).charts.filter(_.chartType == t).head

  /** Gets all data series by chart type and data type.
    * This method will fail if the result (chart/data) you're looking for does not exist.
    */
  def allSeries(ct: SpcChartType, dt: SpcDataType): List[SpcSeriesData] = chart(ct).allSeries(dt)

}

object SpcChartData {
  def apply(chartType: SpcChartType, title: String, xAxisLabel: String, yAxisLabel: String, series: List[SpcSeriesData]) =
    new SpcChartData(chartType, title, ChartAxis(xAxisLabel), ChartAxis(yAxisLabel), series, List())
}

object ItcResult {

  import edu.gemini.itc.shared.ItcService._

  /** Creates an ITC result in case of an error. */
  def forException(e: Throwable): Result = ItcError(e.getMessage).left

  /** Creates an ITC result with a single problem/error message. */
  def forMessage(msg: String): Result = ItcError(msg).left

  /** Creates an ITC result for a result. */
  def forResult(result: ItcResult): Result = result.right

}

/**
 * Service interface for ITC calculations.
 */
trait ItcService {

  import edu.gemini.itc.shared.ItcService._

  /**
   * Perform the ITC calculation if possible, and return an answer or a reason for failure.
   * @param p parameters for the ITC calculation.
   * @param headless pass `true` for headless applications that do not require chart data.
   */
  def calculate(p: ItcParameters, headless: Boolean): Result

}

sealed trait ItcMessage
final case class ItcError(msg: String) extends ItcMessage
final case class ItcWarning(msg: String) extends ItcMessage

case class ItcParameters(
              source: SourceDefinition,
              observation: ObservationDetails,
              conditions: ObservingConditions,
              telescope: TelescopeDetails,
              instrument: InstrumentDetails)

object ItcService {

  type Result = ItcError \/ ItcResult

  /** Performs an ITC call on the given host. */
  def calculate(peer: Peer, inputs: ItcParameters): Future[Result] =
    TrpcClient(peer).withoutKeys future { r =>
      r[ItcService].calculate(inputs, false)
    }

}
