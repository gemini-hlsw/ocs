package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.telescope.IssPort;

trait ItcParametersCodec {
  import keyed._
  import instrumentdetails._
  import observingconditions._
  import observationdetails._
  import sourcedefinition._

  implicit val TelescopeDetailsDecodeJson: CodecJson[TelescopeDetails] =
    codec3[TelescopeDetails.Coating, IssPort, GuideProbe.Type, TelescopeDetails](
      (c, p, g) => new TelescopeDetails(c, p, g),
      d => (d.getMirrorCoating, d.getInstrumentPort, d.getWFS)
    )("mirrorCoating", "instrumentPort", "wfs")

  implicit val ItcParametersDecodeJson: CodecJson[ItcParameters] =
    casecodec5(ItcParameters.apply, ItcParameters.unapply)(
      "source",
      "observation",
      "conditions",
      "telescope",
      "instrument"
    )

}

object itcrequest extends ItcParametersCodec