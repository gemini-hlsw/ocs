package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._
import edu.gemini.spModel.core.Angle


trait AngleCodec {

  implicit val AngleCodec: CodecJson[Angle] =
    CodecJson.derived[Double].xmap(Angle.fromDegrees)(_.toDegrees)

}

object angle extends AngleCodec
