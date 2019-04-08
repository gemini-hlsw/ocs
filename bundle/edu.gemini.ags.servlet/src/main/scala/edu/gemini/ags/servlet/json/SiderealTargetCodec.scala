package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._

import edu.gemini.spModel.core._

trait SiderealTargetCodec {

  import edu.gemini.json.all._
  import coordinates._

  // We only care about a subset of the fields so we do a manual codec
  // implementation.

  implicit val SiderealTargetEncoder: EncodeJson[SiderealTarget] =
    EncodeJson((s: SiderealTarget) =>
      ("coordinates" := s.coordinates) ->:
      ("name"        := s.name       ) ->:
      jEmptyObject
    )

  implicit val SiderealTargetDecoder: DecodeJson[SiderealTarget] =
    DecodeJson { c =>
      for {
        name   <- (c --\ "name"       ).as[String]
        coords <- (c --\ "coordinates").as[Coordinates]
      } yield SiderealTarget(name, coords, None, None, None, Nil, None, None)
    }

  implicit val SiderealTargetCodec: CodecJson[SiderealTarget] =
    CodecJson.derived[SiderealTarget](SiderealTargetEncoder, SiderealTargetDecoder)

}

object siderealtarget extends SiderealTargetCodec
