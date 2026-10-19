package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gnirs.GNIRSParams
import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz.\/

trait ArbExposureCalculation {
  val genItcReadMode: Gen[ItcReadMode] =
    Gen.oneOf(
      Gen.oneOf(GNIRSParams.ReadMode.values.toSeq).map(ItcReadMode.Gnirs(_)),
      Gen.oneOf(Flamingos2.ReadMode.values.toSeq).map(ItcReadMode.Flamingos2(_))
    )

  val genExposureCalculation: Gen[IntegrationTime] =
    for {
      time     <- arbitrary[Double]
      count    <- arbitrary[Int]
      coadds   <- arbitrary[Int]
      readMode <- Gen.option(genItcReadMode)
    } yield IntegrationTime(time, count, coadds, readMode)

  implicit val arbExposureCalculation: Arbitrary[IntegrationTime] =
    Arbitrary(genExposureCalculation)

}

object exposurecalculation extends ArbExposureCalculation

