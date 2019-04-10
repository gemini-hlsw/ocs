package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._
import edu.gemini.skycalc.{ Angle, Offset }


trait OffsetCodec {

  implicit val SkycalcAngleCodec: CodecJson[Angle] =
    CodecJson.derived[Double].xmap(d => new Angle(d, Angle.Unit.DEGREES))(_.toDegrees.getMagnitude)

  implicit val OffsetEncoder: EncodeJson[Offset] =
    EncodeJson((o: Offset) =>
      ("p" := o.p) ->:
      ("q" := o.q) ->:
      jEmptyObject
    )

  implicit val OffsetDecoder: DecodeJson[Offset] =
    DecodeJson { c =>
      for {
        p <- (c --\ "p").as[Angle]
        q <- (c --\ "q").as[Angle]
      } yield new Offset(p, q)
    }

}

object offset extends OffsetCodec
