package edu.gemini.spModel.target.env

import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AlmostEqual.AlmostEqualOps
import edu.gemini.spModel.core.Target._
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.gemini.ghost.GhostAsterism._
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GuideFiberState._
import edu.gemini.spModel.gemini.ghost.GhostParamSetCodecs._
import edu.gemini.spModel.pio.codec._
import edu.gemini.spModel.target.SPTarget

import org.scalacheck.{Arbitrary, Gen, Properties}
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._


class AsterismParamSetCodecSpec extends Specification with ScalaCheck with Arbitraries {

  implicit val SPTargetAlmostEqual: AlmostEqual[SPTarget] =
    AlmostEqual[Target].contramap(_.getTarget)

  implicit val SingleAsterismAlmostEqual: AlmostEqual[Asterism.Single] =
    AlmostEqual[SPTarget].contramap[Asterism.Single](_.t)


  implicit val GhostTargetAlmostEqual: AlmostEqual[GhostTarget] =
    new AlmostEqual[GhostTarget] {
      override def almostEqual(a: GhostTarget, b: GhostTarget): Boolean =
        (a.spTarget ~= b.spTarget) && (a.explicitGuideFiberState === b.explicitGuideFiberState)
    }

  implicit val GhostStdResSingleTargetAlmostEqual: AlmostEqual[GhostStandardResTargets.SingleTarget] =
    AlmostEqual[GhostTarget].contramap[GhostStandardResTargets.SingleTarget](_.target)

  implicit val GhostStdResDualTargetAlmostEqual: AlmostEqual[GhostStandardResTargets.DualTarget] =
    new AlmostEqual[GhostStandardResTargets.DualTarget] {
      override def almostEqual(a: GhostStandardResTargets.DualTarget, b: GhostStandardResTargets.DualTarget): Boolean =
        (a.target1 ~= b.target1) && (a.target2 ~= b.target2)
    }

  implicit val GhostStdResTargetPlusSkyAlmostEqual: AlmostEqual[GhostStandardResTargets.TargetPlusSky] =
    new AlmostEqual[GhostStandardResTargets.TargetPlusSky] {
      override def almostEqual(a: GhostStandardResTargets.TargetPlusSky, b: GhostStandardResTargets.TargetPlusSky): Boolean =
        (a.target ~= b.target) && (a.sky ~= b.sky)
    }
  implicit val GhostStdResSkyPlusTargetAlmostEqual: AlmostEqual[GhostStandardResTargets.SkyPlusTarget] =
    new AlmostEqual[GhostStandardResTargets.SkyPlusTarget] {
      override def almostEqual(a: GhostStandardResTargets.SkyPlusTarget, b: GhostStandardResTargets.SkyPlusTarget): Boolean =
        (a.target ~= b.target) && (a.sky ~= b.sky)
    }

  implicit val GhostAsterismStdResolutionAlmostEqual: AlmostEqual[GhostAsterism.StandardResolution] =
    new AlmostEqual[GhostAsterism.StandardResolution] {
      override def almostEqual(a: StandardResolution, b: StandardResolution): Boolean = (a.base ~= b.base) && ((a.targets, b.targets) match {
        case (at: GhostStandardResTargets.SingleTarget,  bt: GhostStandardResTargets.SingleTarget)  => at ~= bt
        case (at: GhostStandardResTargets.DualTarget,    bt: GhostStandardResTargets.DualTarget)    => at ~= bt
        case (at: GhostStandardResTargets.TargetPlusSky, bt: GhostStandardResTargets.TargetPlusSky) => at ~= bt
        case (at: GhostStandardResTargets.SkyPlusTarget, bt: GhostStandardResTargets.SkyPlusTarget) => at ~= bt
        case _ => false
      })
    }

  implicit val GhostAsterismHighResolutionAlmostEqual: AlmostEqual[GhostAsterism.HighResolution] =
    new AlmostEqual[GhostAsterism.HighResolution] {
      override def almostEqual(a: HighResolution, b: HighResolution): Boolean =
        (a.ghostTarget ~= b.ghostTarget) && (a.sky ~= b.sky) && (a.base ~= b.base)
    }

  implicit val GhostAsterismAlmostEqual: AlmostEqual[GhostAsterism] =
    new AlmostEqual[GhostAsterism] {
      override def almostEqual(a: GhostAsterism, b: GhostAsterism): Boolean = (a,b) match {
        case (a: GhostAsterism.StandardResolution, b: GhostAsterism.StandardResolution) => a ~= b
        case (a: GhostAsterism.HighResolution,     b: GhostAsterism.HighResolution)     => a ~= b
        case _ => false
      }
    }

  implicit val AsterismAlmostEqual: AlmostEqual[Asterism] =
    new AlmostEqual[Asterism] {
      def almostEqual(a: Asterism, b: Asterism): Boolean =
        (a, b) match {
          case (a: Asterism.Single, b: Asterism.Single) => a ~= b
          case (a: GhostAsterism,   b: GhostAsterism)   => a ~= b
          case _ => false
        }
    }

  def close[A: ParamSetCodec: Arbitrary: AlmostEqual](implicit mf: Manifest[A]) =
    mf.runtimeClass.getName ! forAll { (key: String, value: A) =>
      val c = ParamSetCodec[A]
      c.decode(c.encode(key, value)).map(_ ~= value) must_== \/-(true)
    }

  "Asterism ParamSet Codecs" >> {
    close[Asterism.Single]
    close[GhostAsterism.StandardResolution]
    close[GhostAsterism.HighResolution]
    close[GhostAsterism]
    close[Asterism]
  }
}