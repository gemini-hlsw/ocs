package edu.gemini.model.p1.immutable

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable.Specification

import scala.xml.XML
import scalaz.{-\/, \/-}

class FtProposalSpec extends Specification with SemesterProperties with XmlMatchers {
  "FT Proposals" should {
    "support a ngo partner as Partner Affiliation" in {
      val observationEnabled = Observation(None, None, None, Band.BAND_1_2, None)
        val proposal = Proposal.empty.copy(observations = observationEnabled :: Nil, proposalClass = FastTurnaroundProgramClass.empty.copy(partnerAffiliation = Option(-\/(NgoPartner.AR))))

        val xml = XML.loadString(ProposalIo.writeToString(proposal))

        // verify the exported value
        xml must \\("partnerAffiliation") \> "ar"
    }
    "support subaru as Partner Affiliation" in {
      val observationEnabled = Observation(None, None, None, Band.BAND_1_2, None)
        val proposal = Proposal.empty.copy(observations = observationEnabled :: Nil, proposalClass = FastTurnaroundProgramClass.empty.copy(partnerAffiliation = Option(\/-(ExchangePartner.SUBARU))))

        val xml = XML.loadString(ProposalIo.writeToString(proposal))

        // verify the exported value
        xml must \\("exchangeAffiliation") \> "subaru"
    }
  }
}
