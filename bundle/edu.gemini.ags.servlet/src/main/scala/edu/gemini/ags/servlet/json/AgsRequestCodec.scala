package edu.gemini.ags.servlet.json

import argonaut._, Argonaut._

import edu.gemini.ags.servlet._
import edu.gemini.ags.servlet.TargetType._
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, Conditions, ImageQuality, SkyBackground, WaterVapor}


trait AgsRequestCodec {

  import edu.gemini.json.all._
  import coordinates._

  private implicit val SiteCodec: CodecJson[Site] =
    enumCodec[Site]

  private implicit val TargetTypeCodec: CodecJson[TargetType] =
    keyedCodec(_.tag, TargetType.fromTag)

  private implicit val CloudCoverCodec    = enumCodec[CloudCover]
  private implicit val ImageQualityCodec  = enumCodec[ImageQuality]
  private implicit val SkyBackgroundCodec = enumCodec[SkyBackground]

  private implicit val ConditionsEncoder: EncodeJson[Conditions] =
    EncodeJson { (c: Conditions) =>
      ("cc" := c.cc) ->:
      ("iq" := c.iq) ->:
      ("sb" := c.sb) ->:
      jEmptyObject
    }

  private implicit val ConditionsDecoder: DecodeJson[Conditions] =
    DecodeJson { c =>
      for {
        cc <- (c --\ "cc").as[CloudCover]
        iq <- (c --\ "iq").as[ImageQuality]
        sb <- (c --\ "sb").as[SkyBackground]
      } yield new Conditions(cc, iq, sb, WaterVapor.ANY)
    }

  implicit val AgsRequestCodec: CodecJson[AgsRequest] =
    casecodec4(AgsRequest.apply, AgsRequest.unapply)(
      "site",
      "coordinates",
      "targetType",
      "conditions"
    )


}

object agsrequest extends AgsRequestCodec
