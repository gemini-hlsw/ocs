package edu.gemini.phase2.skeleton.factory

import edu.gemini.phase2.template.factory.impl.nifs.{BAT, BT, FT, MT, TargetBrightness}
import edu.gemini.pot.sp.{ISPProgram, ISPTemplateGroup}
import edu.gemini.spModel.core.MagnitudeBand
import org.specs2.mutable.SpecificationLike
import edu.gemini.model.p1.immutable.{AltairNGS, NifsBlueprint, NifsBlueprintAo, NifsBlueprintBase, ProposalIo}
import edu.gemini.model.p1.mutable.{NifsDisperser, NifsOccultingDisk}
import org.specs2.specification.core.Fragment

object REL_2232_Test extends TemplateSpec("NIFS_BP.xml") with SpecificationLike {

  // All possible TargetBrightness buckets
  val buckets: List[Option[TargetBrightness]] =
    None :: List(BAT, FT, MT, BT).map(Some(_))

  // Return the TargetBrightness buckets for the targets in this template group. The returned
  // list should always have size = 1 but we will check that in a test below.
  def groupBuckets(g: ISPTemplateGroup): List[Option[TargetBrightness]] =
    p2targets(g)
      .map(p2mag(_, MagnitudeBand.K))
      .map(_.map(TargetBrightness(_)))

  // Return the TargetBrightness bucket for the first target in the given group. We establish by
  // testing groupBuckets above that this will return the one and only bucket for this group.
  def groupBucket(g: ISPTemplateGroup): Option[TargetBrightness] =
    groupBuckets(g).headOption.flatten

  // A map from TargetBrightness to group. We establish that this is a 1:1 mapping in the first
  // few tests below.
  def bucketMap(sp: ISPProgram): Map[Option[TargetBrightness], ISPTemplateGroup] =
    groups(sp)
      .map { g => groupBucket(g) -> g }
      .toMap

  // Our common tests for NIFS blueprint expansion
  def nifsTest(title: String, bp: NifsBlueprintBase)(more: ISPProgram => Fragment): Unit =
    expand(proposal(bp, (0.0 to 25.0 by 0.5).toList, MagnitudeBand.K)) { (p, sp) =>
      title >> {
        "All targets in a given group should be in the same target brightness bucket." in {
          groups(sp).map(groupBuckets).forall(_.distinct.size must_== 1)
        }
        "There should be a template group for each target brightness bucket." in {
          val found = groups(sp).map(groupBucket).toSet
          buckets.forall(found)
        }
        "All groups should include all notes" in {
          List(
            "Phase II Requirements: General Information",
            "Phase II  \"BEFORE Submission\" Checklist"
          ).forall(n => groups(sp).forall(g => existsNote(g, n)))
        }
        more(sp)
      }
    }

  val nifs = NifsBlueprint(NifsDisperser.H)
  nifsTest("Non-AO NIFS Blueprint Expansion", nifs) { sp =>
    val map = bucketMap(sp)
    checkLibs("BT group",  map(Some(BT)),  Set(3),  Set(4, 5, 23))
    checkLibs("MT group",  map(Some(MT)),  Set(4),  Set(3, 5, 23))
    checkLibs("FT group",  map(Some(FT)),  Set(5),  Set(3, 4, 23))
    checkLibs("BAT group", map(Some(BAT)), Set(23), Set(3, 4, 5))
    checkLibs("Missing K-band group", map(None), Set(3, 4, 5, 23), Set())
  }

  val nifsAO = NifsBlueprintAo(AltairNGS(false), NifsOccultingDisk.CLEAR, NifsDisperser.H)
  nifsTest("NIFS AO without Occulting Disk Blueprint Expansion", nifsAO) { sp =>
    val map = bucketMap(sp)
    checkLibs("BT group",  map(Some(BT)),  Set(3),  Set(4, 5, 23))
    checkLibs("MT group",  map(Some(MT)),  Set(4),  Set(3, 5, 23))
    checkLibs("FT group",  map(Some(FT)),  Set(5),  Set(3, 4, 23))
    checkLibs("BAT group", map(Some(BAT)), Set(23), Set(3, 4, 5))
    checkLibs("Missing K-band group", map(None), Set(3, 4, 5, 23), Set())
  }

  val nifsAO_OD = NifsBlueprintAo(AltairNGS(false), NifsOccultingDisk.OD_2, NifsDisperser.H)
  nifsTest("NIFS AO with Occulting Disk Blueprint Expansion", nifsAO_OD) { sp =>
    val map = bucketMap(sp)
    checkLibs("BT group",  map(Some(BT)),  Set(11), Set(12))
    checkLibs("MT group",  map(Some(MT)),  Set(12), Set(11))
    checkLibs("FT group",  map(Some(FT)),  Set(12), Set(11))
    checkLibs("BAT group", map(Some(BAT)), Set(12), Set(11))
    checkLibs("Missing K-band group", map(None), Set(11, 12), Set())
  }

  // There was a problem with expanding LGS blueprints; verifying fix here
  "NIFS-phase1b.xml" should {
    "Parse and expand without error" in {
      expand(ProposalIo.read(andyXml.toString)) { (p1, p2) => true }
    }
  }

  // NIFS-phase1b.xml from http://swgserv01.cl.gemini.edu:8080/browse/REL-2232
  lazy val andyXml =
    <proposal schemaVersion="2015.2.1" tacCategory="Extragalactic">
      <meta band3optionChosen="true">
        <attachment>/home/astephens/bin/PIT/proposals/GeminiAttachment.pdf</attachment>
      </meta>
      <semester year="2015" half="B"/>
      <title>NIFS Template Tests</title>
      <abstract>We propose to observe a small sample of weak-continuum, dwarf galaxies to investigate the excitation of molecular hydrogen in massive star-forming complexes.  In the usable fraction of our previous allocation we were able to observe one of our targets, NGC5461.  This dataset unambiguously shows that the gas is excited in low density photo-dissociation regions, contrary to the widespread assumption in the literature that the H2 in galaxies is predominantly shock excited.  The weakness of the dwarf galaxy continua permits detection of the higher level H2 transitions which are essential to determine the gas excitation and relative contributions of thermal and UV-excited gas. </abstract>
      <keywords>
        <keyword>Dwarf galaxies</keyword>
        <keyword>Emission lines</keyword>
        <keyword>Starburst galaxies</keyword>
      </keywords>
      <investigators>
        <pi id="investigator-0">
          <firstName>Marie</firstName>
          <lastName>Lemoine-Busserolle</lastName>
          <status>PhD</status>
          <email>mbussreo@gemini.edu</email>
          <phone></phone>
          <address>
            <institution>Gemini Observatory - North</institution>
            <address>Gemini Observatory
              670 N. Aohoku Place
              Hilo
              HI
              96720</address>
            <country>USA</country>
          </address>
        </pi>
      </investigators>
      <targets>
        <sidereal epoch="J2000" id="target-0">
          <name>K=undefined</name>
          <degDeg>
            <ra>0.000004166666864572714</ra>
            <dec>0.00000027777779097151425</dec>
          </degDeg>
          <magnitudes/>
        </sidereal>
        <sidereal epoch="J2000" id="target-1">
          <name>K=16</name>
          <degDeg>
            <ra>240.0</ra>
            <dec>16.0</dec>
          </degDeg>
          <magnitudes>
            <magnitude band="K" system="Vega">16.0</magnitude>
          </magnitudes>
        </sidereal>
        <sidereal epoch="J2000" id="target-2">
          <name>K=21</name>
          <degDeg>
            <ra>315.0</ra>
            <dec>21.0</dec>
          </degDeg>
          <magnitudes>
            <magnitude band="K" system="Vega">21.0</magnitude>
          </magnitudes>
        </sidereal>
        <sidereal epoch="J2000" id="target-3">
          <name>K=14</name>
          <degDeg>
            <ra>210.0</ra>
            <dec>14.0</dec>
          </degDeg>
          <magnitudes>
            <magnitude band="K" system="Vega">14.0</magnitude>
          </magnitudes>
        </sidereal>
        <sidereal epoch="J2000" id="target-4">
          <name>K=12</name>
          <degDeg>
            <ra>180.0</ra>
            <dec>12.0</dec>
          </degDeg>
          <magnitudes>
            <magnitude band="K" system="Vega">12.0</magnitude>
          </magnitudes>
        </sidereal>
        <sidereal epoch="J2000" id="target-5">
          <name>K=9</name>
          <degDeg>
            <ra>135.0</ra>
            <dec>9.0</dec>
          </degDeg>
          <magnitudes>
            <magnitude band="K" system="Vega">9.0</magnitude>
          </magnitudes>
        </sidereal>
        <sidereal epoch="J2000" id="target-6">
          <name>K=8</name>
          <degDeg>
            <ra>120.0</ra>
            <dec>8.0</dec>
          </degDeg>
          <magnitudes>
            <magnitude band="K" system="Vega">8.0</magnitude>
          </magnitudes>
        </sidereal>
        <sidereal epoch="J2000" id="target-7">
          <name>K=18</name>
          <degDeg>
            <ra>270.0</ra>
            <dec>18.0</dec>
          </degDeg>
          <magnitudes>
            <magnitude band="K" system="Vega">18.0</magnitude>
          </magnitudes>
        </sidereal>
        <sidereal epoch="J2000" id="target-8">
          <name>K=13</name>
          <degDeg>
            <ra>195.0</ra>
            <dec>13.0</dec>
          </degDeg>
          <magnitudes>
            <magnitude band="K" system="Vega">13.0</magnitude>
          </magnitudes>
        </sidereal>
      </targets>
      <conditions>
        <condition id="condition-0">
          <name>CC 50%/Clear, IQ 70%/Good, SB Any/Bright, WV Any</name>
          <cc>50%/Clear</cc>
          <iq>70%/Good</iq>
          <sb>Any/Bright</sb>
          <wv>Any</wv>
        </condition>
      </conditions>
      <blueprints>
        <nifs>
          <ao id="blueprint-0">
            <name>NIFS LGS None H (1.49 - 1.80um)</name>
            <visitor>false</visitor>
            <disperser>H (1.49 - 1.80um)</disperser>
            <altair>
              <lgs pwfs1="false" aowfs="false" oiwfs="false"/>
            </altair>
            <occultingDisk>None</occultingDisk>
          </ao>
        </nifs>
      </blueprints>
      <observations>
        <observation band="Band 1/2" enabled="true" target="target-1" condition="condition-0" blueprint="blueprint-0">
          <progTime units="hr">1.0</progTime>
          <meta ck="">
            <guiding>
              <percentage>100</percentage>
              <evaluation>Success</evaluation>
            </guiding>
            <visibility>Bad</visibility>
            <gsa>0</gsa>
          </meta>
        </observation>
      </observations>
      <proposalClass>
        <queue tooOption="None">
          <ngo partnerLead="investigator-0">
            <request>
              <time units="hr">9.0</time>
              <minTime units="hr">4.0</minTime>
            </request>
            <partner>us</partner>
          </ngo>
        </queue>
      </proposalClass>
    </proposal>


}




