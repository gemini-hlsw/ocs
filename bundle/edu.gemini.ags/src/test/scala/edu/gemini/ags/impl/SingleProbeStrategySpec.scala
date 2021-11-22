package edu.gemini.ags.impl

import edu.gemini.ags.api.AgsGuideQuality.{DeliversRequestedIq, PossibleIqDegradation}
import edu.gemini.ags.api.AgsStrategy.{Estimate, Selection}
import edu.gemini.ags.api.AgsGuideQuality
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.catalog.votable.{CannedBackend, TestVoTableBackend}
import edu.gemini.skycalc.{Angle => SkycalcAngle, Offset => SkycalcOffset}
import edu.gemini.spModel.ags.AgsStrategyKey._
import edu.gemini.spModel.core.MagnitudeBand.{_r, R, UC}
import edu.gemini.spModel.core.MagnitudeSystem.Vega
import edu.gemini.spModel.core._
import edu.gemini.shared.util.immutable.{None => JNone, Some => JSome}
import edu.gemini.spModel.gemini.altair.{AltairAowfsGuider, AltairParams, InstAltair}
import edu.gemini.spModel.gemini.flamingos2.{Flamingos2, Flamingos2OiwfsGuideProbe}
import edu.gemini.spModel.gemini.gmos.{GmosNorthType, InstGmosSouth, GmosOiwfsGuideProbe, InstGmosNorth}
import edu.gemini.spModel.gemini.gnirs.{GnirsOiwfsGuideProbe, InstGNIRS}
import edu.gemini.spModel.gemini.niri.{NiriOiwfsGuideProbe, InstNIRI}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.gemini.phoenix.InstPhoenix
import edu.gemini.spModel.gemini.visitor.VisitorInstrument
import edu.gemini.spModel.guide.{ValidatableGuideProbe, GuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe
import edu.gemini.spModel.telescope.IssPort
import edu.gemini.spModel.telescope.PosAngleConstraint.FIXED_180

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

class SingleProbeStrategySpec extends Specification {
  private val magTable = ProbeLimitsTable.loadOrThrow()

  def offset(p: Int, q: Int): SkycalcOffset =
    new SkycalcOffset(SkycalcAngle.arcsecs(p), SkycalcAngle.arcsecs(q))

  def offsets(pqs: (Int, Int)*): Set[SkycalcOffset] =
    pqs.map((offset _).tupled).toSet

  def spTarget(raStr: String, decStr: String): SPTarget = {
    val ra = Angle.parseHMS(raStr).getOrElse(sys.error("couldn't parse RA"))
    val dec = Angle.parseDMS(decStr).getOrElse(sys.error("couldn't parse Dec"))
    new SPTarget(ra.toDegrees, dec.toDegrees)
  }

  def candidate(name: String, raStr: String, decStr: String, mags: (Double, MagnitudeBand)*): SiderealTarget = {
    val ra     = RightAscension.fromAngle(Angle.parseHMS(raStr).getOrElse(sys.error("couldn't parse RA")))
    val dec    = Declination.fromAngle(Angle.parseDMS(decStr).getOrElse(sys.error("couldn't parse Dec"))).getOrElse(sys.error("invalid dec"))
    val coords = Coordinates(ra, dec)
    val ms     = mags.map { case (value, band) => Magnitude(value, band, None, Vega) }.toList
    SiderealTarget.empty.copy(name = name, coordinates = coords, magnitudes = ms)
  }

  "SingleProbeStrategy" should {
    "find a guide star for NIRI+NGS, OCSADV-245" in {
      // zeta Gem target
      val ra = Angle.fromHMS(7, 4, 6.531).getOrElse(Angle.zero)
      val dec = Angle.fromDMS(20, 34, 13.070).getOrElse(Angle.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new InstNIRI <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(AltairAowfsKey, SingleProbeStrategyParams.AltairAowfsParams, Some(TestVoTableBackend("/ocsadv245.xml")))
      val aoComp = new InstAltair <| {_.setMode(AltairParams.Mode.NGS)}
      val ctx = ObsContext.create(env, inst, new JSome(Site.GN), SPSiteQuality.Conditions.BEST, null, aoComp, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "553-036128", AltairAowfsGuider.instance)
    }
    "find a guide star for NIRI+LGS, OCSADV-245" in {
      // Pal 12 target
      val ra = Angle.fromHMS(21, 46, 38.840).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.fromDegrees(338.747389)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new InstNIRI <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(AltairAowfsKey, SingleProbeStrategyParams.AltairAowfsParams, Some(TestVoTableBackend("/ocsadv-245-lgs.xml")))
      val aoComp = new InstAltair <| {_.setMode(AltairParams.Mode.LGS)}
      val ctx = ObsContext.create(env, inst, new JSome(Site.GN), SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY), null, aoComp, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "344-198748", AltairAowfsGuider.instance)
    }
    "find a guide star for NIRI+PWFS1, OCSADV-255" in {
      // HIP 1000 target
      val ra = Angle.fromHMS(0, 12, 30.286).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(22, 4, 2.34).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val guiders = Set[GuideProbe](NiriOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new InstNIRI <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs1NorthKey, SingleProbeStrategyParams.PwfsParams(Site.GN, PwfsGuideProbe.pwfs1), Some(TestVoTableBackend("/niri_pwfs1.xml")))

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY).cc(SPSiteQuality.CloudCover.PERCENT_80).iq(SPSiteQuality.ImageQuality.PERCENT_85)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GN), conditions, null, null, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "340-000202", PwfsGuideProbe.pwfs1)
    }
    "find a guide star for NIRI+PWFS2, OCSADV-255" in {
      // HIP 1024 target
      val ra = Angle.fromHMS(0, 12, 45.821).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.fromDMS(1, 18, 34.79).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](NiriOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target)
      val inst = new InstNIRI <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2NorthKey, SingleProbeStrategyParams.PwfsParams(Site.GN, PwfsGuideProbe.pwfs2), Some(TestVoTableBackend("/niri_pwfs2.xml")))

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY).cc(SPSiteQuality.CloudCover.PERCENT_80).iq(SPSiteQuality.ImageQuality.PERCENT_85)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GN), conditions, null, null, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "458-000297", PwfsGuideProbe.pwfs2)
    }
    "find a guide star for GMOS-N+OIWFS, OCSADV-255" in {
      // NGC 101 target
      val ra = Angle.fromHMS(0, 23, 54.614).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(32, 32, 10.34).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target)
      val inst = new InstGmosNorth <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(GmosNorthOiwfsKey, SingleProbeStrategyParams.GmosOiwfsParams(Site.GN), Some(TestVoTableBackend("/gmosn_oiwfs.xml")))

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GN), conditions, null, null, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "288-000438", GmosOiwfsGuideProbe.instance)
    }
    "find a guide star for this canned example" in {
      // This is just a random test setup that was failing in property tests.
      // I thought I should just leave it once it was fixed.

      val base    = spTarget("21:32:48.000", "-0:06:00.000")
      val env     = TargetEnvironment.create(base)

      val inst    = new InstGmosNorth          <|
        { _.setPosAngle(0.0) }                 <|
        { _.setIssPort(IssPort.SIDE_LOOKING) } <|
        { _.setFPUnit(GmosNorthType.FPUnitNorth.NS_5) }

      val os = offsets(
        (-25, -19),
        ( 37,  -1),
        ( 45,  40)
      )

      val cand       = candidate("Biff", "21:32:46.960", "-0:04:51.135", (16.64, _r), (17.0, R), (20.0, UC))
      val voTable    = CannedBackend(List(cand))

      val strategy   = SingleProbeStrategy(GmosNorthOiwfsKey, SingleProbeStrategyParams.GmosOiwfsParams(Site.GN), Some(voTable))
      val conditions = SPSiteQuality.Conditions.NOMINAL
      val ctx        = ObsContext.create(env, inst, new JSome(Site.GN), conditions, os.asJava, null, JNone.instance())
      val selection  = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)
      verifyGuideStarSelection(strategy, ctx, selection, "Biff", GmosOiwfsGuideProbe.instance, PossibleIqDegradation)
    }
    "when there is equal vignetting at a given pos angle, pick the brightest option" in {
      val base    = spTarget("12:00:00", "85:00:00.000")
      val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance)
      val env     = TargetEnvironment.create(base)

      val inst    = new InstGmosNorth          <|
        { _.setPosAngle(45.0) }                <|
        { _.setIssPort(IssPort.SIDE_LOOKING) } <|
        { _.setFPUnit(GmosNorthType.FPUnitNorth.NS_5) }

      val os = offsets(
        (-25, -25),
        (-25,  25),
        ( 25, -25),
        ( 25,  25)
      )

      val dim        = candidate("Biff Dim",    "12:00:05", "85:03:30", (16.3,  R))
      val bright     = candidate("Biff Bright", "12:00:05", "85:02:30", (16.1, UC))
      val medium     = candidate("Biff Medium", "12:00:05", "85:02:30", (16.2, _r))
      val voTable    = CannedBackend(List(dim, bright, medium))

      val strategy   = SingleProbeStrategy(GmosNorthOiwfsKey, SingleProbeStrategyParams.GmosOiwfsParams(Site.GN), Some(voTable))
      val conditions = SPSiteQuality.Conditions.NOMINAL
      val ctx        = ObsContext.create(env, inst, new JSome(Site.GN), conditions, os.asJava, null, JNone.instance())
      val selection  = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)
      verifyGuideStarSelection(strategy, ctx, selection, "Biff Bright", GmosOiwfsGuideProbe.instance)
    }
    "when there is equal vignetting at a multiple pos angles, pick the brightest option" in {
      val base    = spTarget("12:00:00", "85:00:00.000")
      val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance)
      val env     = TargetEnvironment.create(base)

      val inst    = new InstGmosNorth          <|
        { _.setPosAngle(45.0) }                <|
        { _.setIssPort(IssPort.SIDE_LOOKING) } <|
        { _.setPosAngleConstraint(FIXED_180) } <|
        { _.setFPUnit(GmosNorthType.FPUnitNorth.NS_5) }

      val os = offsets(
        (-25, -25),
        (-25,  25),
        ( 25, -25),
        ( 25,  25)
      )

      val dim        = candidate("Biff Dim",         "12:00:05", "85:03:30", (16.3,  R))
      val bright     = candidate("Biff Bright",      "12:00:05", "85:02:30", (16.1, UC))
      val medium     = candidate("Biff Medium",      "12:00:05", "85:02:30", (16.2, _r))
      val flipDim    = candidate("Biff Flip Dim",    "12:00:05", "84:58:30", (16.2, UC))
      val flipBright = candidate("Biff Flip Bright", "12:00:05", "84:57:30", (16.0,  R))
      val flipMedium = candidate("Biff Flip Medium", "12:00:05", "84:56:30", (16.1, _r))
      val voTable    = CannedBackend(List(dim, bright, medium, flipMedium, flipBright, flipDim))

      val strategy   = SingleProbeStrategy(GmosNorthOiwfsKey, SingleProbeStrategyParams.GmosOiwfsParams(Site.GN), Some(voTable))
      val conditions = SPSiteQuality.Conditions.NOMINAL
      val ctx        = ObsContext.create(env, inst, new JSome(Site.GN), conditions, os.asJava, null, JNone.instance())
      val selection  = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)
      verifyGuideStarSelection(strategy, ctx, selection, "Biff Flip Bright", GmosOiwfsGuideProbe.instance)
    }
    "find a guide star for GMOS-N+PWFS2, OCSADV-255" in {
      // M1 target
      val ra = Angle.fromHMS(5, 34, 31.940).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.fromDMS(22, 0, 52.20).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target)
      val inst = new InstGmosNorth <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2NorthKey, SingleProbeStrategyParams.PwfsParams(Site.GN, PwfsGuideProbe.pwfs2), Some(TestVoTableBackend("/gmosn_pwfs2.xml")))

      val conditions = SPSiteQuality.Conditions.WORST
      val ctx = ObsContext.create(env, inst, new JSome(Site.GN), conditions, null, null, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "560-017530", PwfsGuideProbe.pwfs2)
    }
    "find a guide star for Flamingos2+OIWFS, OCSADV-255" in {
      // RMC 136 target
      val ra = Angle.fromHMS(5, 38, 42.396).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 6, 3.36).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](Flamingos2OiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target)
      val inst = new Flamingos2 <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Flamingos2OiwfsKey, SingleProbeStrategyParams.Flamingos2OiwfsParams, Some(TestVoTableBackend("/f2_oiwfs.xml")))

      val conditions = SPSiteQuality.Conditions.WORST
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, null, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "105-014127", Flamingos2OiwfsGuideProbe.instance)
    }
    "find a guide star for Flamingos2+PWFS2, OCSADV-255" in {
      // RMC 136 target
      val ra = Angle.fromHMS(5, 38, 42.396).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(69, 6, 3.36).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](Flamingos2OiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target)
      val inst = new Flamingos2 <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2SouthKey, SingleProbeStrategyParams.PwfsParams(Site.GS, PwfsGuideProbe.pwfs2), Some(TestVoTableBackend("/f2_pwfs2.xml")))

      val conditions = SPSiteQuality.Conditions.WORST.cc(SPSiteQuality.CloudCover.PERCENT_70)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, null, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "105-014476", PwfsGuideProbe.pwfs2)
    }
    "find a guide star for GMOS-S+OIWFS, OCSADV-255" in {
      // LMC 1 target
      val ra = Angle.fromHMS(5, 25, 1.110).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(62, 28, 48.90).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target)
      val inst = new InstGmosSouth <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(GmosSouthOiwfsKey, SingleProbeStrategyParams.GmosOiwfsParams(Site.GS), Some(TestVoTableBackend("/gmoss_oiwfs.xml")))

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, null, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      // 138-005574 is slightly brighter but vignettes a bit more
      verifyGuideStarSelection(strategy, ctx, selection, "138-005571", GmosOiwfsGuideProbe.instance)
    }
    "find a guide star for GMOS-S+PWFS2, OCSADV-255" in {
      // Blanco 1 target
      val ra = Angle.fromHMS(0, 4, 7).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(29, 50, 0).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GmosOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target)
      val inst = new InstGmosSouth <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2SouthKey, SingleProbeStrategyParams.PwfsParams(Site.GS, PwfsGuideProbe.pwfs2), Some(TestVoTableBackend("/gmoss_pwfs2.xml")))

      val conditions = SPSiteQuality.Conditions.NOMINAL.sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, null, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "302-000084", PwfsGuideProbe.pwfs2)
    }
    "find a guide star for GNIRS, OCSADV-255" in {
      // Pleiades target
      val ra = Angle.fromHMS(3, 47, 0).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.fromDMS(24, 7, 0).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GnirsOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target)
      val inst = new InstGNIRS <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2NorthKey, SingleProbeStrategyParams.PwfsParams(Site.GN, PwfsGuideProbe.pwfs2), Some(TestVoTableBackend("/gnirs_1.xml")))

      val conditions = SPSiteQuality.Conditions.NOMINAL.cc(SPSiteQuality.CloudCover.PERCENT_70).sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GN), conditions, null, null, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "571-008701", PwfsGuideProbe.pwfs2)
    }
    "find a guide star for GNIRS Part II, OCSADV-255" in {
      // Orion target
      val ra = Angle.fromHMS(5, 35, 17.3).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(5, 23, 28).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val guiders = Set[GuideProbe](GnirsOiwfsGuideProbe.instance, PwfsGuideProbe.pwfs1, PwfsGuideProbe.pwfs2)
      val env = TargetEnvironment.create(target)
      val inst = new InstGNIRS <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2NorthKey, SingleProbeStrategyParams.PwfsParams(Site.GN, PwfsGuideProbe.pwfs2), Some(TestVoTableBackend("/gnirs_2.xml")))

      val conditions = SPSiteQuality.Conditions.NOMINAL.cc(SPSiteQuality.CloudCover.PERCENT_70).sb(SPSiteQuality.SkyBackground.ANY)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GN), conditions, null, null, JNone.instance())

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "424-010170", PwfsGuideProbe.pwfs2)
    }
    "not find a guide star for Phoenix with WORST conditions, REL-2436" in {
      // Beta-Pictoris target
      val ra = Angle.fromHMS(5, 47, 17.088).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(51, 3, 59.441).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new InstPhoenix <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2SouthKey, SingleProbeStrategyParams.PwfsParams(Site.GS, PwfsGuideProbe.pwfs2), Some(TestVoTableBackend("/beta_pictoris.xml")))

      // Conditions will not let it go through
      val conditions = SPSiteQuality.Conditions.WORST
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, null, JNone.instance())

      val estimate = Await.result(strategy.estimate(ctx, magTable)(implicitly), 10.seconds)

      estimate should beEqualTo(Estimate.CompleteFailure)

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)
      selection should beNone
    }
    "find a guide star for Phoenix with slightly better conditions, REL-2436" in {
      // Beta-Pictoris target
      val ra = Angle.fromHMS(5, 47, 17.088).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(51, 3, 59.441).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new InstPhoenix <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2SouthKey, SingleProbeStrategyParams.PwfsParams(Site.GS, PwfsGuideProbe.pwfs2), Some(TestVoTableBackend("/beta_pictoris.xml")))

      // Conditions not that bad
      val conditions = SPSiteQuality.Conditions.WORST.cc(SPSiteQuality.CloudCover.PERCENT_70).iq(SPSiteQuality.ImageQuality.PERCENT_85)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, null, JNone.instance())

      val estimate = Await.result(strategy.estimate(ctx, magTable)(implicitly), 10.seconds)

      estimate should beEqualTo(Estimate.GuaranteedSuccess)

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx, selection, "195-006563", PwfsGuideProbe.pwfs2)
    }
    "find a guide star for Phoenix with better conditions but target shifted, REL-2436" in {
      // Beta-Pictoris target
      val ra = Angle.fromHMS(5, 47, 22.207).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(51, 2, 14.650).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new InstPhoenix <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2SouthKey, SingleProbeStrategyParams.PwfsParams(Site.GS, PwfsGuideProbe.pwfs2), Some(TestVoTableBackend("/beta_pictoris_shifted.xml")))

      // Conditions not that bad
      val conditions = SPSiteQuality.Conditions.WORST.cc(SPSiteQuality.CloudCover.PERCENT_70).iq(SPSiteQuality.ImageQuality.PERCENT_85)
      val ctx = ObsContext.create(env, inst, new JSome(Site.GS), conditions, null, null, JNone.instance())

      val estimate = Await.result(strategy.estimate(ctx, magTable)(implicitly), 10.seconds)

      estimate should beEqualTo(Estimate.CompleteFailure)

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)
      selection should beNone
    }
    "find a guide star for Visitor even if the site is not defined, REL-2476" in {
      // Beta-Pictoris target
      val ra = Angle.fromHMS(5, 47, 17.088).getOrElse(Angle.zero)
      val dec = Declination.fromAngle(Angle.zero - Angle.fromDMS(51, 3, 59.441).getOrElse(Angle.zero)).getOrElse(Declination.zero)
      val target = new SPTarget(ra.toDegrees, dec.toDegrees)
      val env = TargetEnvironment.create(target)
      val inst = new VisitorInstrument <| {_.setPosAngle(0.0)}

      val strategy = SingleProbeStrategy(Pwfs2SouthKey, SingleProbeStrategyParams.PwfsParams(Site.GS, PwfsGuideProbe.pwfs2), Some(TestVoTableBackend("/beta_pictoris.xml")))

      val conditions = SPSiteQuality.Conditions.WORST.cc(SPSiteQuality.CloudCover.PERCENT_70).iq(SPSiteQuality.ImageQuality.PERCENT_85)
      // Note, no site defined
      val ctx = ObsContext.create(env, inst, JNone.instance(), conditions, null, null, JNone.instance())

      val estimate = Await.result(strategy.estimate(ctx, magTable)(implicitly), 10.seconds)

      estimate should beEqualTo(Estimate.GuaranteedSuccess)

      val selection = Await.result(strategy.select(ctx, magTable)(implicitly), 10.seconds)

      verifyGuideStarSelection(strategy, ctx.withSite(new JSome(Site.GS)), selection, "195-006563", PwfsGuideProbe.pwfs2)
    }
  }

  def verifyGuideStarSelection(strategy: SingleProbeStrategy, ctx: ObsContext, selection: Option[Selection], expectedName: String, gp: ValidatableGuideProbe, quality: AgsGuideQuality = DeliversRequestedIq): MatchResult[Option[AgsGuideQuality]] = {
    // One guide star found
    selection.map(_.assignments.size) should beSome(1)
    selection.flatMap(_.assignments.headOption.map(_.guideProbe)) should beSome(gp)
    val guideStar = selection.flatMap(_.assignments.headOption.map(_.guideStar))
    guideStar.map(_.name) should beSome(expectedName)
    // Add GS to targets
    val newCtx = selection.map(_.applyTo(ctx))
    val analyzedSelection = ~newCtx.map(strategy.analyze(_, magTable))
    analyzedSelection should be size 1
    analyzedSelection.headOption.map(_.quality) should beSome(quality)

    // Analyze Single Guide Star
    val analyzedGS = (newCtx |@| guideStar) { strategy.analyze(_, magTable, gp, _) }.flatten
    analyzedGS.map(_.quality) should beSome(quality)
  }
}
