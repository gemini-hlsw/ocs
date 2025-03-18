package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import edu.gemini.spModel.core._
import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ArbObservationDetails {
  import core._

  val genImagingS2N: Gen[ImagingS2N] =
    for {
      n <- arbitrary[Int]
      c <- arbitrary[Option[Int]]
      e <- arbitrary[Double]
      f <- arbitrary[Double]
      o <- arbitrary[Double]
    } yield ImagingS2N(n, c, e, f, o)

  val genSpectroscopyS2N: Gen[SpectroscopyS2N] =
    for {
      n <- arbitrary[Int]
      c <- arbitrary[Option[Int]]
      e <- arbitrary[Double]
      f <- arbitrary[Double]
      o <- arbitrary[Double]
      w <- arbitrary[Option[Double]]
    } yield SpectroscopyS2N(n, c, e, f, o, w)

  val genImagingInt: Gen[ImagingIntegrationTime] =
    for {
      s <- arbitrary[Double]
      c <- arbitrary[Option[Int]]
      f <- arbitrary[Double]
      o <- arbitrary[Double]
    } yield ImagingIntegrationTime(s, c, f, o)

  val genSpectroscopyInt: Gen[SpectroscopyIntegrationTime] =
    for {
      s <- arbitrary[Double]
      c <- arbitrary[Option[Int]]
      f <- arbitrary[Double]
      o <- arbitrary[Double]
      w <- arbitrary[Double]
    } yield SpectroscopyIntegrationTime(s, w, c, f, o)

  val genS2NMethod: Gen[S2NMethod] =
    Gen.oneOf(genImagingS2N, genSpectroscopyS2N)

  val genImagingExpCount: Gen[ImagingExposureCount] =
    for {
      s <- arbitrary[Double]
      e <- arbitrary[Double]
      c <- arbitrary[Option[Int]]
      f <- arbitrary[Double]
      o <- arbitrary[Double]
    } yield ImagingExposureCount(s, e, c, f, o)

  val genIntMethod: Gen[IntegrationTimeMethod] =
    Gen.oneOf(genImagingInt, genSpectroscopyInt)

  val genCalculationMethod: Gen[CalculationMethod] =
    Gen.oneOf(genS2NMethod, genIntMethod, genImagingExpCount)

  val genAutoAperture: Gen[AutoAperture] =
    arbitrary[Double].map(AutoAperture)

  val genUserAperture: Gen[UserAperture] =
    for {
      d  <- arbitrary[Double]
      sa <- arbitrary[Double]
    } yield UserAperture(d, sa)

  val genApertureMethod: Gen[ApertureMethod] =
    Gen.oneOf(genAutoAperture, genUserAperture)

  val genIfuSingle: Gen[IfuSingle] =
    for {
      fs <- arbitrary[Int]
      o  <- arbitrary[Double]
    } yield IfuSingle(fs, o)

  val genIfuRadial: Gen[IfuRadial] =
    for {
      fs  <- arbitrary[Int]
      min <- arbitrary[Double]
      max <- arbitrary[Double]
    } yield IfuRadial(fs, min, max)

  val genIfuSummed: Gen[IfuSummed] =
    for {
      fs <- arbitrary[Int]
      nx <- arbitrary[Int]
      ny <- arbitrary[Int]
      cx <- arbitrary[Double]
      cy <- arbitrary[Double]
    } yield IfuSummed(fs, nx, ny, cx, cy)

  val genIfuSum: Gen[IfuSum] =
    for {
      fs <- arbitrary[Int]
      n  <- arbitrary[Double]
      b  <- arbitrary[Boolean]
    } yield IfuSum(fs, n, b)

  val genIfuMethod: Gen[IfuMethod] =
    Gen.oneOf(genIfuSingle, genIfuRadial, genIfuSummed, genIfuSum)

  val genAnalysisMethod: Gen[AnalysisMethod] =
    Gen.oneOf(genApertureMethod, genIfuMethod)

  val genObservationDetails: Gen[ObservationDetails] =
    for {
      c <- genCalculationMethod
      a <- genAnalysisMethod
    } yield ObservationDetails(c, a)

  implicit val arbObservationDetails: Arbitrary[ObservationDetails] =
    Arbitrary(genObservationDetails)

}

object observationdetails extends ArbObservationDetails