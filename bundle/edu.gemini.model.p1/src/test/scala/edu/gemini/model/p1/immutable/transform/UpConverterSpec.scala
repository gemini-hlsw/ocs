package edu.gemini.model.p1.immutable.transform

import org.specs2.matcher.{MatchResult, XmlMatchers}
import org.specs2.mutable._
import org.specs2.scalaz.ValidationMatchers._

import xml._
import java.io.InputStreamReader
import java.io.File

import edu.gemini.model.p1.immutable._
import scalaz.NonEmptyList
import XMLConverter._
import scalaz.{ Validation, Success }

class UpConverterSpec extends Specification with SemesterProperties with XmlMatchers {
  "The UpConverter" should {
    "change the version but not the semester from a 2014.1.1 proposal to the current Release" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_2014.1.1.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have size 6
          changes must contain(s"Updated schema version to ${Proposal.currentSchemaVersion}")

          val proposal = ProposalIo.read(result.toString())
          proposal.meta.band3OptionChosen must beFalse
          proposal.meta.overrideAffiliate must beTrue
          proposal.title must beEqualTo("Observation with GSAOI")
          proposal.abstrakt must beEmpty
          proposal.scheduling must beEmpty
          proposal.investigators.all must have size 1
          proposal.targets must beEmpty
          proposal.conditions must beEmpty
          proposal.blueprints must have size 1
          proposal.observations must have size 2
          Option(proposal.proposalClass) must beSome

          // Check the instruments
          proposal.blueprints.headOption.foreach {
            i =>
              i.name must beEqualTo("GSAOI K(short) (2.150 um)")
              i.visitor must beFalse
          }

          proposal.semester must beEqualTo(Semester(2023, SemesterOption.B))
      }

      UpConverter.upConvert(xml) must beSuccessful.like {
        case ConversionResult(transformed, from, changes, root) =>
          transformed must beTrue
          from must beEqualTo(Semester(2014, SemesterOption.A))
          changes must have length 6
      }

    }
    "change the semester of a previous semester proposal" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.14.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have size 7
          changes must contain(s"Updated schema version to ${Proposal.currentSchemaVersion}")
          changes must contain(s"Updated semester to ${Semester.current.display}")
          changes must contain("Please use the PIT from semester 2013A to view the unmodified proposal")

          val proposal = ProposalIo.read(result.toString())
          proposal.meta.band3OptionChosen must beFalse
          proposal.meta.overrideAffiliate must beTrue
          proposal.title must beEqualTo("Observation with GSAOI")
          proposal.abstrakt must beEmpty
          proposal.scheduling must beEmpty
          proposal.investigators.all must have size 1
          proposal.targets must beEmpty
          proposal.conditions must beEmpty
          proposal.blueprints must have size 1
          proposal.observations must have size 2
          Option(proposal.proposalClass) must beSome

          // Check the instruments
          proposal.blueprints.headOption.foreach {
            i =>
              i.name must beEqualTo("GSAOI K(short) (2.150 um)")
              i.visitor must beFalse
          }

          proposal.semester must beEqualTo(Semester.current)
      }

      UpConverter.upConvert(xml) must beSuccessful.like {
        case ConversionResult(transformed, from, changes, _) =>
          transformed must beTrue
          from must beEqualTo(Semester(2013, SemesterOption.A))
          changes must have length 7
      }

    }
    "change the proposal version of a previous semester proposal" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.14.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have size 7
          changes must contain(s"Updated schema version to ${Proposal.currentSchemaVersion}")
          changes must contain(s"Updated semester to ${Semester.current.display}")
          changes must contain("Please use the PIT from semester 2013A to view the unmodified proposal")

          val proposal = ProposalIo.read(result.toString())

          // Sanity checks
          proposal.meta.band3OptionChosen must beFalse
          proposal.meta.overrideAffiliate must beTrue
          proposal.title must beEqualTo("Observation with GSAOI")
          proposal.abstrakt must beEmpty
          proposal.scheduling must beEmpty
          proposal.investigators.all must have size 1
          proposal.targets must beEmpty
          proposal.conditions must beEmpty
          proposal.blueprints must have size 1
          proposal.observations must have size 2
          Option(proposal.proposalClass) must beSome

          proposal.schemaVersion must beEqualTo(Proposal.currentSchemaVersion)
      }
    }
    "create a band3Option missing attribute from 1.0.0" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0_no_band3option.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 9
          changes must contain(s"Updated schema version to ${Proposal.currentSchemaVersion}")
          changes must contain(s"Updated semester to ${Semester.current.display}")
          changes must contain("Please use the PIT from semester 2012B to view the unmodified proposal")
          changes must contain("Band3 Option is missing, set as false")
          changes must contain("Affiliate override flag is missing")
          changes must contain("Updated existing attachment value")

          (result \\ "meta") must ==/(<meta band3optionChosen="false">
            <firstAttachment>file.pdf</firstAttachment>
          </meta>)

          val proposal = ProposalIo.read(result.toString())
          proposal.meta.band3OptionChosen must beFalse
          proposal.meta.firstAttachment must beEqualTo(Some(new File("file.pdf")))
      }

      UpConverter.upConvert(xml) must beSuccessful.like {
        case ConversionResult(transformed, from, changes, root) =>
          transformed must beTrue
          from must beEqualTo(Semester(2012, SemesterOption.B))
          changes must have length 9
      }
    }
    "add enabled attribute if missing" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0_with_no_observations_enabled_attribute.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          (result \\ "observations") must ==/(<observations>
            <observation band="Band 1/2"/>
          </observations>)

          val proposal = ProposalIo.read(result.toString())
          proposal.observations.head.enabled must beTrue
      }
    }
    "ignore missing missing itac/ngoauthority attribute" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0_itac_no_ngoauthority.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          (result \\ "ngoauthority") must beEmpty

          val proposal = ProposalIo.read(result.toString())
          proposal.proposalClass.itac.map {
            i: Itac => i.ngoAuthority must beNone
          }
          true must beTrue
      }
    }
    "respect itac/ngoauthority attribute" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0_itac_with_ngoauthority.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          (result \\ "ngoauthority") must ==/(<ngoauthority>cl</ngoauthority>)

          val proposal = ProposalIo.read(result.toString())
          proposal.proposalClass.itac.map {
            i: Itac => i.ngoAuthority must beSome(NgoPartner.CL)
          }
          true must beTrue
      }
    }
    "respect enabled attribute" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0_with_disabled_observations.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          (result \\ "observations") must ==/(<observations>
            <observation band="Band 1/2" enabled="false"/>
          </observations>)

          val proposal = ProposalIo.read(result.toString())
          proposal.observations.head.enabled must beFalse
      }
    }
    "remove submissions for ngo uk" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0_with_uk.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 12
          changes must contain(s"Updated schema version to ${Proposal.currentSchemaVersion}")
          changes must contain(s"Updated semester to ${Semester.current.display}")
          changes must contain("Please use the PIT from semester 2012B to view the unmodified proposal")
          changes must contain("Band3 Option is missing, set as false")
          changes must contain("Affiliate override flag is missing")
          changes must contain("NGO acceptance from the United Kingdom has been removed")
          changes must contain("Former observation time parameter mapped to program time")

          // Sanity check
          ProposalIo.read(result.toString())

          // ngo is gone
          result \\ "queue" must not \\ "ngo"
          // but itac is there
          result \\ "queue" must \\("itac")
          // Check that queue attributes are preserved
          result must \\("queue", "key" -> "604c87d8-9bf8-96a8-0642-f70604c87d89", "tooOption" -> "None")
      }
    }
    "remove submissions for ngo uk preserving ar" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0_with_uk_and_arg.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 12
          changes must contain(s"Updated schema version to ${Proposal.currentSchemaVersion}")
          changes must contain(s"Updated semester to ${Semester.current.display}")
          changes must contain("Please use the PIT from semester 2012B to view the unmodified proposal")
          changes must contain("Band3 Option is missing, set as false")
          changes must contain("Affiliate override flag is missing")
          changes must contain("NGO acceptance from the United Kingdom has been removed")
          changes must contain("Former observation time parameter mapped to program time")

          // Sanity check
          ProposalIo.read(result.toString())

          // ngo is gone
          result \\ "partner" must have length 1
          result \\ "queue" must \\("partner") \> "ar"
          // but itac is there
          result \\ "queue" must \\("itac")
          // Check that queue attributes are preserved
          result must \\("queue", "key" -> "604c87d8-9bf8-96a8-0642-f70604c87d89", "tooOption" -> "None")
      }
    }
    "remove itac ngo authority if it is the uk" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_1.0.0_with_uk_and_arg_and_uk_as_ngoauthority.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 13
          changes must contain(s"Updated schema version to ${Proposal.currentSchemaVersion}")
          changes must contain(s"Updated semester to ${Semester.current.display}")
          changes must contain("Please use the PIT from semester 2012B to view the unmodified proposal")
          changes must contain("Band3 Option is missing, set as false")
          changes must contain("Affiliate override flag is missing")
          changes must contain("NGO acceptance from the United Kingdom has been removed")
          changes must contain("The United Kingdom was marked as ITAC NGO authority and has been removed")
          changes must contain("Former observation time parameter mapped to program time")

          // Sanity check
          ProposalIo.read(result.toString())

          // ngo is gone
          result \\ "partner" must have length 1
          result \\ "queue" must \\("partner") \> "ar"
          // but itac is there
          result \\ "queue" must \\("itac")
          result \\ "itac" must not \\ "ngoauthority"
          // Check that queue attributes are preserved
          result must \\("queue", "key" -> "604c87d8-9bf8-96a8-0642-f70604c87d89", "tooOption" -> "None")
      }
    }
    "reject proposals with an unknown version" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_unknown.xml")))
      val converted = UpConverter.convert(xml)
      converted must beFailing.like {
        case e: NonEmptyList[String] =>
          e.head must beEqualTo("I don't know how to handle a proposal with version 0.0.0")
      }
    }
    "check new flags on the special proosal" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_special.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          ProposalIo.read(result.toString())
          result \\ "proposalClass" \\ "special" must \\("special", "key" -> "59df5571-e836-4652-ac4b-68c05d1c13a2", "tooOption" -> "None", "jwstSynergy" -> "false")
      }
    }
    "reject proposals with a missing version" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_ver_missing.xml")))
      val converted = UpConverter.convert(xml)
      converted must beFailing.like {
        case e: NonEmptyList[String] =>
          e.head must beEqualTo("I don't know how to handle a proposal without version")
      }
    }
    "reject xmls that are no proposals" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("no_proposal.xml")))
      val converted = UpConverter.convert(xml)
      converted must beFailing.like {
        case e: NonEmptyList[String] =>
          e.head must beEqualTo("Unknown xml file format")
      }
    }
    "test case for removing ngo" in {
      val node = <proposal>
        <queue>
          <itac></itac> <ngo partnerLead="investigator-1">
          <partner>uk</partner>
        </ngo>
        </queue>
      </proposal>
      val transformed = XMLConverter.transform(node, SemesterConverter2012BTo2013A.ukSubmission)
      transformed must beSuccessful.like {
        case StepResult(changes, n) =>
          changes must have length 1
          changes must contain("NGO acceptance from the United Kingdom has been removed")

          n must ==/(<proposal>
            <queue>
              <itac></itac>
            </queue>
          </proposal>)
      }
    }
    "test case for removing ngo, only the uk" in {
      val node = <proposal>
        <queue>
          <itac></itac> <ngo partnerLead="investigator-0">
          <partner>ar</partner>
        </ngo> <ngo partnerLead="investigator-1">
          <partner>uk</partner>
        </ngo>
        </queue>
      </proposal>
      val transformed = XMLConverter.transform(node, SemesterConverter2012BTo2013A.ukSubmission)
      transformed must beSuccessful.like {
        case StepResult(changes, n) =>
          changes must have length 1
          changes must contain("NGO acceptance from the United Kingdom has been removed")

          n must ==/(<proposal>
            <queue>
              <itac></itac> <ngo partnerLead="investigator-0">
              <partner>ar</partner>
            </ngo>
            </queue>
          </proposal>)
      }
    }
    "open proposals with utf-8 encoding" in {
      val proposal = UpConverter.convert(XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_utf8.xml"))))

      proposal must beSuccessful
    }
    "open proposals with us-ascii encoding" in {
      val proposal = UpConverter.convert(XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_usascii.xml"))))

      proposal must beSuccessful
    }
    "open proposals with latin1 encoding" in {
      val proposal = UpConverter.convert(XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_latin1.xml"))))

      proposal must beSuccessful
    }
    "proposal with Subaru COMICS and FMOS must remove them, REL-3769" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("subaru_multiple.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, _) =>
          changes must contain(SemesterConverter2020ATo2020B.subaruComicsMessage)
          changes must contain(SemesterConverter2020ATo2020B.subaruFmosMessage)
      }
    }
    "proposal with trecs blueprints must remove them, REL-1112" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_trecs.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 8
          changes must contain("The original proposal contained T-ReCS observations. The instrument is not available and those resources have been removed.")
          // The texes blueprint must remain
          result must \\("gmosN")
      }
    }
    "proposal with michelle blueprints must remove them, REL-1112" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_michelle.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 8
          changes must contain("The original proposal contained Michelle observations. The instrument is not available and those resources have been removed.")
          // The texes blueprint must remain
          result must \\("gmosN")
      }
    }
    "proposal with nici blueprints must be removed, REL-1350" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_nici.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 8
          changes must contain("The original proposal contained NICI observations. The instrument is not available and those resources have been removed.")
          // The texes blueprint must remain
          result must \\("gmosN")
      }
    }
    "proposal with texes blueprints must be preserved, REL-2308" in {
      skipped {
        val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_texes.xml")))

        val converted = UpConverter.convert(xml)
        converted must beSuccessful.like {
          case StepResult(changes, result) =>
            changes must have length 8
            // The texes blueprint must remain
            result must \\("Texes")
        }
      }
    }
    "proposal with dssi blueprints at Gemini North must migrate to 'Alopeke, REL-3349" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_dssi_gn.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          changes must contain(SemesterConverter2018ATo2018B.dssiGNToAlopekeMessage)
          result must \\("Alopeke", "id")
          result must \\("Alopeke") \\ "mode" \> AlopekeMode.SPECKLE.value
          result must \\("Alopeke") \\ "name" \> AlopekeBlueprint(AlopekeMode.SPECKLE).name
      }
    }
    "proposal with dssi blueprints at Gemini South must migrate to Zorro, REL-3454" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_dssi_gs.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          changes must contain(SemesterConverter2019ATo2019B.dssiGSToZorroMessage)
          result must \\("Zorro", "id")
          result must \\("Zorro") \\ "mode" \> ZorroMode.SPECKLE.value
          result must \\("Zorro") \\ "name" \> ZorroBlueprint(ZorroMode.SPECKLE).name
      }
    }
    "proposal with phoenix blueprints must have them removed, REL-3233" in {
      skipped {
        val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_phoenix_no_site.xml")))

        val converted = UpConverter.convert(xml)
        converted must beSuccessful.like {
          case StepResult(changes, result) =>
            changes must have length 6
            changes must contain("The original proposal contained Phoenix observations. The instrument is not available and those resources have been removed.")
        }
      }
    }
    "proposals with phoenix blueprints must be assigned to GS, REL-3349" in {
      skipped {
        val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_phoenix_no_site.xml")))

        val converted = UpConverter.convert(xml)
        converted must beSuccessful.like {
          case StepResult(changes, result) =>
            changes must have length 6
            changes must contain(SemesterConverter2018ATo2018B.phoenixSiteMessage)
        }
      }
    }
    "proposal with texes blueprints must have a site, REL-2463" in {
      skipped {
        val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_texes_no_site.xml")))

        val converted = UpConverter.convert(xml)
        converted must beSuccessful.like {
          case StepResult(changes, result) =>
            changes must have length 6
            changes must contain("Texes proposal has been assigned to Gemini North.")
            // The texes blueprint must remain and include a site
            result must \\("Texes", "id")
            result must \\("Texes") \\ "site" \> "Gemini North"
            result must \\("Texes") \\ "name" \> "Texes Gemini North LM_32_echelle"
        }
      }
    }
    "proposal with GmosN blueprints that use a 0.25 slit must be converted to a 0.5 slit, REL-1256" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_gmosn_0.25_slit.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 9
          changes must contain("The unavailable GMOS-N 0.25\" slit has been converted to the 0.5\" slit.")
          // Check that fpu and name are replaced
          result must \\("fpu") \> "0.5 arcsec slit"
          result must \\("name") \>~ ".*0.5 arcsec slit.*"
          // The texes blueprint must remain
          result must \\("flamingos2")
      }
    }
    "proposal with GmosN and GmosS blueprints in spectroscopy MOS mode that don't have a FPU, REL-1256" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_2013.2.1_with_gmos_sn_mos_spectroscopy.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 9
          changes must contain("A default 1\" slit has been added to the GMOS-S MOS observation.")
          changes must contain("A default 1\" slit has been added to the GMOS-N MOS observation.")
          changes must contain(s"Please use the PIT from semester 2013B to view the unmodified proposal")
          // Check that fpu and name are replaced
          (result \\ "gmosS") must \\("fpu") \> "1.0 arcsec slit"
          (result \\ "gmosS") must \\("name") \>~ ".*1.0 arcsec slit.*"
          // Check that fpu and name are replaced
          (result \\ "gmosN") must \\("fpu") \> "1.0 arcsec slit"
          (result \\ "gmosN") must \\("name") \>~ ".*1.0 arcsec slit.*"
      }
    }
    "proposal with Gnirs that doesn't have a central wavelength, REL-1254" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_gnirs_no_centralwavelength.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 8
          changes must contain("GNIRS observation doesn't have a central wavelength range, assigning to '< 2.5um'")
          // Check that the centralWavelength node is added
          result must \\("centralWavelength") \> "< 2.5um"
          result \\ "gnirs" must \\("name") \>~ ".* < 2.5um"
          // The texes blueprint must remain
          result must \\("flamingos2")
      }
    }
    "proposal with F2 narrow band filters must use the filter Y, REL-1282" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_f2_narrow_bandfilter.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 8
          changes must contain("The unavailable Flamingos2 filters F1056 (1.056 um)/F1063 (1.063 um) have been converted to Y (1.020 um).")
          // Check that the centralWavelength node is added
          result \\ "flamingos2" must \\("filter") \> "Y (1.020 um)"
          result \\ "flamingos2" must \\("name") \>~ ".* Y .1.020 um."
          // The texes blueprint must remain
          result must \\("gmosN")
      }
    }
    "proposal with F2 R3K+Y longslit must use the filter Y, REL-1282" in {
      skipped {
        val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_f2_R3K+Y_longslit.xml")))

        testF2R3KYConversion(xml)
      }
    }
    "proposal with F2 R3K+Y MOS must use the filter Y, REL-1282" in {
      skipped {
        val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_f2_R3K+Y_mos.xml")))

        testF2R3KYConversion(xml)
      }
    }
    "proposal with F2 K-long filter must use the new name, REL-2565" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_f2_old_longslit.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          changes must contain("The Flamingos2 filter K-long (2.00 um) has been converted to K-long (2.20 um).")
          // Check that the filter node is added and the name updated
          result \\ "flamingos2" must \\("filter") \> "K-long (2.20 um)"
          result \\ "flamingos2" must \\("name") \>~ ".* K-long .2.20 um.*"
          // The gmosN blueprint must remain
          result must \\("gmosN")
      }
    }
    "proposal with GmosN blueprint and no altair should produce an Altair None option, REL-1257" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_gmosn_ver_2014.2.1.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          changes must contain("GMOS-N observations without an Altair setting have had Adaptive Optics set to None.")
          // Check that fpu and name are replaced
          result must \\("fpu") \> "0.5 arcsec slit"
          result must \\("altair")
          result must \\("altair") \ "none"
          // The texes blueprint must remain
          result must \\("flamingos2")
      }
    }
    "proposal with GmosN Imaging blueprint and no altair should transform the name to Altair None option, REL-1257" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("gmos_imaging_no_altair.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          // Check that fpu and name are replaced
          result must \\("name") \>~ ".*None.*"
      }
    }
    "proposal with GmosN Longslit blueprint and no altair should transform the name to Altair None option, REL-1257" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("gmos_longslit_no_altair.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          // Check that fpu and name are replaced
          result must \\("name") \> "GMOS-N LongSlit None B1200 GG455 (> 460 nm) 0.5 arcsec slit"
      }
    }
    "proposal with GmosN Longslit N&S blueprint and no altair should transform the name to Altair None option, REL-1257" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("gmos_longslit_ns_no_altair.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          // Check that fpu and name are replaced
          result must \\("name") \> "GMOS-N LongSlit N+S None B1200 GG455 (> 460 nm) 0.5 arcsec slit"
      }
    }
    "proposal with GmosN MOS blueprint and no altair should transform the name to Altair None option, REL-1257" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("gmos_mos_no_altair.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          // Check that fpu and name are replaced
          result must \\("name") \> "GMOS-N MOS None 0.75 arcsec slit R400 RG610 (> 615 nm) +Pre"
      }
    }
    "proposal with GmosN MOS N&S blueprint and no altair should transform the name to Altair None option, REL-1257" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("gmos_mos_ns_no_altair.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          // Check that fpu and name are replaced
          result must \\("name") \> "GMOS-N MOS N+S None 1.0 arcsec slit B600 g + GG455 (506 nm) "
      }
    }
    "proposal with GmosN MOS IFU blueprint and no altair should transform the name to Altair None option, REL-1257" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("gmos_ifu_no_altair.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          // Check that fpu and name are replaced
          result must \\("name") \> "GMOS-N IFU None B600 i + CaT (815 nm) IFU 1 slit"
      }
    }
    "proposal with GPI and HStar and HLiwa modes should become H direct, REL-2671" in {
      skipped {
        // GPI is not offered in 2020B.
        val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_gpi_hstar.xml")))

        val converted = UpConverter.convert(xml)
        converted must beSuccessful.like {
          case StepResult(changes, result) =>
            changes must have length 6
            result \\ "gpi" must \\("observingMode") \>~ "H.direct"
            result \\ "gpi" must \\("name") \> "GPI H direct Prism"
        }
      }
    }
    "proposal with observation time attribute should be transformed to progTime attribute, REL-2985" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_observation_time.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          result \\ "observation" must \\("progTime")
          result \\ "observation" must not \\ "time"
      }
    }
    "proposal with request time attribute must be maintained as time attribute, REL-2985" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_with_observation_time.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          result \\ "request" must \\("time")
          result \\ "request" must not \\ "progTime"
      }
    }
    "F2 proposal with Y or J-lo fiters should remove them, REL-3193" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("f2_removedfilters.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 7
          changes must contain("Flamingos2 Multi-Object Spectroscopy is not offered")
          result \\ "flamingos2" must not(\\("filter") \> "J-lo (1.122 um)")
          result \\ "flamingos2" must not(\\("filter") \> "Y (1.020 um)")
          result \\ "flamingos2" must not(\\("name") \>~ ".*Y (1.020 um).*")
          result \\ "flamingos2" must not(\\("name") \>~ ".*J-lo (1.122 um).*")
      }
    }
    "Subaru proposal with Surprime Cam, REL-3638" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("subaru_suprime.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 6
          changes must contain(SemesterConverter2019BTo2020A.subaruSuprimeMessage)
          result \\ "subaru" must \\("name") \> "Subaru (Hyper Suprime Cam)"
          result \\ "subaru" must \\("instrument") \> "Hyper Suprime Cam"
      }
    }
    "update the tacCategory Solar System attribute" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_solarsystem.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must contain(SemesterConverter2019BTo2020A.solarSystemCategoryMessage)

          val proposal = ProposalIo.read(result.toString())
          proposal.category must beSome(TacCategory.SOLAR_SYSTEM_OTHER)
      }
    }
    "update the tacCategory Galactic attribute" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_galactic.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must contain(SemesterConverter2019BTo2020A.galacticCategoryMessage)

          val proposal = ProposalIo.read(result.toString())
          proposal.category must beSome(TacCategory.GALACTIC_OTHER)
      }
    }
    "update the tacCategory Extragalactic attribute" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_extragalactic.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must contain(SemesterConverter2019BTo2020A.extragalacticCategoryMessage)

          val proposal = ProposalIo.read(result.toString())
          proposal.category must beSome(TacCategory.EXTRAGALACTIC_OTHER)
      }
    }
    "remove AU submissions" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("proposal_au_submission.xml")))
      val converted = UpConverter.convert(xml)
      converted match {
        case Success(StepResult(changes, result)) =>
          changes must contain(SemesterConverter2020BTo2021A.AuSubmissionMessage)
          ProposalIo.read(result.toString()).proposalClass match {
            case QueueProposalClass(_, _, _, Left(List(NgoSubmission(_, _, NgoPartner.US, _))), _, _, _, _, _) => success("ok!") // AU is gone!
            case x => failure(x.toString)
          }
        case x => failure(x.toString)
      }
    }
    "update noao institution" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("inst_noao.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must contain(SemesterConverter2021ATo2021B.noaoTransformMessage(SemesterConverter2021ATo2021B.InvestigatorPrelude))
          ProposalIo.read(result.toString()).investigators.pi.address.institution must contain("NSF's NOIRLab")
      }
    }
    "update gn institution" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("inst_gn.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must contain(SemesterConverter2021ATo2021B.gnTransformMessage(SemesterConverter2021ATo2021B.InvestigatorPrelude))
          ProposalIo.read(result.toString()).investigators.pi.address.institution must contain("Gemini Observatory/NSF’s NOIRLab (North)")
      }
    }
    "update gs institution" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("inst_gs.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must contain(SemesterConverter2021ATo2021B.gsTransformMessage(SemesterConverter2021ATo2021B.InvestigatorPrelude))
          ProposalIo.read(result.toString()).investigators.pi.address.institution must contain("Gemini Observatory/NSF’s NOIRLab (South)")
      }
    }
    "update tololo institution" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("inst_tololo.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must contain(SemesterConverter2021ATo2021B.tololoTransformMessage(SemesterConverter2021ATo2021B.InvestigatorPrelude))
          ProposalIo.read(result.toString()).investigators.pi.address.institution must contain("Cerro Tololo Inter-American Observatory/NSF’s NOIRLab")
      }
    }
    "REL-3790 coi conversion" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("REL-3790_21A.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
          case StepResult(changes, result) =>
            changes must contain(SemesterConverter2021ATo2021B.tololoTransformMessage(SemesterConverter2021ATo2021B.COInvestigatorPrelude))
            val res = ProposalIo.read(result.toString())
            res.investigators.pi.address.institution must contain("Gemini Observatory/NSF’s NOIRLab (South)")
            res.investigators.cois.headOption.map(_.institution) must beSome("Gemini Observatory/NSF’s NOIRLab (North)")
            res.investigators.cois(1).institution must contain("NSF's NOIRLab")
            res.investigators.cois(2).institution must contain("Cerro Tololo Inter-American Observatory/NSF’s NOIRLab")
      }
    }
    "REL-4267 graces conversion" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("graces_proposal.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
          case StepResult(changes, result) =>
            changes must contain(SemesterConverter2023ATo2023B.gracesToMaroonXMessage)
            result must \\("MaroonX", "id")
            result must \\("MaroonX") \\ "name" \> MaroonXBlueprint().name
            result must \\("MaroonX") \\ "visitor" \> "true"
      }
    }
    "REL-4267 niri conversion" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("graces_proposal.xml")))
      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
          case StepResult(changes, result) =>
            changes must contain(SemesterConverter2023ATo2023B.gracesToMaroonXMessage)
            result must \\("MaroonX", "id")
            result must \\("MaroonX") \\ "name" \> MaroonXBlueprint().name
            result must \\("MaroonX") \\ "visitor" \> "true"
      }
    }
    "REL-4267 convert niri to gnirs" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("niri_proposal.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 4
          result \\ "gnirs" must \\("filter") \> "H (1.65um)"
          result \\ "gnirs" must \\("name") \> "GNIRS Altair Natural Guidestar 0.05\"/pix H (1.65um)"
          result \\ "gnirs" must \\("pixelScale") \> "0.05\"/pix"
      }
    }
    "REL-4345 Rename sr + sky ghost modes" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("ghost_proposal.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 0
      }
    }
    "REL-4345 Rename sr + sky ghost modes take 2" in {
      val xml = XML.load(new InputStreamReader(getClass.getResourceAsStream("ghost_proposal_2.xml")))

      val converted = UpConverter.convert(xml)
      converted must beSuccessful.like {
        case StepResult(changes, result) =>
          changes must have length 4
          changes must contain("The SRIFU[1|2] + SRIFU[1|2] Sky modes are now just SRIFU + Sky.")
          result \\ "ghost" must \\("name") \> "GHOST Standard SRIFU + Sky"
          result \\ "ghost" must \\("targetMode") \> "SRIFU + Sky"
      }
    }
  }

  def testF2R3KYConversion(xml: Elem): MatchResult[Result] = {
    val converted = UpConverter.convert(xml)
    converted must beSuccessful.like {
      case StepResult(changes, result) =>
        changes must have length 7
        changes must contain("The unavailable Flamingos2 configuration R3K + Y has been converted to R3K + J-lo.")
        // Check that the centralWavelength node is added
        result \\ "flamingos2" must \\("filter") \> "J-lo (1.122 um)"
        result \\ "flamingos2" must \\("name") \>~ ".* J-lo .1.122 um..*"
        // The texes blueprint must remain
        result must \\("gmosN")
    }
  }
}
