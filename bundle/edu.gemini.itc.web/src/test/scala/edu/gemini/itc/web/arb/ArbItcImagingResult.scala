package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz.\/

trait ArbItcImagingResult {
  import itcccd._
  import small._

  val genItcImagingResult: Gen[ItcImagingResult] =
    Gen.smallNonEmptyListOf(arbitrary[ItcCcd]).map(ItcImagingResult)

  implicit val arbItcImagingResult: Arbitrary[ItcImagingResult] =
    Arbitrary(genItcImagingResult)

}

object itcimagingresult extends ArbItcImagingResult