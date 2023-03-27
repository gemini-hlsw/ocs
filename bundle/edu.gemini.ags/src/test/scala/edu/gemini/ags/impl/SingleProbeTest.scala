package edu.gemini.ags.impl

import java.awt.geom.Area
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.acqcam.InstAcqCam
import edu.gemini.spModel.gemini.altair.{AltairAowfsGuider, AltairParams}
import edu.gemini.spModel.gemini.flamingos2.{Flamingos2, Flamingos2OiwfsGuideProbe}
import edu.gemini.spModel.gemini.gmos.{GmosOiwfsGuideProbe, InstGmosNorth, InstGmosSouth}
import edu.gemini.spModel.gemini.gnirs.{GnirsOiwfsGuideProbe, InstGNIRS}
import edu.gemini.spModel.gemini.igrins2.Igrins2Mixin
import edu.gemini.spModel.gemini.michelle.InstMichelle
import edu.gemini.spModel.gemini.nici.InstNICI
import edu.gemini.spModel.gemini.nifs.{InstNIFS, NifsOiwfsGuideProbe}
import edu.gemini.spModel.gemini.niri.{InstNIRI, NiriOiwfsGuideProbe}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions._
import edu.gemini.spModel.gemini.texes.InstTexes
import edu.gemini.spModel.gemini.trecs.InstTReCS
import edu.gemini.spModel.gemini.visitor.VisitorInstrument
import edu.gemini.spModel.guide.{GuideProbe, PatrolField}
import edu.gemini.spModel.guide.GuideSpeed._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import org.junit.{Ignore, Test}

/* OUTSTANDING ISSUES:
 * 1. NIRI is still having problems, and we don't test multiple cameras.
 *    From old PwfsAgsStrategyTest:
 *    // F14 has a smaller inner radius limit than F6, at this position it will therefore choose another (brighter) guide star
 *    niri withCamera Niri.Camera.F6     at ("23:59:44.252", "-00:06:49.48") selects "180-000015" estimates 1.0
 *    niri withCamera Niri.Camera.F14    at ("23:59:44.252", "-00:06:49.48") selects "180-305074" estimates 1.0
 *    // do the same check for F32 and F32PV vs F14 at a slightly moved position
 *    niri withCamera Niri.Camera.F14    at ("23:59:58.091", "-00:02:58.10") selects "180-305054" estimates 1.0
 *    niri withCamera Niri.Camera.F32    at ("23:59:58.091", "-00:02:58.10") selects "180-305049" estimates 1.0
 *    niri withCamera Niri.Camera.F32_PV at ("23:59:58.091", "-00:02:58.10") selects "180-305049" estimates 1.0
 *
 * 2. No tests for chop mode for Michelle and TReCS, as per old PwfsAgsStrategy. All this did in the past was check the
 *    magnitude limits for chop modes, so this doesn't seem very useful now.
 *
 * 3. THESE ARE VERY SLOW. There are many selections done, with a total of:
 *    = 40 instrument / guide probe combinations
 *    =  3 sets of standard conditions
 *    = 12 different angles for rotations
 *    =  4 tests that only test conditions for each inst / gp combo
 *    =  3 tests that test conditions + angles for each inst / gp combo
 *    Grand total: 40 * 3 * (4 + 3 * 12) = 4800 tests.
 */

class SingleProbeTest {
  def pwfsArea(ctx: ObsContext, probe: GuideProbe): Area = {
    val pwfsGuideProbe = probe.asInstanceOf[PwfsGuideProbe]
    val min = pwfsGuideProbe.getVignettingClearance(ctx)
    pwfsGuideProbe.getCorrectedPatrolField(PatrolField.fromRadiusLimits(min, PwfsGuideProbe.PWFS_RADIUS), ctx).getArea
  }

  // Now that we have corrected the PatrolField to exclude blocked areas as it should, this is no longer needed.
//  def removeBlockedArea(ctx: ObsContext, probe: GuideProbe): Area = {
//    val cpf = probe.getCorrectedPatrolField(ctx)
//    val outer = cpf.getArea
//    val blocked = cpf.getBlockedArea
//    outer.subtract(blocked)
//    outer
//  }

  val AcqCamNorthPwfs1: AgsTest = AgsTest(InstAcqCam.SP_TYPE,        PwfsGuideProbe.pwfs1, Site.GN).withValidArea(pwfsArea)
  val AcqCamNorthPwfs2: AgsTest = AgsTest(InstAcqCam.SP_TYPE,        PwfsGuideProbe.pwfs2, Site.GN).withValidArea(pwfsArea)
  val AcqCamSouthPwfs1: AgsTest = AgsTest(InstAcqCam.SP_TYPE,        PwfsGuideProbe.pwfs1, Site.GS).withValidArea(pwfsArea)
  val AcqCamSouthPwfs2: AgsTest = AgsTest(InstAcqCam.SP_TYPE,        PwfsGuideProbe.pwfs2, Site.GS).withValidArea(pwfsArea)

  val F2: AgsTest = AgsTest(Flamingos2.SP_TYPE,        Flamingos2OiwfsGuideProbe.instance)
  val F2Pwfs1: AgsTest = AgsTest(Flamingos2.SP_TYPE,        PwfsGuideProbe.pwfs1).withValidArea(pwfsArea)
  val F2Pwfs2: AgsTest = AgsTest(Flamingos2.SP_TYPE,        PwfsGuideProbe.pwfs2).withValidArea(pwfsArea)

  val GmosNorth: AgsTest = AgsTest(InstGmosNorth.SP_TYPE,     GmosOiwfsGuideProbe.instance)
  val GmosNorthPwfs1: AgsTest = AgsTest(InstGmosNorth.SP_TYPE,     PwfsGuideProbe.pwfs1).withValidArea(pwfsArea)
  val GmosNorthPwfs2: AgsTest = AgsTest(InstGmosNorth.SP_TYPE,     PwfsGuideProbe.pwfs2).withValidArea(pwfsArea)
  val GmosSouth: AgsTest = AgsTest(InstGmosSouth.SP_TYPE,     GmosOiwfsGuideProbe.instance)
  val GmosSouthPwfs1: AgsTest = AgsTest(InstGmosSouth.SP_TYPE,     PwfsGuideProbe.pwfs1).withValidArea(pwfsArea)
  val GmosSouthPwfs2: AgsTest = AgsTest(InstGmosSouth.SP_TYPE,     PwfsGuideProbe.pwfs2).withValidArea(pwfsArea)

  val Gnirs: AgsTest = AgsTest(InstGNIRS.SP_TYPE,         GnirsOiwfsGuideProbe.instance).withStrategyOverride(Strategy.GnirsOiwfs)
  val GnirsAltairLGS: AgsTest = AgsTest(InstGNIRS.SP_TYPE,         AltairAowfsGuider.instance).withAltair(AltairParams.Mode.LGS)
  val GnirsAltairNGS: AgsTest = AgsTest(InstGNIRS.SP_TYPE,         AltairAowfsGuider.instance).withAltair(AltairParams.Mode.NGS)
  val GnirsPwfs1: AgsTest = AgsTest(InstGNIRS.SP_TYPE,         PwfsGuideProbe.pwfs1).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs1North)
  val GnirsPwfs2: AgsTest = AgsTest(InstGNIRS.SP_TYPE,         PwfsGuideProbe.pwfs2).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs1South)

  val Igrins2Pwfs2: AgsTest = AgsTest(Igrins2Mixin.SP_TYPE,       PwfsGuideProbe.pwfs2).withValidArea(pwfsArea).withValidArea(pwfsArea)

  val MichellePwfs1: AgsTest = AgsTest(InstMichelle.SP_TYPE,      PwfsGuideProbe.pwfs1).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs1North)
  val MichellePwfs2: AgsTest = AgsTest(InstMichelle.SP_TYPE,      PwfsGuideProbe.pwfs2).withValidArea(pwfsArea)

  val NiciPwfs1: AgsTest = AgsTest(InstNICI.SP_TYPE,          PwfsGuideProbe.pwfs1).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs1South)
  val NiciPwfs2: AgsTest = AgsTest(InstNICI.SP_TYPE,          PwfsGuideProbe.pwfs2).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs2South)

  val Nifs: AgsTest = AgsTest(InstNIFS.SP_TYPE,          NifsOiwfsGuideProbe.instance).withStrategyOverride(Strategy.NifsOiwfs)
  val NifsAltairLGS: AgsTest = AgsTest(InstNIFS.SP_TYPE,          AltairAowfsGuider.instance).withAltair(AltairParams.Mode.LGS)
  val NifsAltairNGS: AgsTest = AgsTest(InstNIFS.SP_TYPE,          AltairAowfsGuider.instance).withAltair(AltairParams.Mode.NGS)
  val NifsPwfs1: AgsTest = AgsTest(InstNIFS.SP_TYPE,          PwfsGuideProbe.pwfs1).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs1North)
  val NifsPwfs2: AgsTest = AgsTest(InstNIFS.SP_TYPE,          PwfsGuideProbe.pwfs2).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs2North)

  val Niri: AgsTest = AgsTest(InstNIRI.SP_TYPE,          NiriOiwfsGuideProbe.instance)
  val NiriAltairLGS: AgsTest = AgsTest(InstNIRI.SP_TYPE,          AltairAowfsGuider.instance).withAltair(AltairParams.Mode.LGS)
  val NiriAltairNGS: AgsTest = AgsTest(InstNIRI.SP_TYPE,          AltairAowfsGuider.instance).withAltair(AltairParams.Mode.NGS)
  val NiriPwfs1: AgsTest = AgsTest(InstNIRI.SP_TYPE,          PwfsGuideProbe.pwfs1).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs1North)
  val NiriPwfs2: AgsTest = AgsTest(InstNIRI.SP_TYPE,          PwfsGuideProbe.pwfs2).withValidArea(pwfsArea)

  val TexesPwfs1: AgsTest = AgsTest(InstTexes.SP_TYPE,         PwfsGuideProbe.pwfs1).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs1North)
  val TexesPwfs2: AgsTest = AgsTest(InstTexes.SP_TYPE,         PwfsGuideProbe.pwfs2).withValidArea(pwfsArea)

  val TReCSPwfs1: AgsTest = AgsTest(InstTReCS.SP_TYPE,         PwfsGuideProbe.pwfs1).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs1South)
  val TReCSPwfs2: AgsTest = AgsTest(InstTReCS.SP_TYPE,         PwfsGuideProbe.pwfs2).withValidArea(pwfsArea)

  val VisitNorthPwfs1: AgsTest = AgsTest(VisitorInstrument.SP_TYPE, PwfsGuideProbe.pwfs1, Site.GN).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs1North)
  val VisitNorthPwfs2: AgsTest = AgsTest(VisitorInstrument.SP_TYPE, PwfsGuideProbe.pwfs2, Site.GN).withValidArea(pwfsArea)
  val VisitSouthPwfs1: AgsTest = AgsTest(VisitorInstrument.SP_TYPE, PwfsGuideProbe.pwfs1, Site.GS).withValidArea(pwfsArea).withStrategyOverride(Strategy.Pwfs1South)
  val VisitSouthPwfs2: AgsTest = AgsTest(VisitorInstrument.SP_TYPE, PwfsGuideProbe.pwfs2, Site.GS).withValidArea(pwfsArea)

  val All = List(
    AcqCamNorthPwfs1,
    AcqCamNorthPwfs2,
    AcqCamSouthPwfs1,
    AcqCamSouthPwfs2,

    F2,
    F2Pwfs1,
    F2Pwfs2,

    // GMOS OI now uses vignetting in AGS, so tests expecting to pick the brightest guide star are no longer relevant.
    //GmosNorth,
    GmosNorthPwfs1,
    GmosNorthPwfs2,
    //GmosSouth,
    GmosSouthPwfs1,
    GmosSouthPwfs2,

    Gnirs,
    GnirsAltairLGS,
    GnirsAltairNGS,
    GnirsPwfs1,
    GnirsPwfs2,

    Igrins2Pwfs2,

    MichellePwfs1,
    MichellePwfs2,

    NiciPwfs1,
    NiciPwfs2,

    Nifs,
    NifsAltairLGS,
    NifsAltairNGS,
    NifsPwfs1,
    NifsPwfs2,

  // See CandidateValidator and SingleProbeStrategyParams
  // The min distance in CandidateValidator.isValid fails: diff is 0 arcsec, min is 20 arcsec.
//    Niri,
    NiriAltairLGS,
    NiriAltairNGS,
    NiriPwfs1,
    NiriPwfs2,

    TexesPwfs1,
    TexesPwfs2,

    TReCSPwfs1,
    TReCSPwfs2,

    VisitNorthPwfs1,
    VisitNorthPwfs2,
    VisitSouthPwfs1,
    VisitSouthPwfs2
  )

  // GMOS worst conditions range (bright, fast) - (faint, slow)
  //  8.5 ,  14.5 (nominal conditions)
  // -1.0 ,  -1.0 (IQ Any)
  // -3.0 ,  -3.0 (CC Any)
  // -0.5 ,  -0.5 (SB Any)
  // ---- ,  ----
  //  4.0 ,  10.0  (FAST)
  //  4.75,  10.75 (MEDIUM)
  //  5.5,   11.5  (SLOW)

  @Ignore @Test
  def manualSetupTest(): Unit =
    GmosSouth.withConditions(WORST).usable(
      ("23:59:53.993 00:01:48.80", 11.50, SLOW),
      ("23:59:55.127 00:03:07.00", 10.76, SLOW),
      ("23:59:55.127 00:03:07.00", 10.75, MEDIUM),
      ("23:59:55.127 00:03:07.00", 10.01, MEDIUM),
      ("23:59:55.127 00:03:07.00", 10.00, FAST),
      ("23:59:55.127 00:03:07.00",  4.00, FAST)
    ).unusable(
      ("00:00:01.473 00:03:44.40", 10.0), // out of range
      ("23:59:46.287 00:03:41.00", 10.0), // out of range
      ("23:59:53.993 00:01:48.80", 11.6), // too dim
      ("23:59:53.993 00:01:48.80",  3.9)  // too bright
    ).test()

  private def testAll(f: AgsTest => Unit): Unit =
    All.foreach(f)


  @Test def testBase(): Unit = testAll(_.testBase())
  @Test def testBaseOneOffset(): Unit = testAll(_.testBaseOneOffset())
  @Test def testBaseTwoDisjointOffsets(): Unit = testAll(_.testBaseTwoDisjointOffsets())
  @Test def testBaseTwoIntersectingOffsets(): Unit = testAll(_.testBaseTwoIntersectingOffsets())
  @Test def testBaseRotated(): Unit = testAll(_.testBaseRotated())
  @Test def testBaseRotatedOneOffset(): Unit = testAll(_.testBaseRotatedOneOffset())
  @Test def testBaseRotatedTwoIntersectingOffsets(): Unit = testAll(_.testBaseRotatedTwoIntersectingOffsets())
  @Test def testBaseUnboundedPosAngleConstraint(): Unit = testAll(_.testBaseUnboundedPosAngleConstraint())
}
