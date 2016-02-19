package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}
import java.io.InputStreamReader
import org.specs2.matcher.XmlMatchers
import org.specs2.mutable._
import scala.xml.XML

class TexesBlueprintSpec extends Specification with SemesterProperties with XmlMatchers {

  "The Texes Blueprint" should {
    "be able to have a disperser" in {
      // trivial sanity test
      val blueprint = TexesBlueprint(Site.GN, M.TexesDisperser.D_32_LMM)
      blueprint.disperser must beEqualTo(M.TexesDisperser.D_32_LMM)
    }
    "does not use Ao" in {
      val blueprint = TexesBlueprint(Site.GN, M.TexesDisperser.D_32_LMM)
      blueprint.ao must beEqualTo(AoNone)
    }
    "has an appropriate public name" in {
      val blueprint = TexesBlueprint(Site.GN, M.TexesDisperser.D_32_LMM)
      blueprint.name must beEqualTo("Texes Gemini North 32 l/mm echelle")
    }
    "includes the dispersers 'Echelon + 32 l/mm echelle' and 'Echelon + 75 l/mm grating'" in {
      val blueprint32 = TexesBlueprint(Site.GN, M.TexesDisperser.E_D_32_LMM)
      blueprint32.name must beEqualTo("Texes Gemini North Echelon + 32 l/mm echelle")

      val blueprint75 = TexesBlueprint(Site.GN, M.TexesDisperser.E_D_75_LMM)
      blueprint75.name must beEqualTo("Texes Gemini North Echelon + 75 l/mm grating")
    }
    "is a visitor instrument" in {
      val blueprint = TexesBlueprint(Site.GN, M.TexesDisperser.D_32_LMM)
      blueprint.visitor must beTrue
    }
    "export Texes to XML" in {
      val blueprint = TexesBlueprint(Site.GN, M.TexesDisperser.D_32_LMM)
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("texes")
      xml must \\("texes") \\ "Texes"
      xml must \\("Texes") \\ "name" \> "Texes Gemini North 32 l/mm echelle"
      xml must \\("Texes") \ "visitor" \> "true"
      xml must \\("disperser") \> "32 l/mm echelle"
    }
    "be possible to deserialize" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_texes.xml")))

      proposal.blueprints.head must beEqualTo(TexesBlueprint(Site.GN, M.TexesDisperser.D_32_LMM))
    }
    "overwrite visitor as false" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_texes_as_non_visitor.xml")))

      proposal.blueprints.head.visitor must beTrue
      val xml = XML.loadString(ProposalIo.writeToString(proposal  ))

      // verify the blueprint has a true visitor attribute
      xml must \\("Texes") \ "visitor" \> "true"
    }
  }

}