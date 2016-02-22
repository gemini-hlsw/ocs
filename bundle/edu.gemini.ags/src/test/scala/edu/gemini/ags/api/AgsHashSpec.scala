package edu.gemini.ags.api

import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.{Offset => SkyCalcOffset}
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{Declination, Angle}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.{GmosCommonType, InstGmosSouth, GmosSouthType, GmosNorthType, InstGmosNorth}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, Conditions, ImageQuality, SkyBackground, WaterVapor}
import edu.gemini.spModel.obs.context.ObsContext
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

    def scale(d: Double, s: Int): Double =
      BigDecimal(d).setScale(s, BigDecimal.RoundingMode.HALF_UP).doubleValue()

    "differ if the position angle changes significantly" in
      forAll { (ctx0: ObsContext, pa: Angle) =>
        val ctx1 = ctx0.withPositionAngle(pa.toOldModel)

        val pa0  = ctx0.getPositionAngle.toDegrees.getMagnitude
        val pa1  = ctx1.getPositionAngle.toDegrees.getMagnitude

        (scale(pa0, 3) == scale(pa1, 3)) == hashSame(ctx0, ctx1)
      }

    "differ if RA changes" in
      forAll { (ctx0: ObsContext, ra: Angle) =>
        val env0  = ctx0.getTargets
        val base0 = env0.getBase
        val base1 = base0.clone() <| (_.setRaDegrees(ra.toDegrees))
        val env1  = env0.setBasePosition(base1)
        val ctx1  = ctx0.withTargets(env1)

        val time  = Option(new java.lang.Long(now.toEpochMilli)).asGeminiOpt
        val ra0   = base0.getTarget.getRaDegrees(time).getValue
        val ra1   = base1.getTarget.getRaDegrees(time).getValue

        (scale(ra0,8) == scale(ra1,8)) == hashSame(ctx0, ctx1)
      }

    "differ if Dec changes" in
      forAll { (ctx0: ObsContext, dec: Declination) =>
        val env0  = ctx0.getTargets
        val base0 = env0.getBase
        val base1 = base0.clone() <| (_.setDecDegrees(dec.toDegrees))
        val env1  = env0.setBasePosition(base1)
        val ctx1  = ctx0.withTargets(env1)

        val time  = Option(new java.lang.Long(now.toEpochMilli)).asGeminiOpt
        val dec0  = base0.getTarget.getDecDegrees(time).getValue
        val dec1  = base1.getTarget.getDecDegrees(time).getValue

        (scale(dec0,8) == scale(dec1,8)) == hashSame(ctx0, ctx1)
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

    val F2Key = Option(AgsStrategyKey.Flamingos2OiwfsKey: AgsStrategyKey).asGeminiOpt

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

    val GmosNKey = Option(AgsStrategyKey.GmosNorthOiwfsKey: AgsStrategyKey).asGeminiOpt

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

    val GmosSKey = Option(AgsStrategyKey.GmosSouthOiwfsKey: AgsStrategyKey).asGeminiOpt

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
  }
}
