package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz.\/

trait ArbExposureCalculation {
  val genExposureCalculation: Gen[IntegrationTime] =
    for {
      time  <- arbitrary[Double]
      count <- arbitrary[Int]
    } yield IntegrationTime(time, count)

  implicit val arbExposureCalculation: Arbitrary[IntegrationTime] =
    Arbitrary(genExposureCalculation)

}

object exposurecalculation extends ArbExposureCalculation

