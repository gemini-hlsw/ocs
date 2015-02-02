package edu.gemini.model.p1.immutable

import org.specs2.mutable._
import edu.gemini.model.p1.{mutable => M}
import scala.xml.XML
import java.io.InputStreamReader

class GracesBlueprintSpec extends SpecificationWithJUnit with SemesterProperties {

  "The Graces Blueprint" should {
    "has an observing mode and a disperser" in {
      // trivial sanity tests
      val blueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER)
      blueprint.fiberMode must beEqualTo(M.GracesFiberMode.ONE_FIBER)
    }
    "never uses Lgs" in {
      val blueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER)
      blueprint.ao must beEqualTo(AoNone)
    }
    "have an appropriate public name" in {
      val blueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER)
      blueprint.name must beEqualTo("Graces 1 fiber (target, R~48k)")
    }
    "is a visitor" in {
      val blueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER)
      blueprint.visitor must beTrue
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the blueprint has a false attribute
      xml must \\("Graces") \"visitor" \> "true"
    }
    "export fiber mode to XML" in {
      val blueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER)
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("fiberMode") \> "1 fiber (target, R~48k)"
    }
    "be possible to deserialize" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_graces.xml")))

      proposal.blueprints(0).visitor must beTrue
      proposal.blueprints must beEqualTo(GracesBlueprint(M.GracesFiberMode.ONE_FIBER) :: Nil)
    }
  }

}