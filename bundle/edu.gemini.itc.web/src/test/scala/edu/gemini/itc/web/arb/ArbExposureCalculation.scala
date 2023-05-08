package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz.\/

trait ArbExposureCalculation {
  val genExposureCalculation: Gen[ExposureCalculation] =
    for {
      time  <- arbitrary[Double]
      count <- arbitrary[Int]
      sn    <- arbitrary[Double]
    } yield ExposureCalculation(time, count, sn)

  implicit val arbExposureCalculation: Arbitrary[ExposureCalculation] =
    Arbitrary(genExposureCalculation)

}

object exposurecalculation extends ArbExposureCalculation

