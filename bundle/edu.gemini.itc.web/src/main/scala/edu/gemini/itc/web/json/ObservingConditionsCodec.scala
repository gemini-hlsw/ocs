package edu.gemini.itc.web.json

import edu.gemini.itc.shared.{ExactCc, ExactIq, ObservingConditions}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._

import argonaut._
import Argonaut._
import scalaz._

trait ObservingConditionsCodec {
  import edu.gemini.json.keyed._

  private implicit val ImageQualityCodec: CodecJson[ImageQuality] =
    enumCodec[ImageQuality]

  private implicit val ExactImageQualityCodec: CodecJson[ExactIq] =
    casecodec1(ExactIq.apply, ExactIq.unapply)("arcsec")

  private implicit val CloudCoverCodec: CodecJson[CloudCover] =
    enumCodec[CloudCover]

  private implicit val ExactCloudCoverCodec: CodecJson[ExactCc] =
    casecodec1(ExactCc.apply, ExactCc.unapply)("extinction")

  private implicit val WaterVaporCodec: CodecJson[WaterVapor] =
    enumCodec[WaterVapor]

  private implicit val SkyBackgroundCodec: CodecJson[SkyBackground] =
    enumCodec[SkyBackground]

  implicit val EncodeJsonObservingConditions: EncodeJson[ObservingConditions] =
    EncodeJson { oc =>
      ("iq"      :=? oc.iq.toOption     ) ->?:
      ("exactiq" :=? oc.iq.swap.toOption) ->?:
      ("cc"      :=? oc.cc.toOption     ) ->?:
      ("exactcc" :=? oc.cc.swap.toOption) ->?:
      ("wv"      :=  oc.wv              ) ->:
      ("sb"      :=  oc.sb              ) ->:
      ("airmass" :=  oc.airmass         ) ->:
      jEmptyObject
    }

  private def disjunctionResult[A, B](name: String, h: CursorHistory, a: Option[A], b: Option[B]): DecodeResult[A \/ B] =
    b.fold(DecodeResult.fail[A \/ B](name, h))(b => DecodeResult.ok(\/.right[A, B](b))) |||
      a.fold(DecodeResult.fail[A \/ B](name, h))(a => DecodeResult.ok(\/.left[A, B](a)))

  implicit val DecodeJsonObservingConditions: DecodeJson[ObservingConditions] =
    DecodeJson { c =>
      for {
        enumIq  <- (c --\ "iq").as[Option[ImageQuality]]
        exactIq <- (c --\ "exactiq").as[Option[ExactIq]]
        iq      <- disjunctionResult("iq or exactiq", c.history, exactIq, enumIq)
        enumCc  <- (c --\ "cc").as[Option[CloudCover]]
        exactCc <- (c --\ "exactcc").as[Option[ExactCc]]
        cc      <- disjunctionResult("cc or exactcc", c.history, exactCc, enumCc)
        wv      <- (c --\ "wv").as[WaterVapor]
        sb      <- (c --\ "sb").as[SkyBackground]
        airmass <- (c --\ "airmass").as[Double]
      } yield ObservingConditions(iq, cc, wv, sb, airmass)
    }

}

object observingconditions extends ObservingConditionsCodec
