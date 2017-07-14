package edu.gemini.model.p1.immutable

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable.Specification

import scala.xml.XML

class SIPProposalSpec extends Specification with SemesterProperties with XmlMatchers {
  "SIP Proposals" should {
    "support ToO" in {
      val observationEnabled = Observation(None, None, None, Band.BAND_1_2, None)
        val proposal = Proposal.empty.copy(observations = observationEnabled :: Nil, proposalClass = SubaruIntensiveProgramClass.empty.copy(tooOption = ToOChoice.Rapid))

        val xml = XML.loadString(ProposalIo.writeToString(proposal))

        // verify the exported value
        xml must \\("sip", "tooOption" -> "Rapid")
    }
  }
}
