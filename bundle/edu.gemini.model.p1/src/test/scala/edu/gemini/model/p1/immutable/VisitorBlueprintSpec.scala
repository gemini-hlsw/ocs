package edu.gemini.model.p1.immutable

import java.io.InputStreamReader
import org.specs2.matcher.XmlMatchers
import org.specs2.mutable._
import scala.xml.XML
import scala.Some

class VisitorBlueprintSpec extends Specification with SemesterProperties with XmlMatchers {

  "The Visitor Blueprint" should {
    "does not use Ao" in {
      val blueprint = VisitorBlueprint(Site.GS, "")
      blueprint.ao must beEqualTo(AoNone)
    }
    "has an appropriate public name" in {
      val blueprint = VisitorBlueprint(Site.GS, "name")
      blueprint.name must beEqualTo("Visitor - Gemini South - name")
    }
    "has a site" in {
      VisitorBlueprint(Site.GS, "").site must beEqualTo(Site.GS)
      VisitorBlueprint(Site.GN, "").site must beEqualTo(Site.GN)
    }
    "is a visitor instrument" in {
      val blueprint = VisitorBlueprint(Site.GS, "")
      blueprint.visitor must beTrue
    }
    "exports to XML" in {
      val blueprint = VisitorBlueprint(Site.GS, "Instrument name")
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("visitor")
      xml must \\("visitor") \\"visitor"
      xml must \\("name") \> "Visitor - Gemini South - Instrument name"
      xml must \\("visitor") \"visitor" \> "true"
      xml must \\("site") \> "Gemini South"
      xml must \\("custom-name") \> "Instrument name"
    }
    "can be deserialized with site GS" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_visitor_gs.xml")))

      proposal.blueprints.head must beEqualTo(VisitorBlueprint(Site.GS, "My instrument"))
    }
    "can be deserialized with site GN" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_visitor_gn.xml")))

      proposal.blueprints.head must beEqualTo(VisitorBlueprint(Site.GN, "My instrument"))
    }
  }

}
