package edu.gemini.spModel.test

import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.None
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.{Offset => SkyCalcOffset}
import edu.gemini.spModel.ags.AgsStrategyKey
import edu.gemini.spModel.core.{OffsetQ, OffsetP, Offset, Angle, Arbitraries}
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.gemini.altair.{AltairParams, InstAltair}
import edu.gemini.spModel.gemini.flamingos2.Flamingos2
import edu.gemini.spModel.gemini.gmos.GmosCommonType.FPUnitMode._
import edu.gemini.spModel.gemini.gmos.{GmosCommonType, GmosSouthType, InstGmosSouth, GmosNorthType, InstGmosNorth}
import edu.gemini.spModel.gemini.gsaoi.Gsaoi
import edu.gemini.spModel.gemini.niri.{InstNIRI, Niri}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, Conditions, ImageQuality, SkyBackground, WaterVapor}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.{PosAngleConstraint, IssPort}

import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

import scala.collection.JavaConverters._

import scalaz._, Scalaz._


trait SpModelArbitraries extends Arbitraries with edu.gemini.spModel.target.env.Arbitraries {

  // TODO: Eventually all instruments should be included and all instrument
  // TODO: features as well.  They should probably also be broken out into
  // TODO: individual Arbitraries.

  implicit val arbAltairMode: Arbitrary[AltairParams.Mode] =
    Arbitrary { Gen.oneOf(AltairParams.Mode.values) }

  implicit val arbAltair: Arbitrary[InstAltair] =
    Arbitrary {
      arbitrary[AltairParams.Mode].map { mode =>
        new InstAltair <|
          (_.setMode(mode))
      }
    }

  implicit val arbFlamingos2Fpu: Arbitrary[Flamingos2.FPUnit] =
    Arbitrary { Gen.oneOf(Flamingos2.FPUnit.values) }

  implicit val arbFlamingos2Lyout: Arbitrary[Flamingos2.LyotWheel] =
    Arbitrary { Gen.oneOf(Flamingos2.LyotWheel.values) }

  implicit val arbFlamingos2: Arbitrary[Flamingos2] =
    Arbitrary {
      for {
        filter    <- Gen.oneOf(Flamingos2.Filter.values)
        fpu       <- arbitrary[Flamingos2.FPUnit]
        disperser <- Gen.oneOf(Flamingos2.Disperser.values)
        lyot      <- arbitrary[Flamingos2.LyotWheel]
        port      <- arbitrary[IssPort]
        posAngle  <- arbitrary[Angle]
        pac       <- arbitrary[PosAngleConstraint]
      } yield
        new Flamingos2()                       <|
          (_.setFilter(filter))                <|
          (_.setFpu(fpu))                      <|
          (_.setDisperser(disperser))          <|
          (_.setLyotWheel(lyot))               <|
          (_.setIssPort(port))                 <|
          (_.setPosAngleConstraint(pac))       <|
          (_.setPosAngle(posAngle.toDegrees))

    }

  implicit val argGmosFpuMode: Arbitrary[GmosCommonType.FPUnitMode] =
    Arbitrary { Gen.oneOf(CUSTOM_MASK, BUILTIN) }

  implicit val arbGmosNFpu: Arbitrary[GmosNorthType.FPUnitNorth] =
    Arbitrary { Gen.oneOf(GmosNorthType.FPUnitNorth.values) }

  implicit val arbGmosN: Arbitrary[InstGmosNorth] =
    Arbitrary {
      for {
        filter   <- Gen.oneOf(GmosNorthType.FilterNorth.values)
        fpu      <- arbitrary[GmosNorthType.FPUnitNorth]
        mode     <- Gen.frequency((1, CUSTOM_MASK), (9, BUILTIN))
        port     <- arbitrary[IssPort]
        posAngle <- arbitrary[Angle]
        pac      <- arbitrary[PosAngleConstraint]
      } yield
        new InstGmosNorth                      <|
          (_.setFilter(filter))                <|
          (_.setFPUnit(fpu))                   <|
          (_.setFPUnitMode(mode))              <|
          (_.setIssPort(port))                 <|
          (_.setPosAngleConstraint(pac))       <|
          (_.setPosAngle(posAngle.toDegrees))
    }

  implicit val arbGmosSFpu: Arbitrary[GmosSouthType.FPUnitSouth] =
    Arbitrary { Gen.oneOf(GmosSouthType.FPUnitSouth.values) }

  implicit val arbGmosS: Arbitrary[InstGmosSouth] =
    Arbitrary {
      for {
        filter   <- Gen.oneOf(GmosSouthType.FilterSouth.values)
        fpu      <- arbitrary[GmosSouthType.FPUnitSouth]
        mode     <- Gen.frequency((1, CUSTOM_MASK), (9, BUILTIN))
        port     <- arbitrary[IssPort]
        posAngle <- arbitrary[Angle]
        pac      <- arbitrary[PosAngleConstraint]
      } yield
        new InstGmosSouth                     <|
          (_.setFilter(filter))               <|
          (_.setFPUnit(fpu))                  <|
          (_.setFPUnitMode(mode))             <|
          (_.setIssPort(port))                <|
          (_.setPosAngleConstraint(pac))      <|
          (_.setPosAngle(posAngle.toDegrees))
    }

  implicit val arbGsaoi: Arbitrary[Gsaoi] =
    Arbitrary {
      for {
        port     <- arbitrary[IssPort]
        posAngle <- arbitrary[Angle]
      } yield
        new Gsaoi                             <|
          (_.setIssPort(port))                <|
          (_.setPosAngle(posAngle.toDegrees))
    }

  implicit val arbNiriCamera: Arbitrary[Niri.Camera] =
    Arbitrary { Gen.oneOf(Niri.Camera.values) }

  implicit val arbNiri: Arbitrary[InstNIRI] =
    Arbitrary {
      for {
        camera    <- arbitrary[Niri.Camera]
        disperser <- Gen.oneOf(Niri.Disperser.values)
        posAngle  <- arbitrary[Angle]
      } yield
        new InstNIRI                        <|
          (_.setCamera(camera))             <|
          (_.setDisperser(disperser))       <|
          (_.setPosAngle(posAngle.toDegrees))
    }

  implicit val arbInst: Arbitrary[SPInstObsComp] =
    Arbitrary {
      Gen.oneOf(
        arbitrary[Flamingos2],
        arbitrary[InstGmosNorth],
        arbitrary[InstGmosSouth],
        arbitrary[Gsaoi],
        arbitrary[InstNIRI]
      )
    }

  val genSmallOffset: Gen[SkyCalcOffset] =
    for {
      pi <- Gen.chooseNum(-50, 50)
      qi <- Gen.chooseNum(-50, 50)
    } yield Offset(pi.toDouble.arcsecs[OffsetP], qi.toDouble.arcsecs[OffsetQ]).toOldModel

  implicit val arbSciencePosSet: Arbitrary[java.util.Set[SkyCalcOffset]] =
    Arbitrary {
      for {
        count <- Gen.chooseNum(1, 4)
        offs  <- Gen.listOfN(count, genSmallOffset)
      } yield new java.util.HashSet(offs.asJava)
    }

  implicit val arbCloudCover: Arbitrary[CloudCover] =
    Arbitrary { Gen.oneOf(CloudCover.values) }

  implicit val arbImageQuality: Arbitrary[ImageQuality] =
    Arbitrary { Gen.oneOf(ImageQuality.values) }

  implicit val arbSkyBackground: Arbitrary[SkyBackground] =
    Arbitrary { Gen.oneOf(SkyBackground.values) }

  implicit val arbWaterVapor: Arbitrary[WaterVapor] =
    Arbitrary { Gen.oneOf(WaterVapor.values) }

  implicit val arbConditions: Arbitrary[Conditions] =
    Arbitrary {
      for {
        cc <- arbitrary[CloudCover]
        iq <- arbitrary[ImageQuality]
        sb <- arbitrary[SkyBackground]
        wv <- arbitrary[WaterVapor]
      } yield new Conditions(cc, iq, sb, wv)
    }

  implicit val arbPositionAngleConstraint: Arbitrary[PosAngleConstraint] =
    Arbitrary { Gen.oneOf(PosAngleConstraint.values) }

  implicit val arbIssPort: Arbitrary[IssPort] =
    Arbitrary { Gen.oneOf(IssPort.values) }

  implicit val arbAgsStrategy: Arbitrary[AgsStrategyKey] =
    Arbitrary { Gen.oneOf(AgsStrategyKey.All) }

  implicit val arbContext: Arbitrary[ObsContext] =
    Arbitrary {
      for {
        ags  <- arbitrary[Option[AgsStrategyKey]]
        env  <- arbitrary[TargetEnvironment]
        inst <- arbitrary[SPInstObsComp]
        site  = inst.getSite.asScala.headOption.asGeminiOpt
        cond <- arbitrary[Conditions]
        offs <- arbitrary[java.util.Set[SkyCalcOffset]]
      } yield ObsContext.create(ags.asGeminiOpt, env, inst, site, cond, offs, null, None.instance())
    }
}
