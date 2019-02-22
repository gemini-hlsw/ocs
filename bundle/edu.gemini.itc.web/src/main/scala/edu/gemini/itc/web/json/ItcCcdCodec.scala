package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

trait ItcCcdCodec {

  implicit val ItcWarningCodec: CodecJson[ItcWarning] =
    casecodec1(ItcWarning.apply, ItcWarning.unapply)(
      "msg"
    )

  implicit val ItcCcdCodec: CodecJson[ItcCcd] =
    casecodec6(ItcCcd.apply, ItcCcd.unapply)(
      "singleSNRatio",
      "totalSNRatio",
      "peakPixelFlux",
      "wellDepth",
      "ampGain",
      "warnings"
    )

}

object itcccd extends ItcCcdCodec