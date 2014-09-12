package edu.gemini.model.p1.immutable

import org.specs2.mutable._
import org.specs2.scalaz.ValidationMatchers._
import edu.gemini.model.p1.{mutable => M}
import scala.xml.XML
import java.io.InputStreamReader

//import edu.gemini.spModel.gemini.gsaoi.Gsaoi
//import edu.gemini.spModel.`type`.SpTypeUtil



class GsaoiBlueprintSpec extends SpecificationWithJUnit with SemesterProperties {

  "The Gsaoi Blueprint" should {
    "be able to have filters" in {
      // trivial sanity test
      val blueprint = GsaoiBlueprint(M.GsaoiFilter.J :: Nil)
      blueprint.filters must beEqualTo(M.GsaoiFilter.J :: Nil)
    }
    "always use Lgs" in {
      val blueprint = GsaoiBlueprint(M.GsaoiFilter.J :: Nil)
      blueprint.ao must beEqualTo(AoLgs)
    }
    "have an appropriate public name" in {
      val blueprint = GsaoiBlueprint(M.GsaoiFilter.J :: Nil)
      blueprint.name must beEqualTo("GSAOI J (1.250 um)")
    }
    "is not a visitor" in {
      val blueprint = GsaoiBlueprint(M.GsaoiFilter.J :: Nil)
      blueprint.visitor must beFalse
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the blueprint has a false attribute
      xml must \\("Gsaoi") \("visitor") \> "false"
    }
    "export J (1.250 um) Filter to XML" in {
      val blueprint = GsaoiBlueprint(M.GsaoiFilter.J :: Nil)
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("filter") \> ("J (1.250 um)")
    }
    "be possible to deserialize" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_gsaoi.xml")))

      proposal.blueprints(0).visitor must beFalse
      proposal.blueprints(0) must beEqualTo(GsaoiBlueprint(GsaoiFilter.forName("K_SHORT") :: Nil))
    }
    "overwrite visitor as true" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_gsaoi_as_visitor.xml")))

      // Even though it is true in the xml it becomes false in the logic
      proposal.blueprints(0).visitor must beFalse
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the blueprint has a false attribute
      xml must \\("Gsaoi") \("visitor") \> "false"
    }
  }


  /*
  // Commented out to avoid polluting the pit with old ocs jars
  // TODO move to ./osgi/bundle/phase2/
  "The Gsoai Blueprint Filters" should {
    "match the GSOAI.Filters enumeration" in {
      val enumNotInOT = M.GsaoiFilter.values() map {`oae
        f => Option(SpTypeUtil.noExceptionValueOf(classOf[Gsaoi.Filter], f.toString))
      } filter {
        _.isEmpty
      }

      enumNotInOT must be empty
    }
  }*/

}