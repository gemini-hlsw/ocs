package edu.gemini.model.p1.immutable

import java.io.InputStreamReader

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable.Specification

import scala.xml.XML

class IgrinsBlueprintSpec extends Specification with SemesterProperties with XmlMatchers {
  private val blueprints = List(IgrinsBlueprint())

  // A proposal for IGRINS with visitor set to false.
  private val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_igrins.xml")))

  "The IGRINS Blueprint" should {
    "not use Ao" in {
      ((_: IgrinsBlueprint).ao must beEqualTo(AoNone)).forall(blueprints)
    }
    "be a visitor instrument" in {
      ((_: IgrinsBlueprint).visitor must beTrue).forall(blueprints)
    }
    "export IGRINS to XML" in {
      { (b: IgrinsBlueprint) =>
        val observation = Observation(Some(b), None, None, Band.BAND_1_2, None)
        val proposal    = Proposal.empty.copy(observations = observation :: Nil)
        val xml         = XML.loadString(ProposalIo.writeToString(proposal))

        // Verify that this has the tags we expect.
        xml must \\("igrins")
        xml must \\("igrins") \\ "Igrins"
        xml must \\("Igrins") \\ "name" \> b.name
        xml must \\("Igrins") \ "visitor" \> "true"
      }.forall(blueprints)
    }
    "be possible to deserialize" in {
      // This is configured with speckle mode.
      proposal.blueprints.head must beEqualTo(IgrinsBlueprint())
    }
    "overwrite visitor as true" in {
      // Even though it is false in the XML, it should become true in the model.
      proposal.blueprints.head.visitor must beTrue
      val xml = XML.loadString(ProposalIo.writeToString(proposal))
      xml must \\("Igrins") \ "visitor" \> "true"
    }
  }
}