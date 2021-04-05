package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._

import edu.gemini.spModel.core._

import scalaz._
import Scalaz._

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

  implicit val MagnitudeBandCodec: CodecJson[MagnitudeBand] =
    keyedCodec(_.name, MagnitudeBand.fromString)

  implicit val MagnitudeSystemCodec: CodecJson[MagnitudeSystem] =
    keyedCodec(_.name, MagnitudeSystem.fromString)

  implicit val MagnitudeCodec: CodecJson[Magnitude] =
    casecodec4(Magnitude.apply, Magnitude.unapply)(
      "value",
      "band",
      "error",
      "system"
    )

  implicit val SiderealTargetEncoder: EncodeJson[SiderealTarget] =
    EncodeJson((s: SiderealTarget) =>
      ("magnitudes"   := s.magnitudes         ) ->:
      ("parallax"     := s.parallax.map(_.mas)) ->:
      ("redshift"     := s.redshift.map(_.z)  ) ->:
      ("properMotion" := s.properMotion       ) ->:
      ("coordinates"  := s.coordinates        ) ->:
      ("name"         := s.name               ) ->:
      jEmptyObject
    )

  implicit val SiderealTargetDecoder: DecodeJson[SiderealTarget] =
    DecodeJson { c =>
      for {
        n <- (c --\ "name"        ).as[String]
        o <- (c --\ "coordinates" ).as[Coordinates]
        p <- (c --\ "properMotion").as[Option[ProperMotion]]
        r <- (c --\ "redshift"    ).as[Option[Double]].map(_.map(z => Redshift(z)))
        x <- (c --\ "parallax"    ).as[Option[Double]].map(_.map(mas => Parallax.fromMas(mas).orZero))
        m <- (c --\ "magnitudes"  ).as[List[Magnitude]]
      } yield SiderealTarget(n, o, p, r, x, m, None, None)
    }

}

object siderealtarget extends SiderealTargetCodec
