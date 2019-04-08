package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._

import edu.gemini.spModel.core._


trait CoordinatesCodec {

  import edu.gemini.json.keyed._

  implicit val DecCodec: CodecJson[Declination] =
    keyedCodec(_.toAngle.formatDMS, (s: String) => Angle.parseDMS(s).toOption.flatMap(Declination.fromAngle))

  implicit val RaCodec: CodecJson[RightAscension] =
    keyedCodec(_.toAngle.formatHMS, (s: String) => Angle.parseHMS(s).toOption.map(RightAscension.fromAngle))

  implicit val CoordinatesCodec: CodecJson[Coordinates] =
    casecodec2(Coordinates.apply, Coordinates.unapply)(
      "ra",
      "dec"
    )
}

object coordinates extends CoordinatesCodec