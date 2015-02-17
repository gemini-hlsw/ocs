package edu.gemini.model.p1.immutable

import org.specs2.mutable._
import edu.gemini.model.p1.{mutable => M}
import scala.xml.XML
import java.io.InputStreamReader

class GracesBlueprintSpec extends SpecificationWithJUnit with SemesterProperties {

  "The Graces Blueprint" should {
    "has a fiber mode and a read mode" in {
      // trivial sanity tests
      val blueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER, M.GracesReadMode.NORMAL)
      blueprint.fiberMode must beEqualTo(M.GracesFiberMode.ONE_FIBER)
      blueprint.readMode must beEqualTo(M.GracesReadMode.NORMAL)
    }
    "never uses Lgs" in {
      val blueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER, M.GracesReadMode.NORMAL)
      blueprint.ao must beEqualTo(AoNone)
    }
    "has an appropriate public name" in {
      val blueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER, M.GracesReadMode.NORMAL)
      blueprint.name must beEqualTo("Graces 1 fiber (target only, R~67.5k) Normal (Gain=1.3e/ADU, Read noise=4.3e)")
    }
    "is a visitor" in {
      val blueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER, M.GracesReadMode.NORMAL)
      blueprint.visitor must beTrue
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the blueprint has a false attribute
      xml must \\("Graces") \"visitor" \> "true"
    }
    "export fiber mode and read mode to XML" in {
      val blueprint = GracesBlueprint(M.GracesFiberMode.ONE_FIBER, M.GracesReadMode.NORMAL)
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("fiberMode") \> "1 fiber (target only, R~67.5k)"
      xml must \\("readMode") \> "Normal (Gain=1.3e/ADU, Read noise=4.3e)"
    }
    "be possible to deserialize" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_graces.xml")))

      proposal.blueprints(0).visitor must beTrue
      proposal.blueprints must beEqualTo(GracesBlueprint(M.GracesFiberMode.ONE_FIBER, M.GracesReadMode.NORMAL) :: Nil)
    }
  }

}