package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared.ObservingConditions
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality._

trait ObservingConditionsCodec {
  import keyed._

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