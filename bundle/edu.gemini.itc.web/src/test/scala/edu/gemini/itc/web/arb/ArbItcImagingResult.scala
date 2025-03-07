package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz.\/

trait ArbItcImagingResult {
  import itcccd._
  import exposurecalculation._
  import small._

  val genItcCcd: Gen[List[ItcCcd]] =
    Gen.smallNonEmptyListOf(arbitrary[ItcCcd])

  val genExposureCalculation: Gen[List[TotalExposure]] =
    Gen.smallNonEmptyListOf(arbitrary[TotalExposure])

  implicit val arbItcImagingResult: Arbitrary[ItcImagingResult] =
    Arbitrary {
      for {
        ccds  <- genItcCcd
        calcs <- Gen.option(genExposureCalculation.map(e => AllExposureCalculations(e, 0)))
      } yield ItcImagingResult(ccds, calcs)
    }

}

object itcimagingresult extends ArbItcImagingResult

