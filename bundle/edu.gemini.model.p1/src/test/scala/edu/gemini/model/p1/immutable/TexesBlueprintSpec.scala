package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import java.io.InputStreamReader
import org.specs2.mutable._
import scala.xml.XML
import org.specs2.scalaz.ValidationMatchers._
import scala.Some


class TexesBlueprintSpec extends SpecificationWithJUnit with SemesterProperties {

  "The Texes Blueprint" should {
    "be able to have a disperser" in {
      // trivial sanity test
      val blueprint = TexesBlueprint(M.TexesDisperser.D_32_LMM)
      blueprint.disperser must beEqualTo(M.TexesDisperser.D_32_LMM)
    }
    "does not use Ao" in {
      val blueprint = TexesBlueprint(M.TexesDisperser.D_32_LMM)
      blueprint.ao must beEqualTo(AoNone)
    }
    "has an appropriate public name" in {
      val blueprint = TexesBlueprint(M.TexesDisperser.D_32_LMM)
      blueprint.name must beEqualTo("Texes 32 l/mm echelle")
    }
    "includes the dispersers 'Echelon + 32 l/mm echelle' and 'Echelon + 75 l/mm grating'" in {
      val blueprint32 = TexesBlueprint(M.TexesDisperser.E_D_32_LMM)
      blueprint32.name must beEqualTo("Texes Echelon + 32 l/mm echelle")

      val blueprint75 = TexesBlueprint(M.TexesDisperser.E_D_75_LMM)
      blueprint75.name must beEqualTo("Texes Echelon + 75 l/mm grating")
    }
    "is a visitor instrument" in {
      val blueprint = TexesBlueprint(M.TexesDisperser.D_32_LMM)
      blueprint.visitor must beTrue
    }
    "export Texes to XML" in {
      val blueprint = TexesBlueprint(M.TexesDisperser.D_32_LMM)
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("texes")
      xml must \\("texes") \\("Texes")
      xml must \\("Texes") \\("name") \> ("Texes 32 l/mm echelle")
      xml must \\("Texes") \("visitor") \> "true"
      xml must \\("disperser") \> ("32 l/mm echelle")
    }
    "be possible to deserialize" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_texes.xml")))

      proposal.blueprints(0) must beEqualTo(TexesBlueprint(M.TexesDisperser.D_32_LMM))
    }
    "overwrite visitor as false" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_texes_as_non_visitor.xml")))

      proposal.blueprints(0).visitor must beTrue
      val xml = XML.loadString(ProposalIo.writeToString(proposal  ))

      // verify the blueprint has a true attribute
      xml must \\("Texes") \("visitor") \> "true"
    }
  }

}