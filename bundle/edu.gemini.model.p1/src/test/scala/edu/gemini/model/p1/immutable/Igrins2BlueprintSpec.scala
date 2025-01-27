package edu.gemini.model.p1.immutable

import java.io.InputStreamReader

import edu.gemini.model.p1.{mutable => M}
import org.specs2.matcher.XmlMatchers
import org.specs2.mutable.Specification

import scala.xml.XML

class Igrins2BlueprintSpec extends Specification with SemesterProperties with XmlMatchers {
  private val blueprints = Igrins2NoddingOption.values.zip(Igrins2TelluricStars.values).map{case (m, n) => Igrins2Blueprint(m, n)}

  // A proposal for Igrins2
  private val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_igrins2.xml")))

  "The Igrins2 Blueprint" should {
    "not use Ao" in {
      ((_: Igrins2Blueprint).ao must beEqualTo(AoNone)).forall(blueprints)
    }
    "not be a visitor instrument" in {
      ((_: Igrins2Blueprint).visitor must beFalse).forall(blueprints)
    }
    "export Igrins2 to XML" in {
      { (b: Igrins2Blueprint) =>
        val observation = Observation(Some(b), None, None, Band.BAND_1_2, None)
        val proposal    = Proposal.empty.copy(observations = observation :: Nil)
        val xml         = XML.loadString(ProposalIo.writeToString(proposal))

        // Verify that this has the tags we expect.
        xml must \\("igrins2")
        xml must \\("igrins2") \\ "Igrins2"
        xml must \\("Igrins2") \\ "name" \> b.name
        xml must \\("Igrins2") \\ "nodding" \> b.nodding.value
        xml must \\("Igrins2") \\ "telluricStars" \> b.telluricStars.value
        xml must \\("Igrins2") \ "visitor" \> "false"
      }.forall(blueprints)
    }
    "be possible to deserialize" in {
      // This proposal is configured with nod off to sky option.
      proposal.blueprints.head must beEqualTo(Igrins2Blueprint(Igrins2NoddingOption.NodToSky, Igrins2TelluricStars.TwoStar))
    }
  }
}
