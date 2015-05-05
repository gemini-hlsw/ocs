package edu.gemini.phase2.skeleton.factory

import java.security.Principal

import edu.gemini.model.p1.immutable.{Proposal, ProposalIo}
import edu.gemini.phase2.core.model.SkeletonShell
import edu.gemini.phase2.core.odb.SkeletonStoreService
import edu.gemini.phase2.template.factory.api.TemplateFolderExpansionFactory
import edu.gemini.phase2.template.factory.impl.{TemplateDb, TemplateFactoryImpl}
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.spModel.core.SPProgramID
import org.specs2.mutable.Specification

trait TemplateSpec {

  def expand[A](x: xml.Elem)(func: ISPProgram => A): A =
    expand(x.toString)(func)

  def expand[A](s: String)(func: ISPProgram => A): A =
    expand(ProposalIo.read(s))(func)

  def expand[A](p: Proposal)(func: ISPProgram => A): A = {
    val db  = DBLocalDatabase.createTransient()
    try {
      val f   = Phase1FolderFactory.create(p).right.get
      val ss  = new SkeletonShell(TemplateSpec.programId, SpProgramFactory.create(p), f)
      val tf  = TemplateFactoryImpl(TemplateSpec.templateDb)
      val tfe = TemplateFolderExpansionFactory.expand(ss.folder, tf).right.get
      func(SkeletonStoreService.store(ss, tfe, db).program)
    } finally {
      db.getDBAdmin.shutdown()
    }
  }


}

object TemplateSpec {
  // Read-only and expensive to construct, so only do it once.
  val templateDb = TemplateDb.load(java.util.Collections.emptySet[Principal]).right.get
  val programId  = SPProgramID.toProgramID("GS-2015A-Q-1")
}


object Test extends Specification with TemplateSpec {

  expand(nifs) { p =>

    "Expanded program" should {
      "have a template folder" in {
        p.getTemplateFolder != null
      }
    }

  }

  lazy val nifs =
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
          <firstName>Richard</firstName>
          <lastName>McDermid</lastName>
          <status>PhD</status>
          <email>rmcdermid@gemini.edu</email>
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
          <nonAo id="blueprint-0">
            <name>NIFS Z (0.94 - 1.15um)</name>
            <visitor>false</visitor>
            <disperser>Z (0.94 - 1.15um)</disperser>
          </nonAo>
        </nifs>
        <nifs>
          <ao id="blueprint-1">
            <name>NIFS LGS/PWFS1 None J (1.15 - 1.33um)</name>
            <visitor>false</visitor>
            <disperser>J (1.15 - 1.33um)</disperser>
            <altair>
              <lgs pwfs1="true" aowfs="false" oiwfs="false"/>
            </altair>
            <occultingDisk>None</occultingDisk>
          </ao>
        </nifs>
        <nifs>
          <ao id="blueprint-2">
            <name>NIFS LGS None H (1.49 - 1.80um)</name>
            <visitor>false</visitor>
            <disperser>H (1.49 - 1.80um)</disperser>
            <altair>
              <lgs pwfs1="false" aowfs="false" oiwfs="false"/>
            </altair>
            <occultingDisk>None</occultingDisk>
          </ao>
        </nifs>
        <nifs>
          <ao id="blueprint-3">
            <name>NIFS LGS None Z (0.94 - 1.15um)</name>
            <visitor>false</visitor>
            <disperser>Z (0.94 - 1.15um)</disperser>
            <altair>
              <lgs pwfs1="false" aowfs="false" oiwfs="false"/>
            </altair>
            <occultingDisk>None</occultingDisk>
          </ao>
        </nifs>
        <nifs>
          <ao id="blueprint-4">
            <name>NIFS NGS/FL 0.2&quot; H (1.49 - 1.80um)</name>
            <visitor>false</visitor>
            <disperser>H (1.49 - 1.80um)</disperser>
            <altair>
              <ngs fieldLens="true"/>
            </altair>
            <occultingDisk>0.2&quot;</occultingDisk>
          </ao>
        </nifs>
        <nifs>
          <ao id="blueprint-5">
            <name>NIFS NGS None K (1.99 - 2.40um)</name>
            <visitor>false</visitor>
            <disperser>K (1.99 - 2.40um)</disperser>
            <altair>
              <ngs fieldLens="false"/>
            </altair>
            <occultingDisk>None</occultingDisk>
          </ao>
        </nifs>
        <nifs>
          <nonAo id="blueprint-6">
            <name>NIFS K (1.99 - 2.40um)</name>
            <visitor>false</visitor>
            <disperser>K (1.99 - 2.40um)</disperser>
          </nonAo>
        </nifs>
        <nifs>
          <nonAo id="blueprint-7">
            <name>NIFS H (1.49 - 1.80um)</name>
            <visitor>false</visitor>
            <disperser>H (1.49 - 1.80um)</disperser>
          </nonAo>
        </nifs>
        <nifs>
          <nonAo id="blueprint-8">
            <name>NIFS J (1.15 - 1.33um)</name>
            <visitor>false</visitor>
            <disperser>J (1.15 - 1.33um)</disperser>
          </nonAo>
        </nifs>
      </blueprints>
      <observations>
        <observation band="Band 1/2" enabled="true" target="target-6" condition="condition-0" blueprint="blueprint-0">
          <time units="hr">1.0</time>
          <meta ck="">
            <guiding>
              <percentage>100</percentage>
              <evaluation>Success</evaluation>
            </guiding>
            <visibility>Good</visibility>
            <gsa>0</gsa>
          </meta>
        </observation>
        <observation band="Band 1/2" enabled="true" target="target-5" condition="condition-0" blueprint="blueprint-1">
          <time units="hr">1.0</time>
          <meta ck="">
            <guiding>
              <percentage>100</percentage>
              <evaluation>Success</evaluation>
            </guiding>
            <visibility>Good</visibility>
            <gsa>0</gsa>
          </meta>
        </observation>
        <observation band="Band 1/2" enabled="true" target="target-1" condition="condition-0" blueprint="blueprint-2">
          <time units="hr">1.0</time>
          <meta ck="">
            <guiding>
              <percentage>100</percentage>
              <evaluation>Success</evaluation>
            </guiding>
            <visibility>Bad</visibility>
            <gsa>0</gsa>
          </meta>
        </observation>
        <observation band="Band 1/2" enabled="true" target="target-2" condition="condition-0" blueprint="blueprint-3">
          <time units="hr">1.0</time>
          <meta ck="">
            <guiding>
              <percentage>0</percentage>
              <evaluation>Failure</evaluation>
            </guiding>
            <visibility>Good</visibility>
            <gsa>0</gsa>
          </meta>
        </observation>
        <observation band="Band 1/2" enabled="true" target="target-7" condition="condition-0" blueprint="blueprint-4">
          <time units="hr">1.0</time>
          <meta ck="">
            <guiding>
              <percentage>0</percentage>
              <evaluation>Failure</evaluation>
            </guiding>
            <visibility>Limited</visibility>
            <gsa>0</gsa>
          </meta>
        </observation>
        <observation band="Band 1/2" enabled="true" target="target-4" condition="condition-0" blueprint="blueprint-5">
          <time units="hr">1.0</time>
          <meta ck="">
            <guiding>
              <percentage>0</percentage>
              <evaluation>Failure</evaluation>
            </guiding>
            <visibility>Limited</visibility>
            <gsa>0</gsa>
          </meta>
        </observation>
        <observation band="Band 1/2" enabled="true" target="target-3" condition="condition-0" blueprint="blueprint-6">
          <time units="hr">1.0</time>
          <meta ck="">
            <guiding>
              <percentage>100</percentage>
              <evaluation>Success</evaluation>
            </guiding>
            <visibility>Bad</visibility>
            <gsa>0</gsa>
          </meta>
        </observation>
        <observation band="Band 1/2" enabled="true" target="target-0" condition="condition-0" blueprint="blueprint-7">
          <time units="hr">1.0</time>
          <meta ck="">
            <guiding>
              <percentage>100</percentage>
              <evaluation>Success</evaluation>
            </guiding>
            <visibility>Good</visibility>
            <gsa>0</gsa>
          </meta>
        </observation>
        <observation band="Band 1/2" enabled="true" target="target-8" condition="condition-0" blueprint="blueprint-8">
          <time units="hr">1.0</time>
          <meta ck="">
            <guiding>
              <percentage>100</percentage>
              <evaluation>Success</evaluation>
            </guiding>
            <visibility>Limited</visibility>
            <gsa>0</gsa>
          </meta>
        </observation>
      </observations>
      <proposalClass>
        <queue tooOption="None"/>
      </proposalClass>
    </proposal>


}


