package edu.gemini.model.p1.immutable

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable._
import edu.gemini.model.p1.{mutable => M}
import xml.XML
import java.io.InputStreamReader
import org.specs2.scalaz.ValidationMatchers._

class ObservationSpec extends Specification with SemesterProperties with XmlMatchers {
  "The Observation class" should {
    "support the enable attribute and set it to true by default, REL-658" in {
      val proposal = Proposal.empty.copy(observations = ObservationSpec.observation :: Nil)

      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("observation", "enabled" -> "true")
    }
    "support the enable attribute as false, REL-658" in {
      val proposal = Proposal.empty.copy(observations = ObservationSpec.observation.copy(enabled = false) :: Nil)

      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("observation", "enabled" -> "false")
    }
    "preserve the enable attribute as false, REL-658" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_disabled_observations.xml")))

      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify that the exported value is preserved
      xml must \\("observation", "enabled" -> "false")
    }
  }
}

object ObservationSpec {
  // A non-empty observation. Empty observations are ignored, and not submittable to ITAC.
  val observation = Observation(Some(VisitorBlueprint(Site.GS, "Visitor Instrument")), None, None, Band.BAND_1_2, None)
}