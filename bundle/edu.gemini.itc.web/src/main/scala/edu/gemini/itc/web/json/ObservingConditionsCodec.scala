package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared.ObservingConditions
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._

trait ObservingConditionsCodec {
  import keyed._

  private implicit val ImageQualityCodec = enumCodec[ImageQuality]
  private implicit val CloudCoverCodec = enumCodec[CloudCover]
  private implicit val WaterVaporCodec = enumCodec[WaterVapor]
  private implicit val SkyBackgroundCodec = enumCodec[SkyBackground]


  implicit val ObservingConditionsCodec: CodecJson[ObservingConditions] =
    casecodec5(ObservingConditions.apply, ObservingConditions.unapply)(
      "iq",
      "cc",
      "wv",
      "sb",
      "airmass"
    )

}

object observingconditions extends ObservingConditionsCodec