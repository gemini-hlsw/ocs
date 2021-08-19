package edu.gemini.model.p1.immutable

import java.io.InputStreamReader

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable.Specification

import scala.xml.XML

class MaroonXBlueprintSpec extends Specification with SemesterProperties with XmlMatchers {
  private val blueprints = List(MaroonXBlueprint())

  // A proposal for MaroonX with visitor set to false.
  private val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_maroonx.xml")))

  "The MaroonX Blueprint" should {
    "not use Ao" in {
      ((_: MaroonXBlueprint).ao must beEqualTo(AoNone)).forall(blueprints)
    }
    "be a visitor instrument" in {
      ((_: MaroonXBlueprint).visitor must beTrue).forall(blueprints)
    }
    "export MaroonX to XML" in {
      { (b: MaroonXBlueprint) =>
        val observation = Observation(Some(b), None, None, Band.BAND_1_2, None)
        val proposal    = Proposal.empty.copy(observations = observation :: Nil)
        val xml         = XML.loadString(ProposalIo.writeToString(proposal))

        // Verify that this has the tags we expect.
        xml must \\("maroonx")
        xml must \\("maroonx") \\ "MaroonX"
        xml must \\("MaroonX") \\ "name" \> b.name
        xml must \\("MaroonX") \ "visitor" \> "true"
      }.forall(blueprints)
    }
    "be possible to deserialize" in {
      proposal.blueprints.head must beEqualTo(MaroonXBlueprint())
    }
    "overwrite visitor as true" in {
      proposal.blueprints.head.visitor must beTrue
      val xml = XML.loadString(ProposalIo.writeToString(proposal))
      xml must \\("MaroonX") \ "visitor" \> "true"
    }
  }
}
