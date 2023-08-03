package edu.gemini.model.p1.immutable

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable._
import edu.gemini.model.p1.{mutable => M}
import xml.XML
import util.parsing.input.StreamReader
import java.io.InputStreamReader
import org.specs2.scalaz.ValidationMatchers._

class ItacSpec extends Specification with SemesterProperties with XmlMatchers {
  "The ITAC XML deserialization" should {
    "support NGO Authority element missing" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_itac_no_ngoauthority.xml")))
      ProposalIo.validate(proposal) must beRight
    }
    "support NGO Authority element present" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_itac_with_ngoauthority.xml")))

      ProposalIo.validate(proposal) must beRight
    }
  }
  "The ITAC XML serialization" should {
    "preserve ngo authority" in {
      val proposal = ProposalIo.read(new InputStreamReader(classOf[ItacSpec].getResourceAsStream("proposal_itac_with_ngoauthority.xml")))

      // let's change title
      val xml = XML.loadString(ProposalIo.writeToString(proposal.copy(scheduling = "Changed scheduling`")))

      // verify the exported XML preserves the ngo authority
      xml must \\("ngoauthority") \> "cl"
    }
  }
}
