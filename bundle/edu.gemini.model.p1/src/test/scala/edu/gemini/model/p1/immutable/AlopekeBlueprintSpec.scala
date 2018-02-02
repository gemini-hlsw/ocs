package edu.gemini.model.p1.immutable

import java.io.InputStreamReader

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable.Specification

import scala.xml.XML

class AlopekeBlueprintSpec extends Specification with SemesterProperties with XmlMatchers {
  private val blueprints = AlopekeMode.values.map(m => AlopekeBlueprint(m))

  // A proposal for Alopeke with speckle mode and visitor set to false.
  private val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_alopeke.xml")))

  "The ʻAlopeke Blueprint" should {
    "not use Ao" in {
      ((_: AlopekeBlueprint).ao must beEqualTo(AoNone)).forall(blueprints)
    }
    "be a visitor instrument" in {
      ((_: AlopekeBlueprint).visitor must beTrue).forall(blueprints)
    }
    "have a mode" in {
      AlopekeBlueprint(AlopekeMode.SPECKLE).mode must beEqualTo(AlopekeMode.SPECKLE)
      AlopekeBlueprint(AlopekeMode.WIDE_FIELD).mode must beEqualTo(AlopekeMode.WIDE_FIELD)
      true must beTrue
    }
    "export Alopeke to XML" in {
      { (b: AlopekeBlueprint) =>
        val observation = Observation(Some(b), None, None, Band.BAND_1_2, None)
        val proposal    = Proposal.empty.copy(observations = observation :: Nil)
        val xml         = XML.loadString(ProposalIo.writeToString(proposal))

        // Verify that this has the tags we expect.
        xml must \\("alopeke")
        xml must \\("alopeke") \\ "Alopeke"
        xml must \\("Alopeke") \\ "name" \> b.name
        xml must \\("Alopeke") \\ "mode" \> b.mode.value
        xml must \\("Alopeke") \ "visitor" \> "true"
      }.forall(blueprints)
    }
    "be possible to deserialize" in {
      // This is configured with speckle mode.
      proposal.blueprints.head must beEqualTo(AlopekeBlueprint(AlopekeMode.SPECKLE))
    }
    "overwrite visitor as true" in {
      // Even though it is false in the XML, it should become true in the model.
      proposal.blueprints.head.visitor must beTrue
      val xml = XML.loadString(ProposalIo.writeToString(proposal))
      xml must \\("Alopeke") \ "visitor" \> "true"
    }
  }
}