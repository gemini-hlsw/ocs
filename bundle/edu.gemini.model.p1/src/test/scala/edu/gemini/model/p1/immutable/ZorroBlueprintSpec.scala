package edu.gemini.model.p1.immutable

import java.io.InputStreamReader

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable.Specification

import scala.xml.XML

class ZorroBlueprintSpec extends Specification with SemesterProperties with XmlMatchers {
  private val blueprints = ZorroMode.values.map(m => ZorroBlueprint(m))

  // A proposal for Zorro with speckle mode and visitor set to false.
  private val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_zorro.xml")))

  "The 'Zorro Blueprint" should {
    "not use Ao" in {
      ((_: ZorroBlueprint).ao must beEqualTo(AoNone)).forall(blueprints)
    }
    "be a visitor instrument" in {
      ((_: ZorroBlueprint).visitor must beTrue).forall(blueprints)
    }
    "have a mode" in {
      ZorroBlueprint(ZorroMode.SPECKLE).mode must beEqualTo(ZorroMode.SPECKLE)
      ZorroBlueprint(ZorroMode.WIDE_FIELD).mode must beEqualTo(ZorroMode.WIDE_FIELD)
      true must beTrue
    }
    "export Zorro to XML" in {
      { (b: ZorroBlueprint) =>
        val observation = Observation(Some(b), None, None, Band.BAND_1_2, None)
        val proposal    = Proposal.empty.copy(observations = observation :: Nil)
        val xml         = XML.loadString(ProposalIo.writeToString(proposal))

        // Verify that this has the tags we expect.
        xml must \\("zorro")
        xml must \\("zorro") \\ "Zorro"
        xml must \\("Zorro") \\ "name" \> b.name
        xml must \\("Zorro") \\ "mode" \> b.mode.value
        xml must \\("Zorro") \ "visitor" \> "true"
      }.forall(blueprints)
    }
    "be possible to deserialize" in {
      // This is configured with speckle mode.
      proposal.blueprints.head must beEqualTo(ZorroBlueprint(ZorroMode.SPECKLE))
    }
    "overwrite visitor as true" in {
      // Even though it is false in the XML, it should become true in the model.
      proposal.blueprints.head.visitor must beTrue
      val xml = XML.loadString(ProposalIo.writeToString(proposal))
      xml must \\("Zorro") \ "visitor" \> "true"
    }
  }
}