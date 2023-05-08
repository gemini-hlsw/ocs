package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

trait ItcImagingResultCodec {
  import edu.gemini.json.coproduct._
  import itcccd._

  val ItcImagingResultCodec: CodecJson[ItcImagingResult] =
    casecodec2(ItcImagingResult.apply, ItcImagingResult.unapply)(
      "ccds",
      "exposureCalculation"
    )

}

object itcimagingresult extends ItcImagingResultCodec
