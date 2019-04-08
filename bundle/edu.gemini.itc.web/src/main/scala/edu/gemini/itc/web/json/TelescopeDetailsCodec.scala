package edu.gemini.itc.web.json

import argonaut._, Argonaut._
import edu.gemini.itc.shared._
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.telescope.IssPort;

trait TelescopeDetailsCodec {
  import edu.gemini.json.keyed._

  private implicit val TelescopeDetailsCoatingCodec = enumCodec[TelescopeDetails.Coating]
  private implicit val IssPortCodec                 = enumCodec[IssPort]
  private implicit val GuideProbeTypeCodec          = enumCodec[GuideProbe.Type]

  implicit val TelescopeDetailsDecodeJson: CodecJson[TelescopeDetails] =
    codec3[TelescopeDetails.Coating, IssPort, GuideProbe.Type, TelescopeDetails](
      (c, p, g) => new TelescopeDetails(c, p, g),
      d => (d.getMirrorCoating, d.getInstrumentPort, d.getWFS)
    )("mirrorCoating", "instrumentPort", "wfs")

}

object telescopedetails extends TelescopeDetailsCodec