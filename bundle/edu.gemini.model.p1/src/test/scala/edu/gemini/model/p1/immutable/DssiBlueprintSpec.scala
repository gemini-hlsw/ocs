package edu.gemini.model.p1.immutable

import java.io.InputStreamReader
import org.specs2.mutable._
import xml.XML

class DssiBlueprintSpec extends SpecificationWithJUnit with SemesterProperties {

  "The Dssi Blueprint" should {
    "not use Ao" in {
      val blueprint = DssiBlueprint(Site.GN)
      blueprint.ao must beEqualTo(AoNone)
    }
    "have an appropriate public name" in {
      val blueprint = DssiBlueprint(Site.GN)
      blueprint.name must beEqualTo("DSSI")
    }
    "is a visitor instrument" in {
      val blueprint = DssiBlueprint(Site.GN)
      blueprint.visitor must beTrue
    }
    "has a site" in {
      DssiBlueprint(Site.GS).site must beEqualTo(Site.GS)
      DssiBlueprint(Site.GN).site must beEqualTo(Site.GN)
    }
    "export Dssi to XML" in {
      val blueprint = DssiBlueprint(Site.GN)
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("dssi")
      xml must \\("dssi") \\ "Dssi"
      xml must \\("Dssi") \\ "name" \> "DSSI"
      xml must \\("Dssi") \\ "site" \> "Gemini North"
      xml must \\("Dssi") \ "visitor" \> "true"
    }
    "be possible to deserialize" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_dssi.xml")))

      proposal.blueprints.head must beEqualTo(DssiBlueprint(Site.GN))
    }
    "overwrite visitor as false" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_dssi_as_non_visitor.xml")))

      // Even though it is false in the xml it becomes true in the logic
      proposal.blueprints.head.visitor must beTrue
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the blueprint has a true attribute
      xml must \\("Dssi") \ "visitor" \> "true"
    }
  }

}