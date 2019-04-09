package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._

import edu.gemini.ags.servlet.AgsResult
import edu.gemini.spModel.core.Angle

trait AgsResultCodec {

  import siderealtarget._

  implicit val AngleCodec: CodecJson[Angle] =
    CodecJson.derived[Double].xmap(Angle.fromDegrees)(_.toDegrees)

  implicit val AgsResultCodec: CodecJson[AgsResult] =
    casecodec2(AgsResult.apply, AgsResult.unapply)(
      "posAngle",
      "target"
    )

}

object agsresult extends AgsResultCodec
