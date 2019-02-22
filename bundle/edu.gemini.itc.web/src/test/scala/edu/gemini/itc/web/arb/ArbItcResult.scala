package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ArbItcResult {
  import itcimagingresult._
  import itcspectroscopyresult._

  val genItcResult: Gen[ItcResult] =
    Gen.oneOf(
      arbitrary[ItcImagingResult],
      arbitrary[ItcSpectroscopyResult]
    )

  implicit val arbItcResult: Arbitrary[ItcResult] =
    Arbitrary(genItcResult)

}

object itcresult extends ArbItcResult