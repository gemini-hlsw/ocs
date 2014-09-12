package edu.gemini.model.p1.immutable

import org.specs2.mutable._
import edu.gemini.model.p1.{mutable => M}
import xml.XML
import java.io.InputStreamReader
import org.specs2.scalaz.ValidationMatchers._

class ObservationSpec extends SpecificationWithJUnit with SemesterProperties {
  "The Observation class" should {
    "support the enable attribute and set it to true by default, REL-658" in {
      val observationEnabled = Observation(None, None, None, Band.BAND_1_2, None)
      val proposal = Proposal.empty.copy(observations = observationEnabled :: Nil)

      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("observation", "enabled" -> "true")
    }
    "support the enable attribute as false, REL-658" in {
      val observationEnabled = Observation(None, None, None, Band.BAND_1_2, None)
      val proposal = Proposal.empty.copy(observations = observationEnabled.copy(enabled = false) :: Nil)

      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("observation", "enabled" -> "false")
      true must beTrue
    }
    "preserve the enable attribute as false, REL-658" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_disabled_observations.xml")))

      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify that the exported value is preserved
      xml must \\("observation", "enabled" -> "false")
    }
  }
}