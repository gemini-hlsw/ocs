package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz.\/

trait ArbItcSpectroscopyResult {
  import color._
  import itcccd._
  import small._

  val genSpcChartType: Gen[SpcChartType] =
    Gen.oneOf(
      SignalChart,
      S2NChart,
      SignalPixelChart
    )

  val genChartAxisRange: Gen[ChartAxisRange] =
    for {
      start <- arbitrary[Double]
      end   <- arbitrary[Double]
    } yield ChartAxisRange(start, end)

  val genChartAxis: Gen[ChartAxis] =
    for {
      label    <- arbitrary[String]
      inverted <- arbitrary[Boolean]
      range    <- Gen.option(genChartAxisRange)
    } yield ChartAxis(label, inverted, range)

  val genSpcDataType: Gen[SpcDataType] =
    Gen.oneOf(
      SignalData,
      BackgroundData,
      SingleS2NData,
      FinalS2NData,
      PixSigData,
      PixBackData
    )

  val genSpcSeriesData: Gen[SpcSeriesData] =
    for {
      dataType <- genSpcDataType
      title    <- arbitrary[String]
      data     <- arbitrary[Array[Array[Double]]]
      color    <- Gen.option(genColor)
    } yield SpcSeriesData(dataType, title, data, color)

  // There is a constraint on SpcChartData that requires the series data elements to have distinct
  // titles. In order to do this I'm implementing distinctBy, which doesn't exist in stdlib.
  private implicit class ListOps[A](as: List[A]) {
    def distinctBy[B](f: A => B): List[A] =
      as.foldRight((Set.empty[B], List.empty[A])) { case (a, (set, accum)) =>
        val b = f(a)
        if (set(b)) (set, accum) else (set + b, a :: accum)
      } ._2
  }

  val genSpcChartData: Gen[SpcChartData] =
    for {
      chartType <- genSpcChartType
      title     <- arbitrary[String]
      xAxis     <- genChartAxis
      yAxis     <- genChartAxis
      series    <- Gen.smallListOf(genSpcSeriesData).map(_.distinctBy(_.title))
      axes      <- Gen.smallListOf(genChartAxis)
    } yield SpcChartData(chartType, title, xAxis, yAxis, series, axes)

  val genSpcChartGroup: Gen[SpcChartGroup] =
    Gen.smallListOf(genSpcChartData).map(SpcChartGroup)

  val genExposureCalculation: Gen[TotalExposure] =
    for {
      time  <- arbitrary[Double]
      count <- arbitrary[Int]
    } yield TotalExposure(time, count)

  val genSignalToNoiseAt: Gen[SignalToNoiseAt] =
    for {
      w <- arbitrary[Double]
      s <- arbitrary[Double]
      t <- arbitrary[Double]
    } yield SignalToNoiseAt(w, s, t)

  val genItcSpectroscopyResult: Gen[ItcSpectroscopyResult] =
    for {
      ccds        <- Gen.nonEmptyListOf(arbitrary[ItcCcd])
      chartGroups <- Gen.smallListOf(genSpcChartGroup)
      calcs       <- Gen.option(Gen.nonEmptyListOf(genExposureCalculation).map(e => AllExposureCalculations(e, 0)))
      snAt        <- Gen.option(genSignalToNoiseAt)
     } yield ItcSpectroscopyResult(ccds, chartGroups, calcs, snAt)

  implicit val arbItcSpectroscopyResult: Arbitrary[ItcSpectroscopyResult] =
    Arbitrary(genItcSpectroscopyResult)

}

object itcspectroscopyresult extends ArbItcSpectroscopyResult