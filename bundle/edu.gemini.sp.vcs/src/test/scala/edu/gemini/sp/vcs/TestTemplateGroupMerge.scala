package edu.gemini.sp.vcs

import edu.gemini.sp.vcs.TestingEnvironment._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.template.{SplitFunctor, TemplateParameters, TemplateGroup, TemplateFolder}
import edu.gemini.spModel.pio.ParamSet
import edu.gemini.spModel.pio.xml.PioXmlUtil

import org.junit.Assert._
import org.junit.Test

import scala.collection.JavaConverters._
import edu.gemini.pot.sp.{ISPTemplateParameters, ISPTemplateGroup}
import edu.gemini.spModel.util.VersionToken

class TestTemplateGroupMerge {
  private def setupProg(pc: ProgContext): Unit = {
    val tfn = pc.odb.getFactory.createTemplateFolder(pc.sp, null)
    val tgn = pc.odb.getFactory.createTemplateGroup(pc.sp, null)
    val tpn = (0 to 4).map { _ => pc.odb.getFactory.createTemplateParameters(pc.sp, null) }.toList

    val tfd = tfn.getDataObject.asInstanceOf[TemplateFolder]
    tfd.setParamSet(read(templateFolder))
    tfn.setDataObject(tfd)

    val tgd = tgn.getDataObject.asInstanceOf[TemplateGroup]
    tgd.setParamSet(read(templateGroup))
    tgn.setDataObject(tgd)

    tpn.zip(templateParameters).map { case (n, s) =>
      val tpd = n.getDataObject.asInstanceOf[TemplateParameters]
      tpd.setParamSet(read(s))
      n.setDataObject(tpd)
    }

    tpn.foreach { tgn.addTemplateParameters }
    tfn.addTemplateGroup(tgn)
    pc.sp.setTemplateFolder(tfn)
  }

  implicit def pimpTemplateGroup(tg: ISPTemplateGroup) = new Object {
    def dataObject   = tg.getDataObject.asInstanceOf[TemplateGroup]

    def versionToken = dataObject.getVersionToken

    def versionToken_=(vt: VersionToken): Unit = {
      val dob = dataObject
      if (dob.getVersionToken != vt) {
        dob.setVersionToken(vt)
        tg.setDataObject(dob)
      }
    }
  }

  implicit def pimpTemplateParameters(tp: ISPTemplateParameters) = new Object {
    def dataObject  = tp.getDataObject.asInstanceOf[TemplateParameters]

    def target: SPTarget = dataObject.getTarget
    def targetName: String = target.getName
  }

  @Test def testConflictingTemplateGroups() {
    piSyncTest { env =>
      import env._

      setupProg(central)
      update(user)

      // split remote
      val tgr = central.sp.getTemplateFolder.getTemplateGroups.get(0)
      val sfr = new SplitFunctor(tgr)
      sfr.add(tgr.getTemplateParameters.get(0))
      sfr.add(tgr.getTemplateParameters.get(1))
      central.odb.getQueryRunner(javaUser).execute(sfr, null)

      // split local
      val tgl = cloned.sp.getTemplateFolder.getTemplateGroups.get(0)
      val sfl = new SplitFunctor(tgl)
      sfl.add(tgl.getTemplateParameters.get(2))
      sfl.add(tgl.getTemplateParameters.get(3))
      cloned.odb.getQueryRunner(javaUser).execute(sfl, null)

      update(user)
      commit()

      val tgs = central.sp.getTemplateFolder.getTemplateGroups.asScala.toList
      assertEquals(3, tgs.size)
      assertEquals(List("1", "1.2", "1.1"), tgs.map(_.versionToken.toString))

      def targets(tg: ISPTemplateGroup): Set[String] =
        tg.getTemplateParameters.asScala.map(_.targetName).toSet

      assertEquals(Set("W0535-7500"), targets(tgs(0)))
      assertEquals(Set("W0713-2917", "W0359-5401"), targets(tgs(1)))
      assertEquals(Set("W0647-6232", "W0734-7157"), targets(tgs(2)))

      assertEquals("1.3", tgs(0).versionToken.next.toString)
    }
  }

  private def read(s: String): ParamSet = PioXmlUtil.read(s).asInstanceOf[ParamSet]

  val templateFolder = """
        <paramset name="Template Folder" kind="dataObj">
          <param name="title" value="Templates"/>
          <paramset name="gsoaiBlueprint">
            <param name="filters" value="CH4_SHORT"/>
            <param name="templateFolderMapKey" value="blueprint-0"/>
          </paramset>
         </paramset>"""

  val templateGroup = """
          <paramset name="Template Group" kind="dataObj">
            <param name="title" value="GSAOI CH4(short) (1.580 um)"/>
            <param name="blueprint" value="blueprint-0"/>
            <param name="status" value="PHASE2"/>
            <param name="versionToken" value="1"/>
          </paramset>"""

  val templateParameters4 = """
            <paramset name="Template Parameters" kind="dataObj">
              <param name="title" value="Template Parameters"/>
              <paramset name="spTarget">
                <param name="name" value="W0535-7500"/>
                <param name="system" value="J2000"/>
                <param name="epoch" value="2000.0" units="years"/>
                <param name="brightness" value=""/>
                <param name="c1" value="05:35:21.080"/>
                <param name="c2" value="-74:59:33.20"/>
                <param name="pm1" value="0.0" units="milli-arcsecs/year"/>
                <param name="pm2" value="0.0" units="milli-arcsecs/year"/>
                <param name="parallax" value="0.0" units="arcsecs"/>
                <param name="rv" value="0.0" units="km/sec"/>
                <param name="wavelength" value="-1.0" units="angstroms"/>
                <paramset name="magnitudeList">
                  <paramset name="magnitude">
                    <param name="band" value="J"/>
                    <param name="val" value="22.0"/>
                    <param name="system" value="Vega"/>
                  </paramset>
                </paramset>
                <param name="templateFolderMapKey" value="target-4"/>
              </paramset>
              <paramset name="Observing Conditions" kind="dataObj">
                <param name="CloudCover" value="PERCENT_50"/>
                <param name="ImageQuality" value="PERCENT_70"/>
                <param name="SkyBackground" value="ANY"/>
                <param name="WaterVapor" value="ANY"/>
                <param name="ElevationConstraintType" value="AIRMASS"/>
                <param name="ElevationConstraintMin" value="1.0"/>
                <param name="ElevationConstraintMax" value="1.8"/>
                <paramset name="timing-window-list"/>
                <param name="templateFolderMapKey" value="condition-0"/>
              </paramset>
              <param name="time" value="13824000"/>
            </paramset>"""

  val templateParameters3 = """
            <paramset name="Template Parameters" kind="dataObj">
              <param name="title" value="Template Parameters"/>
              <paramset name="spTarget">
                <param name="name" value="W0359-5401"/>
                <param name="system" value="J2000"/>
                <param name="epoch" value="2000.0" units="years"/>
                <param name="brightness" value=""/>
                <param name="c1" value="03:59:38.785"/>
                <param name="c2" value="-54:01:53.10"/>
                <param name="pm1" value="0.0" units="milli-arcsecs/year"/>
                <param name="pm2" value="0.0" units="milli-arcsecs/year"/>
                <param name="parallax" value="0.0" units="arcsecs"/>
                <param name="rv" value="0.0" units="km/sec"/>
                <param name="wavelength" value="-1.0" units="angstroms"/>
                <paramset name="magnitudeList">
                  <paramset name="magnitude">
                    <param name="band" value="J"/>
                    <param name="val" value="20.0"/>
                    <param name="system" value="Vega"/>
                  </paramset>
                </paramset>
                <param name="templateFolderMapKey" value="target-3"/>
              </paramset>
              <paramset name="Observing Conditions" kind="dataObj">
                <param name="CloudCover" value="PERCENT_50"/>
                <param name="ImageQuality" value="PERCENT_70"/>
                <param name="SkyBackground" value="ANY"/>
                <param name="WaterVapor" value="ANY"/>
                <param name="ElevationConstraintType" value="AIRMASS"/>
                <param name="ElevationConstraintMin" value="1.0"/>
                <param name="ElevationConstraintMax" value="1.8"/>
                <paramset name="timing-window-list"/>
                <param name="templateFolderMapKey" value="condition-0"/>
              </paramset>
              <param name="time" value="13824000"/>
            </paramset>"""

  val templateParameters2 = """
            <paramset name="Template Parameters" kind="dataObj">
              <param name="title" value="Template Parameters"/>
              <paramset name="spTarget">
                <param name="name" value="W0713-2917"/>
                <param name="system" value="J2000"/>
                <param name="epoch" value="2000.0" units="years"/>
                <param name="brightness" value=""/>
                <param name="c1" value="07:13:25.190"/>
                <param name="c2" value="-29:17:39.80"/>
                <param name="pm1" value="0.0" units="milli-arcsecs/year"/>
                <param name="pm2" value="0.0" units="milli-arcsecs/year"/>
                <param name="parallax" value="0.0" units="arcsecs"/>
                <param name="rv" value="0.0" units="km/sec"/>
                <param name="wavelength" value="-1.0" units="angstroms"/>
                <paramset name="magnitudeList">
                  <paramset name="magnitude">
                    <param name="band" value="J"/>
                    <param name="val" value="20.0"/>
                    <param name="system" value="Vega"/>
                  </paramset>
                </paramset>
                <param name="templateFolderMapKey" value="target-2"/>
              </paramset>
              <paramset name="Observing Conditions" kind="dataObj">
                <param name="CloudCover" value="PERCENT_50"/>
                <param name="ImageQuality" value="PERCENT_70"/>
                <param name="SkyBackground" value="ANY"/>
                <param name="WaterVapor" value="ANY"/>
                <param name="ElevationConstraintType" value="AIRMASS"/>
                <param name="ElevationConstraintMin" value="1.0"/>
                <param name="ElevationConstraintMax" value="1.8"/>
                <paramset name="timing-window-list"/>
                <param name="templateFolderMapKey" value="condition-0"/>
              </paramset>
              <param name="time" value="6912000"/>
            </paramset>"""

  val templateParameters1 = """
            <paramset name="Template Parameters" kind="dataObj">
              <param name="title" value="Template Parameters"/>
              <paramset name="spTarget">
                <param name="name" value="W0734-7157"/>
                <param name="system" value="J2000"/>
                <param name="epoch" value="2000.0" units="years"/>
                <param name="brightness" value=""/>
                <param name="c1" value="07:34:39.140"/>
                <param name="c2" value="-71:57:50.00"/>
                <param name="pm1" value="0.0" units="milli-arcsecs/year"/>
                <param name="pm2" value="0.0" units="milli-arcsecs/year"/>
                <param name="parallax" value="0.0" units="arcsecs"/>
                <param name="rv" value="0.0" units="km/sec"/>
                <param name="wavelength" value="-1.0" units="angstroms"/>
                <paramset name="magnitudeList">
                  <paramset name="magnitude">
                    <param name="band" value="J"/>
                    <param name="val" value="22.0"/>
                    <param name="system" value="Vega"/>
                  </paramset>
                </paramset>
                <param name="templateFolderMapKey" value="target-1"/>
              </paramset>
              <paramset name="Observing Conditions" kind="dataObj">
                <param name="CloudCover" value="PERCENT_50"/>
                <param name="ImageQuality" value="PERCENT_70"/>
                <param name="SkyBackground" value="ANY"/>
                <param name="WaterVapor" value="ANY"/>
                <param name="ElevationConstraintType" value="AIRMASS"/>
                <param name="ElevationConstraintMin" value="1.0"/>
                <param name="ElevationConstraintMax" value="1.8"/>
                <paramset name="timing-window-list"/>
                <param name="templateFolderMapKey" value="condition-0"/>
              </paramset>
              <param name="time" value="6912000"/>
            </paramset>"""

  val templateParameters0 = """
            <paramset name="Template Parameters" kind="dataObj">
              <param name="title" value="Template Parameters"/>
              <paramset name="spTarget">
                <param name="name" value="W0647-6232"/>
                <param name="system" value="J2000"/>
                <param name="epoch" value="2000.0" units="years"/>
                <param name="brightness" value=""/>
                <param name="c1" value="06:47:18.560"/>
                <param name="c2" value="-62:32:45.90"/>
                <param name="pm1" value="0.0" units="milli-arcsecs/year"/>
                <param name="pm2" value="0.0" units="milli-arcsecs/year"/>
                <param name="parallax" value="0.0" units="arcsecs"/>
                <param name="rv" value="0.0" units="km/sec"/>
                <param name="wavelength" value="-1.0" units="angstroms"/>
                <paramset name="magnitudeList">
                  <paramset name="magnitude">
                    <param name="band" value="J"/>
                    <param name="val" value="22.399999618530273"/>
                    <param name="system" value="Vega"/>
                  </paramset>
                </paramset>
                <param name="templateFolderMapKey" value="target-0"/>
              </paramset>
              <paramset name="Observing Conditions" kind="dataObj">
                <param name="CloudCover" value="PERCENT_50"/>
                <param name="ImageQuality" value="PERCENT_70"/>
                <param name="SkyBackground" value="ANY"/>
                <param name="WaterVapor" value="ANY"/>
                <param name="ElevationConstraintType" value="AIRMASS"/>
                <param name="ElevationConstraintMin" value="1.0"/>
                <param name="ElevationConstraintMax" value="1.8"/>
                <paramset name="timing-window-list"/>
                <param name="templateFolderMapKey" value="condition-0"/>
              </paramset>
              <param name="time" value="6912000"/>
            </paramset>"""

  val templateParameters = List(templateParameters0, templateParameters1, templateParameters2, templateParameters3, templateParameters4)
}
