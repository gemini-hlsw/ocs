package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._

import edu.gemini.ags.servlet.AgsResult

trait AgsResultCodec {

  import siderealtarget._

  implicit val AgsResultCodec: CodecJson[AgsResult] =
    casecodec1(AgsResult.apply, AgsResult.unapply)(
      "target"
    )

}

object agsresult extends AgsResultCodec
