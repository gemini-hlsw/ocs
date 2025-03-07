package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz.\/

trait ArbExposureCalculation {
  val genExposureCalculation: Gen[TotalExposure] =
    for {
      time  <- arbitrary[Double]
      count <- arbitrary[Int]
    } yield TotalExposure(time, count)

  implicit val arbExposureCalculation: Arbitrary[TotalExposure] =
    Arbitrary(genExposureCalculation)

}

object exposurecalculation extends ArbExposureCalculation

