package edu.gemini.ags.api

import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.{Option => JOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.{Offset => SkyCalcOffset}
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{Site, Declination, Angle}
import edu.gemini.spModel.gemini.altair.{AltairParams, InstAltair}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gems.Gems
import edu.gemini.spModel.gemini.gmos.{GmosCommonType, InstGmosSouth, GmosSouthType, GmosNorthType, InstGmosNorth}
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.niri.{InstNIRI, Niri}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, Conditions, ImageQuality, SkyBackground, WaterVapor}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.telescope.{IssPort, PosAngleConstraint, PosAngleConstraintAware}

import java.time.Instant

import org.scalacheck.Prop._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._

class AgsHashSpec extends Specification with ScalaCheck with edu.gemini.spModel.test.SpModelArbitraries {
  val now = Instant.now()

  def hashSame(ctx0: ObsContext, ctx1: ObsContext): Boolean =
    AgsHash.hash(ctx0, now) == AgsHash.hash(ctx1, now)

  def hashDiffers(ctx0: ObsContext, ctx1: ObsContext): Boolean =
    !hashSame(ctx0, ctx1)

  "AgsHash" should {
    "differ if the AGS strategy changes" in
      forAll { (ctx0: ObsContext, k: AgsStrategyKey) =>
        val key0 = AgsRegistrar.currentStrategy(ctx0).map(_.key)
        val ctx1 = ctx0.withAgsStrategyOverride(Option(k).asGeminiOpt)
        val key1 = AgsRegistrar.currentStrategy(ctx1).map(_.key)

        (key0 == key1) == hashSame(ctx0, ctx1)
      }

    "differ if the cloud cover changes" in
      forAll { (ctx0: ObsContext, cc: CloudCover) =>
        val ctx1 = ctx0.withConditions(ctx0.getConditions.cc(cc))
        (ctx0.getConditions.cc == cc) == hashSame(ctx0, ctx1)
      }

    "differ if the image quality changes" in
      forAll { (ctx0: ObsContext, iq: ImageQuality) =>
        val ctx1 = ctx0.withConditions(ctx0.getConditions.iq(iq))
        (ctx0.getConditions.iq == iq) == hashSame(ctx0, ctx1)
      }

    "differ if the sky background changes" in
      forAll { (ctx0: ObsContext, sb: SkyBackground) =>
        val ctx1 = ctx0.withConditions(ctx0.getConditions.sb(sb))
        (ctx0.getConditions.sb == sb) == hashSame(ctx0, ctx1)
      }

    "stay the same if only the water vapor changes" in
      forAll { (ctx0: ObsContext, wv: WaterVapor) =>
        hashSame(ctx0, ctx0.withConditions(ctx0.getConditions.wv(wv)))
      }

    "differ if multiple conditions change" in
      forAll { (ctx0: ObsContext, c: Conditions) =>
        val ctx1 = ctx0.withConditions(c)

        // ignoring water vapor, if the conditions differ the hash should differ
        val c1   = c.wv(ctx0.getConditions.wv)
        (ctx0.getConditions == c1) == hashSame(ctx0, ctx1)
      }

    "differ if the ISS Port differs" in
      forAll { (ctx0: ObsContext, iss: IssPort) =>
        val ctx1 = ctx0.withIssPort(iss)
        val iss1 = ctx1.getIssPort

        (ctx0.getIssPort == iss1) == hashSame(ctx0, ctx1)
      }

    "differ if the position angle constraint differs" in
      forAll { (ctx0: ObsContext, pac: PosAngleConstraint) =>
        val i1 = ctx0.getInstrument
        i1 match {
          case pacw: PosAngleConstraintAware => pacw.setPosAngleConstraint(pac)
          case _                             => // do nothing
        }

        val ctx1 = ctx0.withInstrument(i1)
        val pac1 = ctx1.getPosAngleConstraint

        (ctx0.getPosAngleConstraint == pac1) == hashSame(ctx0, ctx1)
      }

    "differ if the position angle changes significantly" in
      forAll { (ctx0: ObsContext, pa: Angle) =>
        val ctx1 = ctx0.withPositionAngle(pa)

        val pa0  = ctx0.getPositionAngle.toDegrees
        val pa1  = ctx1.getPositionAngle.toDegrees

        (pa0 == pa1) == hashSame(ctx0, ctx1)
      }

    "differ if RA changes" in
      forAll { (ctx0: ObsContext, ra: Angle) =>
        val env0  = ctx0.getTargets
        val ast0  = env0.getAsterism
        val ast1  = ast0.copyWithClonedTargets <| (_.allSpTargets.foreach(_.setRaDegrees(ra.toDegrees))) // update all targets?
        val env1  = env0.setAsterism(ast1)
        val ctx1  = ctx0.withTargets(env1)

        val time  = Option(new java.lang.Long(now.toEpochMilli)).asGeminiOpt
        val ra0   = ast0.allSpTargets.map(_.getRaDegrees(time))
        val ra1   = ast1.allSpTargets.map(_.getRaDegrees(time))

        (ra0 == ra1) == hashSame(ctx0, ctx1)
      }

    "differ if Dec changes" in
      forAll { (ctx0: ObsContext, dec: Declination) =>
        val env0  = ctx0.getTargets
        val ast0  = env0.getAsterism
        val ast1  = ast0.copyWithClonedTargets <| (_.allSpTargets.foreach(_.setDecDegrees(dec.toDegrees))) // update all targets?
        val env1  = env0.setAsterism(ast1)
        val ctx1  = ctx0.withTargets(env1)

        val time  = Option(new java.lang.Long(now.toEpochMilli)).asGeminiOpt
        val dec0  = ast0.allSpTargets.map(_.getDecDegrees(time))
        val dec1  = ast1.allSpTargets.map(_.getDecDegrees(time))

        (dec0 == dec1) == hashSame(ctx0, ctx1)
      }

    "differ if offset positions change" in
      forAll { (ctx0: ObsContext, posList: java.util.Set[SkyCalcOffset]) =>
        val ctx1 = ctx0.withSciencePositions(posList)

        (ctx0.getSciencePositions == ctx1.getSciencePositions) == hashSame(ctx0, ctx1)
      }

    "stay the same if only an unimportant instrument feature changes" in
      forAll { (ctx0: ObsContext) =>
        val i1 = ctx0.getInstrument match {
          case f2: Flamingos2    => f2 <| (_.setFilter(Flamingos2.Filter.K_SHORT))
          case gn: InstGmosNorth => gn <| (_.setFilter(GmosNorthType.FilterNorth.OIII_G0318))
          case gs: InstGmosSouth => gs <| (_.setFilter(GmosSouthType.FilterSouth.OIII_G0338))
          case i0                => i0
        }
        hashSame(ctx0, ctx0.withInstrument(i1))
      }

    def agsKey(k: AgsStrategyKey): JOption[AgsStrategyKey] =
      Option(k).asGeminiOpt

    val F2Key = agsKey(AgsStrategyKey.Flamingos2OiwfsKey)

    "differ if Flamingos2 FPU changes" in
      forAll { (ctx0: ObsContext, f: Flamingos2, fpu2: Flamingos2.FPUnit) =>
        val fpu1 = f.getFpu
        val ctx1 = ctx0.withInstrument(f).withAgsStrategyOverride(F2Key)
        val ctx2 = ctx1.withInstrument(f <| (_.setFpu(fpu2)))

        (fpu1 == fpu2) == hashSame(ctx1, ctx2)
      }

    "differ if Flamingos2 plate scale changes" in
      forAll { (ctx0: ObsContext, f: Flamingos2, ly2: Flamingos2.LyotWheel) =>
        val ly1  = f.getLyotWheel
        val ctx1 = ctx0.withInstrument(f).withAgsStrategyOverride(F2Key)
        val ctx2 = ctx1.withInstrument(f <| (_.setLyotWheel(ly2)))

        (ly1.getPlateScale == ly2.getPlateScale) == hashSame(ctx1, ctx2)
      }

    val GmosNKey = agsKey(AgsStrategyKey.GmosNorthOiwfsKey)

    "differ if GMOS North FPU changes" in
      forAll { (ctx0: ObsContext, g: InstGmosNorth, fpu2: GmosNorthType.FPUnitNorth) =>
        val fpu1 = g.getFPUnit
        val ctx1 = ctx0.withInstrument(g).withAgsStrategyOverride(GmosNKey)
        val ctx2 = ctx1.withInstrument(g <| (_.setFPUnit(fpu2)))

        (fpu1 == fpu2) == hashSame(ctx1, ctx2)
      }

    "differ if GMOS North FPU Mode changes" in
      forAll { (ctx0: ObsContext, g: InstGmosNorth, mode2: GmosCommonType.FPUnitMode) =>
        val mode1 = g.getFPUnitMode
        val ctx1  = ctx0.withInstrument(g).withAgsStrategyOverride(GmosNKey)
        val ctx2  = ctx1.withInstrument(g <| (_.setFPUnitMode(mode2)))

        (mode1 == mode2) == hashSame(ctx1, ctx2)
      }

    val GmosSKey = agsKey(AgsStrategyKey.GmosSouthOiwfsKey)

    "differ if GMOS South FPU changes" in
      forAll { (ctx0: ObsContext, g: InstGmosSouth, fpu2: GmosSouthType.FPUnitSouth) =>
        val fpu1 = g.getFPUnit
        val ctx1 = ctx0.withInstrument(g).withAgsStrategyOverride(GmosSKey)
        val ctx2 = ctx1.withInstrument(g <| (_.setFPUnit(fpu2)))

        (fpu1 == fpu2) == hashSame(ctx1, ctx2)
      }

    "differ if GMOS South FPU Mode changes" in
      forAll { (ctx0: ObsContext, g: InstGmosSouth, mode2: GmosCommonType.FPUnitMode) =>
        val mode1 = g.getFPUnitMode
        val ctx1  = ctx0.withInstrument(g).withAgsStrategyOverride(GmosSKey)
        val ctx2  = ctx1.withInstrument(g <| (_.setFPUnitMode(mode2)))

        (mode1 == mode2) == hashSame(ctx1, ctx2)
      }

    def testPwfs(ctx0: ObsContext, n: InstNIRI, camera2: Niri.Camera, k: AgsStrategyKey, f: (SPInstObsComp) => edu.gemini.skycalc.Angle): Boolean = {
      val ctx1 = ctx0.withInstrument(n).withSite(Option(Site.GN).asGeminiOpt).withAgsStrategyOverride(agsKey(k))
      val ctx2 = ctx1.withInstrument(n <| (_.setCamera(camera2)))

      def vc(ctx: ObsContext): Double = f(ctx.getInstrument).getMagnitude

      (vc(ctx1) == vc(ctx2)) == hashSame(ctx1, ctx2)
    }

    "differ if PWFS1 vignetting clearance changes" in
      forAll { (ctx0: ObsContext, n: InstNIRI, camera2: Niri.Camera) =>
        testPwfs(ctx0, n, camera2, AgsStrategyKey.Pwfs1NorthKey, _.pwfs1VignettingClearance)
      }

    "differ if PWFS2 vignetting clearance changes" in
      forAll { (ctx0: ObsContext, n: InstNIRI, camera2: Niri.Camera) =>
        testPwfs(ctx0, n, camera2, AgsStrategyKey.Pwfs2NorthKey, _.pwfs2VignettingClearance)
      }

    val GemsKey = agsKey(AgsStrategyKey.GemsKey)

    "differ between GeMS GSAOI vs Flamingos2" in
      forAll { (ctx0: ObsContext) =>
        val f    = new Flamingos2
        val g    = new Gsaoi

        val ctx1 = ctx0.withSite(Option(Site.GS).asGeminiOpt)
                       .withAOComponent(new Gems)
                       .withAgsStrategyOverride(GemsKey)
                       .withInstrument(f)

        val ctx2 = ctx1.withInstrument(g)

        hashDiffers(ctx1, ctx2)
      }

    "differ if Altair mode differs" in
      forAll { (ctx: ObsContext, gmosN: InstGmosNorth, altair1: InstAltair, mode2: AltairParams.Mode) =>
        val ctx1    = ctx.withInstrument(gmosN).withAOComponent(altair1)
        val mode1   = altair1.getMode

        val altair2 = altair1.clone.asInstanceOf[InstAltair]
        altair2.setMode(mode2)
        val ctx2    = ctx1.withAOComponent(altair2)

        (mode1 == mode2) == hashSame(ctx1, ctx2)
      }
  }
}
