package edu.gemini.model.p1.immutable

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable._
import edu.gemini.model.p1.{mutable => M}
import scala.xml.XML
//import edu.gemini.spModel.gemini.gnirs.GNIRSParams
//import edu.gemini.spModel.`type`.SpTypeUtil

class GNIRSBlueprintSpec extends Specification with SemesterProperties with XmlMatchers {

  "The Gnirs Blueprint Imaging" should {
    "include a Y filter, REL-630" in {
      // trivial sanity test
      val blueprint = GnirsBlueprintImaging(AltairNone, GnirsPixelScale.PS_005, M.GnirsFilter.Y)
      blueprint.filter must beEqualTo(M.GnirsFilter.Y)
    }
    "export Y Filter to XML, REL-630" in {
      val blueprint = GnirsBlueprintImaging(AltairNone, GnirsPixelScale.PS_005, M.GnirsFilter.Y)
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("filter") \> "Y (1.03um)"
    }
    "include the central wavelength on export to XML, REL-1254" in {
      val blueprint = GnirsBlueprintSpectroscopy(AltairNone, GnirsPixelScale.PS_005, GnirsDisperser.D_10, GnirsCrossDisperser.LXD, GnirsFpu.values(0), GnirsCentralWavelength.values(0))
      val observation = Observation(Some(blueprint), None, None, Band.BAND_1_2, None)

      val proposal = Proposal.empty.copy(observations = observation :: Nil)
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("centralWavelength") \> "< 2.5um"
    }
  }

  /*
  // Commented out to avoid polluting the pit with old ocs jars
  // TODO move to ./osgi/bundle/phase2/
  "The Gnirs Blueprint Imaging Filters" should {
    "match the GNIRSParams enumeration" in {
      val enumNotInOT = M.GnirsFilter.values() map {
        // Wrap it in SpTypeUtil to catch exceptions
        f => Option(SpTypeUtil.noExceptionValueOf(classOf[GNIRSParams.Filter], f.toString))
      } filter {
        _.isEmpty
      }

      enumNotInOT must be empty
    }
  }*/

}