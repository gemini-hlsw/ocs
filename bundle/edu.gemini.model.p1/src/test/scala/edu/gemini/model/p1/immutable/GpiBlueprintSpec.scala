package edu.gemini.model.p1.immutable

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable._
import edu.gemini.model.p1.{mutable => M}
import scala.xml.XML
import java.io.InputStreamReader

class GpiBlueprintSpec extends Specification with SemesterProperties with XmlMatchers {

  "The Gpi Blueprint" should {
    "has an observing mode and a disperser" in {
      // trivial sanity tests
      val blueprint = GpiBlueprint(M.GpiObservingMode.CORON_Y_BAND, M.GpiDisperser.PRISM)
      blueprint.observingMode must beEqualTo(M.GpiObservingMode.CORON_Y_BAND)
      blueprint.disperser must beEqualTo(M.GpiDisperser.PRISM)
    }
    "never uses Lgs" in {
      val blueprint = GpiBlueprint(M.GpiObservingMode.CORON_Y_BAND, M.GpiDisperser.PRISM)
      blueprint.ao must beEqualTo(AoNone)
    }
    "have an appropriate public name" in {
      val blueprint = GpiBlueprint(M.GpiObservingMode.CORON_Y_BAND, M.GpiDisperser.PRISM)
      blueprint.name must beEqualTo("GPI Coronograph Y-band Prism")
    }
    "is not a visitor" in {
      val blueprint = GpiBlueprint(M.GpiObservingMode.CORON_Y_BAND, M.GpiDisperser.PRISM)
      blueprint.visitor must beFalse
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the blueprint has a false attribute
      xml must \\("Gpi") \ "visitor" \> "false"
    }
    "export observing mode and disperser to XML" in {
      val blueprint = GpiBlueprint(M.GpiObservingMode.CORON_Y_BAND, M.GpiDisperser.PRISM)
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("observingMode") \> "Coronograph Y-band"
      xml must \\("disperser") \> "Prism"
    }
    "be possible to deserialize" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_gpi.xml")))

      proposal.blueprints(0).visitor must beFalse
      proposal.blueprints must beEqualTo(GpiBlueprint(M.GpiObservingMode.CORON_Y_BAND, M.GpiDisperser.PRISM) :: Nil)
    }
  }

  "The Gpi Observing mode" should {
    "tell if it is a Coronograph" in {
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.CORON_H_BAND) must beTrue
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.CORON_J_BAND) must beTrue
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.CORON_K1_BAND) must beTrue
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.CORON_K2_BAND) must beTrue
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.CORON_Y_BAND) must beTrue
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.DIRECT_H_BAND) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.DIRECT_J_BAND) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.DIRECT_K1_BAND) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.DIRECT_K2_BAND) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.DIRECT_Y_BAND) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.H_LIWA) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.H_STAR) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.NRM_H) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.NRM_J) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.NRM_K1) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.NRM_K2) must beFalse
      GpiObservingMode.isCoronographMode(M.GpiObservingMode.NRM_Y) must beFalse
    }
    "read the science band" in {
      GpiObservingMode.scienceBand(M.GpiObservingMode.CORON_H_BAND) must beSome("H")
      GpiObservingMode.scienceBand(M.GpiObservingMode.CORON_J_BAND) must beSome("J")
      GpiObservingMode.scienceBand(M.GpiObservingMode.CORON_K1_BAND) must beSome("K1")
      GpiObservingMode.scienceBand(M.GpiObservingMode.CORON_K2_BAND) must beSome("K2")
      GpiObservingMode.scienceBand(M.GpiObservingMode.CORON_Y_BAND) must beSome("Y")
      GpiObservingMode.scienceBand(M.GpiObservingMode.DIRECT_H_BAND) must beSome("H")
      GpiObservingMode.scienceBand(M.GpiObservingMode.DIRECT_J_BAND) must beSome("J")
      GpiObservingMode.scienceBand(M.GpiObservingMode.DIRECT_K1_BAND) must beSome("K1")
      GpiObservingMode.scienceBand(M.GpiObservingMode.DIRECT_K2_BAND) must beSome("K2")
      GpiObservingMode.scienceBand(M.GpiObservingMode.DIRECT_Y_BAND) must beSome("Y")
      GpiObservingMode.scienceBand(M.GpiObservingMode.H_LIWA) must beSome("H")
      GpiObservingMode.scienceBand(M.GpiObservingMode.H_STAR) must beSome("H")
      GpiObservingMode.scienceBand(M.GpiObservingMode.NRM_H) must beSome("H")
      GpiObservingMode.scienceBand(M.GpiObservingMode.NRM_J) must beSome("J")
      GpiObservingMode.scienceBand(M.GpiObservingMode.NRM_K1) must beSome("K1")
      GpiObservingMode.scienceBand(M.GpiObservingMode.NRM_K2) must beSome("K2")
      GpiObservingMode.scienceBand(M.GpiObservingMode.NRM_Y) must beSome("Y")
    }
  }

}