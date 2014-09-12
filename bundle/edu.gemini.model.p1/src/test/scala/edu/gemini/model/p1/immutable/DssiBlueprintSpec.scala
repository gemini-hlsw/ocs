package edu.gemini.model.p1.immutable

import java.io.InputStreamReader
import org.specs2.mutable._
import xml.XML

class DssiBlueprintSpec extends SpecificationWithJUnit with SemesterProperties {

  "The Dssi Blueprint" should {
    "not use Ao" in {
      val blueprint = DssiBlueprint()
      blueprint.ao must beEqualTo(AoNone)
    }
    "have an appropriate public name" in {
      val blueprint = DssiBlueprint()
      blueprint.name must beEqualTo("DSSI")
    }
    "is a visitor instrument" in {
      val blueprint = DssiBlueprint()
      blueprint.visitor must beTrue
    }
    "export Dssi to XML" in {
      val blueprint = DssiBlueprint()
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("dssi")
      xml must \\("dssi") \\("Dssi")
      xml must \\("Dssi") \\("name") \> ("DSSI")
      xml must \\("Dssi") \("visitor") \> "true"
    }
    "be possible to deserialize" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_dssi.xml")))

      proposal.blueprints(0) must beEqualTo(DssiBlueprint())
    }
    "overwrite visitor as false" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_dssi_as_non_visitor.xml")))

      // Even though it is false in the xml it becomes true in the logic
      proposal.blueprints(0).visitor must beTrue
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the blueprint has a true attribute
      xml must \\("Dssi") \("visitor") \> "true"
    }
  }

}