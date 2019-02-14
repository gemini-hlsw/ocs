package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._

trait ItcRequestCodec {
  import instrumentdetails._
  import observingconditions._
  import sourcedefinition._

  implicit val ObservationDetailsDecodeJson: CodecJson[ObservationDetails] =
    ???

  implicit val TelescopeDetailsDecodeJson: CodecJson[TelescopeDetails] =
    ???

  implicit val ItcParametersDecodeJson: CodecJson[ItcParameters] =
    casecodec5(ItcParameters.apply, ItcParameters.unapply)(
      "source",
      "observation",
      "conditions",
      "telescope",
      "instrument"
    )

}

object itcrequest extends ItcRequestCodec