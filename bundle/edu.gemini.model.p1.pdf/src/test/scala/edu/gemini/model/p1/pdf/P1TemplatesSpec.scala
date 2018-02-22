package edu.gemini.model.p1.pdf

import org.specs2.matcher.XmlMatchers
import org.specs2.mutable.Specification
import javax.xml.transform.stream.{StreamResult, StreamSource}
import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import edu.gemini.model.p1.pdf.P1PDF.{InvestigatorsListOption, Template, P1PdfUriResolver}
import scala.xml.XML

class P1TemplatesSpec extends Specification with XmlMatchers {
  // These test transform the proposal files using the templates
  // and validate the output of the transformation
  //
  // Though this doesn't test the PDF directly we should trust FOP to produce de desired result
  // at the end
  "The P1 DEFAULT Template" should {
    "write on the ObservingMode Queue if there is no ToO option set" in {
      val result = transformProposal("proposal_no_too.xml")

      /*(XML.loadString(result) \\ ("block")) foreach {
        e => println("'" + e.text + "'")
      }*/
      XML.loadString(result) must \\("block") \>~ """\s*Observing Mode: Queue\s*"""
    }
    "write on the ObservingMode Queue + Rapid ToO, REL-646" in {
      val result = transformProposal("proposal_rapid_too.xml")

      XML.loadString(result) must \\("block") \>~ """\s*Observing Mode: Queue \+ Rapid ToO\s*"""
    }
    "write on the ObservingMode Queue + Standard ToO, REL-646" in {
      val result = transformProposal("proposal_standard_too.xml")

      XML.loadString(result) must \\("block") \>~ """\s*Observing Mode: Queue \+ Standard ToO\s*"""
    }
    "include TAC information in case all are approved, REL-677" in {
      val result = transformProposal("proposal_submitted_to_tac_all_approved.xml")

      val accepted = (XML.loadString(result) \\ "table-row" \ "table-cell") collect {
        case e if e.text.matches( """\s*Accepted\s*""") => true
      }

      // Check there is a table
      XML.loadString(result) must \\("block") \ "inline" \>~ """\s*TAC information\s*"""
      // Check there is three accepted
      accepted must be size 3
    }
    "include TAC information with accepts and rejects, REL-677" in {
      val result = transformProposal("proposal_submitted_to_tac_one_approved_one_rejected.xml")

      val accepted = (XML.loadString(result) \\ "table-row" \ "table-cell") collect {
        case e if e.text.matches( """\s*Accepted\s*""") => true
      }
      val rejected = (XML.loadString(result) \\ "table-row" \ "table-cell") collect {
        case e if e.text.matches( """\s*Rejected\s*""") => true
      }

      // Check there is a table
      XML.loadString(result) must \\("block") \ "inline" \>~ """\s*TAC information\s*"""
      // Check there is one accepted
      accepted must be size 1
      // And one rejected
      rejected must be size 1
    }
    "include TAC information with only one response, REL-677" in {
      val result = transformProposal("proposal_submitted_to_tac_one_decision.xml")

      val accepted = (XML.loadString(result) \\ "table-row" \ "table-cell") collect {
        case e if e.text.matches( """\s*Accepted\s*""") => true
      }
      val rejected = (XML.loadString(result) \\ "table-row" \ "table-cell") collect {
        case e if e.text.matches( """\s*Rejected\s*""") => true
      }

      // Check there is a table
      XML.loadString(result) must \\("block") \ "inline" \>~ """\s*TAC information\s*"""
      // Check there is one accepted
      accepted must be size 1
      // And none rejected
      rejected must beEmpty
    }
    "skip TAC information when there are no responses, REL-677" in {
      val result = transformProposal("proposal_submitted_to_tac_no_decisions.xml")

      // Check there is no TAC table
      XML.loadString(result) must not(\\("block") \ "inline" \>~ """\s*TAC information\s*""")
    }
    "include TAC partner ranking, REL-677" in {
      val result = transformProposal("proposal_submitted_to_tac_all_approved.xml")

      // Check there is a block with text 4.0
      XML.loadString(result) must \\("block")  \> "4.0"
    }
    "include recommended and min recommended time, REL-677" in {
      val result = transformProposal("proposal_submitted_to_tac_all_approved.xml")

      // Check there is a block with text 2.0 hr (1.0 hr)
      XML.loadString(result) must \\("block")  \>~ """2\.0.hr.\(1\.0.hr\)"""
    }
    "include text with scheduling requests, REL-687" in {
      val result = transformProposal("proposal_with_schedule.xml")
      val schedRegex = """\s*This proposal has the following scheduling restrictions.*""".r
      val foundMatches = (XML.loadString(result) \\ "block") collect {
        case e if schedRegex.findFirstIn(e.text).isDefined => true
      }
      // Check there is a scheduling element
      XML.loadString(result) must (\\("block") \ "inline" \>~ """\s*Scheduling Constraints\s*""")
      // Check there is a scheduling text
      foundMatches must be size 1
    }
    "use new text for observations with guiding between 50% and less than 100%, REL-640" in {
      val result = transformProposal("proposal_guiding_caution.xml")
      // Check there is a scheduling element
      XML.loadString(result) must (\\("block") \ "inline" \>~ """.*Some PAs do not have suitable guide stars \(\d\d%\).*""")
    }
    "use new text for observations with guiding between 0% and less than 50%, REL-640" in {
      val result = transformProposal("proposal_guiding_warning.xml")
      // Check there is a scheduling element
      XML.loadString(result) must (\\("block") \ "inline" \>~ """.*Many PAs do not have suitable guide stars \(\d\d%\).*""")
    }
    "use new text for observations with guiding equals to 0%, REL-640" in {
      val result = transformProposal("proposal_guiding_bad.xml")
      // Check there is a scheduling element
      XML.loadString(result) must (\\("block") \ "inline" \>~ """.*Guiding is problematic \(0%\).*""")
    }
    "present the correct name when using GSAOI, REL-693" in {
      val result = transformProposal("proposal_with_gsaoi.xml")
      // Check that we use the proper public name of GSOAI
      XML.loadString(result) must (\\("table-cell") \ "block" \> "GSAOI")
    }
    "present the correct name when using Texes, REL-1062" in {
      val result = transformProposal("proposal_with_texes.xml")
      // Check that we use the proper public name of Texes
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Texes - Gemini North")
    }
    "present the correct name when using Dssi, REL-1061" in {
      val result = transformProposal("proposal_with_dssi.xml")
      // Check that we use the proper public name of DSSI
      XML.loadString(result) must (\\("table-cell") \ "block" \> "DSSI - Gemini North")
    }
    "present the correct name when using 'Alopeke, REL-3351" in {
      val result = transformProposal("proposal_with_alopeke.xml")
      XML.loadString(result) must (\\("table-cell") \ "block" \> "'Alopeke")
    }
    "present the correct name when using Visitor GN, REL-1090" in {
      val result = transformProposal("proposal_with_visitor_gn.xml")
      // Check that we use the proper public name of a north visitor
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Visitor - Gemini North - My instrument")
    }
    "present the correct name when using Visitor GS, REL-1090" in {
      val result = transformProposal("proposal_with_visitor_gs.xml")
      // Check that we use the proper public name of a south visitor
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Visitor - Gemini South - Super Camera")
    }
    "present Phoenix's Site, REL-2463" in {
      val result = transformProposal("proposal_with_phoenix.xml")
      // Check that we use the proper public name of a south visitor
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Phoenix - Gemini South")
    }
    "present Texes' Site, REL-2463" in {
      val result = transformProposal("proposal_with_texes.xml")
      // Check that we use the proper public name of a south visitor
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Texes - Gemini North")
    }
    "show an ITAC information section if the proposal contains a comment, REL-1165" in {
      val result = transformProposal("proposal_with_itac_comment.xml")
      // Check that we have an ITAC information section with the comment
      XML.loadString(result) must (\\("block") \ "inline" \> "ITAC Information")
      XML.loadString(result) must \\("block") \>~ "An Itac comment"
    }
    "show an ITAC information section if the proposal contains multiple comments, REL-1165" in {
      val result = transformProposal("proposal_with_itac_and_several_comments.xml")
      // Check that we have an ITAC information section with the comment
      XML.loadString(result) must (\\("block") \ "inline" \> "ITAC Information")
      XML.loadString(result) must \\("block") \>~ "One Itac comment"
      XML.loadString(result) must \\("block") \>~ "Another itac comment"
    }
    "if there is no ITAC section in the proposal, no ITAC Information section should be included, REL-1165" in {
      val result = transformProposal("proposal_with_gsaoi.xml")
      // Check that there is no ITAC information section
      XML.loadString(result) must not (\\("block") \ "inline" \> "ITAC Information")
    }
    "show an ITAC information section if the proposal contains a multiline comment, REL-1165" in {
      val result = transformProposal("proposal_with_itac_and_ntac_comments.xml")
      // Check that we have an ITAC information section with the comment
      XML.loadString(result) must \\("block") \ "inline" \> "ITAC Information"
    }
    "present the correct name when using GPI, REL-1193" in {
      val result = transformProposal("proposal_with_gpi.xml")
      // Check that we use the proper public name of GPI
      XML.loadString(result) must (\\("table-cell") \ "block" \> "GPI")
    }
    "present the correct name when using GRACES, REL-1356" in {
      val result = transformProposal("proposal_with_graces.xml")
      // Check that we use the proper public name of GPI
      XML.loadString(result) must (\\("table-cell") \ "block" \> "GRACES")
    }
    "present the correct instrument name when using Phoenix, REL-2356" in {
      val result = transformProposal("proposal_with_phoenix.xml")
      // Check that we use the proper public name of Phoenix
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Phoenix - Gemini South")
    }
    "Supports Large Programs, REL-1614" in {
      val result = transformProposal("large_program.xml")
      val proposalXml = XML.loadString(result)
      // Check that the Observing Mode is Large Program
      val largeProgramMode = (proposalXml \\ "table-cell" \ "block") collect {
        case e if e.text.matches( """\s*Observing Mode:.Large Program\s*""") => true
      }
      largeProgramMode must be size 1
      // LPTAC table
      proposalXml must (\\("inline") \>~ """\s*LPTAC information\s*""")
    }
    "Shows Visitors on each site on the summary" in {
      val result = transformProposal("proposal_with_visitor_gn_and_gs.xml")
      val proposalXml = XML.loadString(result)
      proposalXml must (\\("table-cell") \ "block" \>~ """GPI, GRACES, GMOS North, GNIRS, GMOS South, Flamingos2, NIRI, GSAOI, NIFS, Visitor - Gemini South - GS Cam, Visitor - Gemini North - DSSI""")
    }
    "Shows Texes on each site on the summary" in {
      val result = transformProposal("proposal_with_texes_gn_and_gs.xml")
      val proposalXml = XML.loadString(result)
      proposalXml must (\\("table-cell") \ "block" \>~ """GPI, GRACES, GMOS North, GNIRS, GMOS South, Flamingos2, Texes - Gemini North, NIRI, GSAOI, Texes - Gemini South, NIFS""")
    }
    "show correct Observing Mode for FT, REL-1894" in {
      val result = transformProposal("proposal_fast_turnaround.xml")
      val proposalXml = XML.loadString(result)
      // Check that Observing Mode is correct
      val ftMode = (proposalXml \\ "table-cell" \ "block") collect {
        case e if e.text.matches( """\s*Observing Mode:.Fast Turnaround\s*""") => true
      }
      ftMode must be size 1
    }
    "display reviewer for FT, REL-1894" in {
      val result = transformProposal("proposal_fast_turnaround.xml", P1PDF.NOAO.copy(investigatorsList = InvestigatorsListOption.DefaultList))
      val proposalXml = XML.loadString(result)
      // Check the reviewer is displayed
      proposalXml must (\\("inline") \>~ """\s*Reviewer:\s*""")
      proposalXml must (\\("block") \>~ """.*Andrew.Stephens\s*""")
    }
    "display mentor for FT, REL-1894" in {
      val result = transformProposal("proposal_fast_turnaround.xml", P1PDF.NOAO.copy(investigatorsList = InvestigatorsListOption.DefaultList))
      val proposalXml = XML.loadString(result)
      // Check that Observing Mode is correct
      proposalXml must (\\("inline") \>~ """\s*Mentor:\s*""")
      proposalXml must (\\("block") \>~ """.*John.Doe\s*""")
    }
    "calculate the total time for all observations for GN and GS, REL-1298" in {
      val result = transformProposal("proposal_with_gn_and_gs.xml")
      val proposalXml = XML.loadString(result)
      // Check values manually calculated
      // Band 1/2 GN
      proposalXml must (\\("block") \>~ """11.1 hr\s*""")
      // Band 1/2 GS
      proposalXml must (\\("block") \>~ """8.4 hr\s*""")
      // Band 3 GN
      proposalXml must (\\("block") \>~ """3.0 hr\s*""")
      // Band 3 GS
      proposalXml must (\\("block") \>~ """1.0 hr\s*""")
    }
    "includes the principal investigator, REL-3151" in {
      val result = transformProposal("proposal_no_too.xml")
      val proposalXml = XML.loadString(result)

      // Show the principal
      proposalXml must (\\("block") \ "inline" \>~ "Principal Investigator:")
    }
    "includes the principal at the end, REL-3151" in {
      val result = transformProposal("proposal_no_too.xml", P1PDF.GeminiDefaultListAtTheEnd)
      val proposalXml = XML.loadString(result)

      // Show the principal
      proposalXml must (\\("block") \>~ """P..Investigator""")
    }

  }

  "The P1 NOAO Template" should {
    "include text with scheduling requests, REL-687" in {
      val result = transformProposal("proposal_with_schedule.xml", P1PDF.NOAONoInvestigatorsList)
      val schedRegex = """\s*This proposal has the following scheduling restrictions.*""".r
      val foundMatches = (XML.loadString(result) \\ "block") collect {
        case e if schedRegex.findFirstIn(e.text).isDefined => true
      }
      // Check there is a scheduling element
      XML.loadString(result) must (\\("block") \ "inline" \>~ """\s*Scheduling Constraints:\s*""")
      // Check there is a scheduling text
      foundMatches must be size 1
    }
    "include TAC partner ranking, REL-677" in {
      val result = transformProposal("proposal_submitted_to_tac_all_approved.xml", P1PDF.NOAONoInvestigatorsList)

      // Check there is a block with text 4.0
      XML.loadString(result) must \\("block") \> "4.0"
    }
    "include recommended and min recommended time, REL-677" in {
      val result = transformProposal("proposal_submitted_to_tac_all_approved.xml", P1PDF.NOAONoInvestigatorsList)

      // Check there is a block with text 2.0 hr (1.0 hr)
      XML.loadString(result) must \\("block")  \>~ """2\.0.hr.\(1\.0.hr\)"""
    }
    "show that GSAOI is in Gemini South, REL-693" in {
      val result = transformProposal("proposal_with_gsaoi.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that GSAOI is shown in Gemini South
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Gemini South")
    }
    "show that Texes is in Gemini North, REL-1062" in {
      val result = transformProposal("proposal_with_texes.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that Texes is shown in Gemini North
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Gemini North")
    }
    "show that Dssi is in Gemini North, REL-1061" in {
      val result = transformProposal("proposal_with_dssi.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that Speckle is shown in Gemini North
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Gemini North")
    }
    "show that a GN visitor is in Gemini North, REL-1090" in {
      val result = transformProposal("proposal_with_visitor_gn.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that Speckle is shown in Gemini North
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Gemini North")
    }
    "show that a GS visitor is in Gemini North, REL-1090" in {
      val result = transformProposal("proposal_with_visitor_gs.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that Speckle is shown in Gemini North
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Gemini South")
    }
    "show that a Phoenix site is displayed, REL-1090" in {
      val result = transformProposal("proposal_with_phoenix.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that Phoenix is shown in Gemini South
      XML.loadString(result) must (\\("table-cell") \ "block" \> "Gemini South")
    }
    "show an ITAC information section if the proposal contains a comment, REL-1165" in {
      val result = transformProposal("proposal_with_itac_comment.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that we have an ITAC information section with the comment
      XML.loadString(result) must \\("block") \ "inline" \>~ "ITAC Information.*"
      XML.loadString(result) must \\("block") \>~ "An Itac comment"
    }
    "show an ITAC information section if the proposal contains multiple comment, REL-1165" in {
      val result = transformProposal("proposal_with_itac_and_several_comments.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that we have an ITAC information section with the comment
      XML.loadString(result) must \\("block") \ "inline" \>~ "ITAC Information.*"
      XML.loadString(result) must \\("block") \>~ "One Itac comment"
      XML.loadString(result) must \\("block") \>~ "Another itac comment"
    }
    "show an ITAC information section if the proposal contains itac and ntac comments, REL-1165" in {
      val result = transformProposal("proposal_with_itac_and_ntac_comments.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that we have an ITAC information section with the comment
      XML.loadString(result) must (\\("block") \ "inline" \>~ "ITAC Information:.*")
    }
    "if there is no ITAC section in the proposal, no ITAC Information section should be included, REL-1165" in {
      val result = transformProposal("proposal_with_gsaoi.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that there is no ITAC information section
      XML.loadString(result) must not (\\("block") \ "inline" \> "ITAC Information: ")
    }
    "show that GPI is in GS, REL-1193" in {
      val result = transformProposal("proposal_with_gpi.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that GPI is shown in Gemini South
      XML.loadString(result) must (\\("table-cell") \ "block" \>~ "Gemini South")
    }
    "show that GRACES is in GN, REL-1356" in {
      val result = transformProposal("proposal_with_graces.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that Graces is shown in Gemini South
      XML.loadString(result) must (\\("table-cell") \ "block" \>~ "Gemini North")
    }
    "show that Phoenix is in GS, REL-2356" in {
      val result = transformProposal("proposal_with_phoenix.xml", P1PDF.NOAONoInvestigatorsList)
      // Check that Phoenix is shown in Gemini South
      XML.loadString(result) must (\\("table-cell") \ "block" \>~ "Gemini South")
    }
    "show correct Observing Mode for FT, REL-1894" in {
      val result = transformProposal("proposal_fast_turnaround.xml", P1PDF.NOAONoInvestigatorsList)
      val proposalXml = XML.loadString(result)
      // Check that Observing Mode is correct
      val ftMode = (proposalXml \\ "table-cell" \ "block") collect {
        case e if e.text.contains("""Fast Turnaround""") => true
      }
      ftMode must be size 1
    }
    "calculate the total time for all observations for GN and GS, REL-1298" in {
      val result = transformProposal("proposal_with_gn_and_gs.xml", P1PDF.NOAONoInvestigatorsList)
      val proposalXml = XML.loadString(result)
      // Check values manually calculated
      // Band 1/2 GN
      proposalXml must (\\("block") \>~ """11.1 hr\s*""")
      // Band 1/2 GS
      proposalXml must (\\("block") \>~ """8.4 hr\s*""")
      // Band 3 GN
      proposalXml must (\\("block") \>~ """3.0 hr\s*""")
      // Band 3 GS
      proposalXml must (\\("block") \>~ """1.0 hr\s*""")
    }
  }

  "The P1 AU Template" should {
    "includes the program id with a sensible default, REL-813" in {
      val result = transformProposal("proposal_au_no_submission.xml", P1PDF.AU.copy(investigatorsList = InvestigatorsListOption.DefaultList))
      val proposalXml = XML.loadString(result)

      // Show the semester
      proposalXml must (\\("block") \>~ "AU-2013B-.*")
    }
  }

  "The P1 Without investigators template" should {
    "includes the program id with a sensible default, REL-3151" in {
      val result = transformProposal("proposal_no_too.xml", P1PDF.GeminiDefaultNoInvestigatorsList)
      val proposalXml = XML.loadString(result)

      // Should not display the principal
      proposalXml must not (\\("block") \ "inline" \>~ "Principal Investigator:")
    }
  }

  def transformProposal(proposal: String, template: Template = P1PDF.GeminiDefault) = {
    val xslStream = getClass.getResourceAsStream(template.location)

    val xslSource = new StreamSource(xslStream)
    val xmlSource = new StreamSource(getClass.getResourceAsStream(proposal))

    // Setup XSLT
    val factory = TransformerFactory.newInstance()
    factory.setURIResolver(P1PdfUriResolver)

    val transformer = factory.newTransformer(xslSource)
    transformer.setURIResolver(P1PdfUriResolver)
    template.parameters.foreach(p => transformer.setParameter(p._1, p._2))

    val writer = new StringWriter()

    val res = new StreamResult(writer)
    // Do XSLT Transform
    transformer.transform(xmlSource, res)

    writer.toString
  }
}
