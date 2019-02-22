package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz.\/

trait ArbItcCcd {
  import small._

  val genItcWarning: Gen[ItcWarning] =
    arbitrary[String].map(ItcWarning)

  val genItcCcd: Gen[ItcCcd] =
    for {
      singleSNRatio <- arbitrary[Double]
      totalSNRatio  <- arbitrary[Double]
      peakPixelFlux <- arbitrary[Double]
      wellDepth     <- arbitrary[Double]
      ampGain       <- arbitrary[Double]
      warnings      <- Gen.smallListOf(genItcWarning)
    } yield ItcCcd(singleSNRatio, totalSNRatio, peakPixelFlux, wellDepth, ampGain, warnings)

  implicit val arbItcCcd: Arbitrary[ItcCcd] =
    Arbitrary(genItcCcd)

}

object itcccd extends ArbItcCcd