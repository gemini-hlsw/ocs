package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._

import edu.gemini.spModel.core._

trait SiderealTargetCodec {

  import edu.gemini.json.all._
  import coordinates._

  implicit val ProperMotionEncoder: EncodeJson[ProperMotion] =
    EncodeJson((p: ProperMotion) =>
      ("epoch"    := p.epoch.year                  ) ->:
      ("deltaDec" := p.deltaDec.velocity.masPerYear) ->:
      ("deltaRA"  := p.deltaRA.velocity.masPerYear ) ->:
      jEmptyObject
    )

  implicit val ProperMotionDecoder: DecodeJson[ProperMotion] =
    DecodeJson { c =>
      for {
        r <- (c --\ "deltaRA" ).as[Double].map(d => RightAscensionAngularVelocity(AngularVelocity(d)))
        d <- (c --\ "deltaDec").as[Double].map(d => DeclinationAngularVelocity(AngularVelocity(d)))
        e <- (c --\ "epoch"   ).as[Double].map(d => Epoch(d))
      } yield ProperMotion(r, d, e)
    }

  implicit val SiderealTargetEncoder: EncodeJson[SiderealTarget] =
    EncodeJson((s: SiderealTarget) =>
      ("properMotion" := s.properMotion) ->:
      ("coordinates"  := s.coordinates ) ->:
      ("name"         := s.name        ) ->:
      jEmptyObject
    )

  implicit val SiderealTargetDecoder: DecodeJson[SiderealTarget] =
    DecodeJson { c =>
      for {
        name   <- (c --\ "name"        ).as[String]
        coords <- (c --\ "coordinates" ).as[Coordinates]
        pm     <- (c --\ "properMotion").as[Option[ProperMotion]]
      } yield SiderealTarget(name, coords, pm, None, None, Nil, None, None)
    }

}

object siderealtarget extends SiderealTargetCodec
