package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.telescope.IssPort;

trait ItcParametersCodec {
  import instrumentdetails._
  import observingconditions._
  import observationdetails._
  import sourcedefinition._
  import telescopedetails._

  implicit val ItcParametersDecodeJson: CodecJson[ItcParameters] =
    casecodec5(ItcParameters.apply, ItcParameters.unapply)(
      "source",
      "observation",
      "conditions",
      "telescope",
      "instrument"
    )

}

object itcparameters extends ItcParametersCodec