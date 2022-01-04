package edu.gemini.model.p1.immutable

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable._
import xml.XML
import java.io.{File, InputStreamReader}

class ProposalSpec extends Specification with SemesterProperties with XmlMatchers {

  "The Proposal class" should {
    "support a free text scheduling field, REL-687" in {
      Proposal.empty.scheduling must beEmpty
      Proposal.empty.copy(scheduling = "text").scheduling must beEqualTo("text")
    }
    "use a schema version read from System properties" in {
      val proposal = Proposal.empty
      proposal.schemaVersion must beEqualTo("2022.2.1")
    }
    "set the band3optionChosen by default to false" in {
      val proposal = Proposal.empty
      proposal.meta.band3OptionChosen must beFalse
    }
  }
  "The Proposal XML serialization" should {
    "support missing schedule" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_itac_no_ngoauthority.xml")))

      ProposalIo.validate(proposal) must beRight
    }
    "include a scheduling field" in {
      val proposal = Proposal.empty.copy(scheduling = "text")
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("scheduling") \> "text"
    }
    "include a scheduling element if empty" in {
      val proposal = Proposal.empty
      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value
      xml must \\("scheduling") \> ""
    }
  }
  "The Schema XML deserialization" should {
    "set the semester to current upon saving a new proposal" in {
      val proposal = Proposal.empty

      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value is set to the current semester
      xml must \\("semester", "year" -> "2022", "half" -> "B")
    }
    "set the schemaVersion to current upon saving a new proposal" in {
      val proposal = Proposal.empty

      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported value is set to the current semester
      xml must \\("proposal", "schemaVersion" -> "2022.2.1")
    }
    "be able to open latin1 encoded files" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_latin1_encoding.xml")))

      ProposalIo.validate(proposal) must beRight
    }
    "be able to scrub non-valid xml chars, REL-2030" in {
      val proposal = ProposalIo.readAndConvert(new File(getClass.getResource("proposal_with_invalid_character.xml").getFile))

      proposal.isSuccess should beTrue
    }
  }
  "The Schema XML serialization version 1.0.14" should {
    "preserve the meta band3Option" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_band3option_false.xml")))

      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported attribute remains false
      xml must \\("meta", "band3optionChosen" -> "false")
    }
    "preserve the meta band3Option, part II" in {
      val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_with_band3option_true.xml")))

      val xml = XML.loadString(ProposalIo.writeToString(proposal))

      // verify the exported attribute remains true
      xml must \\("meta", "band3optionChosen" -> "true")
    }
  }
  "The Schema XML deserialization version 1.0.14 handling of version 1.0.0 proposals" should {
      "open proposals from version 1.0.0" in {
        val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0.xml")))

        ProposalIo.validate(proposal) must beRight
      }
      "open proposals from version 1.0.0 that include the uk as partner" in {
        val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0_with_uk.xml")))

        // just verify it exists, it cannot validate
        Some(proposal) must beSome
      }
      "preserve the schema version if not rolled" in {
        val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0.xml")))

        val xml = XML.loadString(ProposalIo.writeToString(proposal))

        // verify the exported value is set to the current semester
        xml must \\("proposal", "schemaVersion" -> "1.0.0")
      }
      "preserve the semester upon save" in {
        val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0.xml")))

        val xml = XML.loadString(ProposalIo.writeToString(proposal))

        // verify the exported value is set to the current semester
        xml must \\("semester", "year" -> "2012", "half" -> "B")
      }
      "must preserve the gsa, visibility and guiding" in {
        val proposal = ProposalIo.read(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0.xml")))

        proposal.observations.headOption must beSome
        proposal.observations.headOption.get.meta must beSome

        val xml = XML.loadString(ProposalIo.writeToString(proposal))

        // verify the exported xml preserves the guiding estimation
        xml must \\("meta") \"guiding" \"percentage" \> "69"
        // verify the exported xml preserves the guiding evaluation
        xml must \\("meta") \"guiding" \"evaluation" \> "Caution"
        // verify the exported xml preserves the visibility estimation
        xml must \\("meta") \"visibility" \> "Good"
        // verify the exported xml preserves the GSA
        xml must \\("meta") \"gsa" \> "0"
      }
    }
}
