package edu.gemini.model.p1.immutable

import java.io.InputStreamReader

import edu.gemini.model.p1.{mutable => M}
import org.specs2.mutable._

import scala.xml.XML

class PhoenixBlueprintSpec extends SpecificationWithJUnit with SemesterProperties {

  "The Phoenix Blueprint" should {
    "has an FPU and a filter" in {
      // trivial sanity tests
      val blueprint = PhoenixBlueprint(M.PhoenixFocalPlaneUnit.MASK_1, List(M.PhoenixFilter.H6073))
      blueprint.fpu must beEqualTo(M.PhoenixFocalPlaneUnit.MASK_1)
      blueprint.filter must beEqualTo(List(M.PhoenixFilter.H6073))
    }
    "never uses Lgs" in {
      val blueprint = PhoenixBlueprint(M.PhoenixFocalPlaneUnit.MASK_1, List(M.PhoenixFilter.H6073))
      blueprint.ao must beEqualTo(AoNone)
    }
    "has an appropriate public name" in {
      val blueprint = PhoenixBlueprint(M.PhoenixFocalPlaneUnit.MASK_1, List(M.PhoenixFilter.H6073))
      blueprint.name must beEqualTo("Phoenix 0.17 arcsec slit H6073")
    }
    "is not a visitor" in {
      val blueprint = PhoenixBlueprint(M.PhoenixFocalPlaneUnit.MASK_1, List(M.PhoenixFilter.H6073))
      blueprint.visitor must beFalse
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the blueprint has a false attribute
      xml must \\("Phoenix") \ "visitor" \> "false"
    }
    "export fpu and filter to XML" in {
      val blueprint = PhoenixBlueprint(M.PhoenixFocalPlaneUnit.MASK_1, List(M.PhoenixFilter.H6073))
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("fpu") \> "0.17 arcsec slit"
      xml must \\("filter") \> "H6073"
    }
    "export fpu and multiple filters to XML" in {
      val blueprint = PhoenixBlueprint(M.PhoenixFocalPlaneUnit.MASK_1, List(M.PhoenixFilter.H6073, M.PhoenixFilter.H6420))
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("fpu") \> "0.17 arcsec slit"
      xml must \\("filter") \> "H6073"
      xml must \\("filter") \> "H6420"
    }
    "be possible to deserialize" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_phoenix.xml")))

      proposal.blueprints.head.visitor must beFalse
      proposal.blueprints must beEqualTo(PhoenixBlueprint(M.PhoenixFocalPlaneUnit.MASK_1, List(M.PhoenixFilter.H6073, M.PhoenixFilter.H6420)) :: Nil)
    }
  }

}