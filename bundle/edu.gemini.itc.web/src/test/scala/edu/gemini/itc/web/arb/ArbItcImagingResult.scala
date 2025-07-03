package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import edu.gemini.itc.web.arb.itcspectroscopyresult.genSignalToNoiseAt
import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz.\/

trait ArbItcImagingResult {
  import itcccd._
  import exposurecalculation._
  import small._

  val genItcCcd: Gen[List[ItcCcd]] =
    Gen.smallNonEmptyListOf(arbitrary[ItcCcd])

  val genExposureCalculation: Gen[List[IntegrationTime]] =
    Gen.smallNonEmptyListOf(arbitrary[IntegrationTime])

  implicit val arbItcImagingResult: Arbitrary[ItcImagingResult] =
    Arbitrary {
      for {
        ccds  <- genItcCcd
        calcs <- genExposureCalculation.map(e => AllIntegrationTimes(e, 0))
        snAt        <- Gen.option(genSignalToNoiseAt)
      } yield ItcImagingResult(ccds, calcs, snAt)
    }

}

object itcimagingresult extends ArbItcImagingResult

