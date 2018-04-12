package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.{ImOption, Option => GemOption}
import edu.gemini.spModel.core.{AlmostEqual, Target}
import edu.gemini.spModel.core.AlmostEqual.{AlmostEqualOps, AlmostEqualOption}
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.target.SPTarget

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

trait Almosts {

  implicit val AlmostEqualJavaDouble =
    new AlmostEqual[java.lang.Double] {
      def almostEqual(a: java.lang.Double, b: java.lang.Double): Boolean =
        (a - b).abs < 0.000001
    }

  implicit def AlmostEqualGemOption[A: AlmostEqual]: AlmostEqual[GemOption[A]] =
      new AlmostEqual[GemOption[A]] {
        def almostEqual(a: GemOption[A], b: GemOption[A]): Boolean =
          (a.isDefined === b.isDefined) &&
            (!(a.isDefined && b.isDefined) || (a.getValue ~= b.getValue))
      }

  implicit val AlmostEqualSPTarget =
    new AlmostEqual[SPTarget] {
      val Now: GemOption[java.lang.Long] = ImOption.apply[java.lang.Long](System.currentTimeMillis)

      def almostEqual(at: SPTarget, bt: SPTarget): Boolean = {
        (at.getName === bt.getName) &&
          (at.getRaDegrees(Now)  ~= bt.getRaDegrees(Now)) &&
          (at.getDecDegrees(Now) ~= bt.getDecDegrees(Now))
      }
    }

  implicit def AlmostEqualList[A: AlmostEqual]: AlmostEqual[List[A]] =
    new AlmostEqual[List[A]] {
      def almostEqual(as0: List[A], as1: List[A]): Boolean =
        (as0.length === as1.length) && as0.zip(as1).forall { case (a0, a1) => a0 ~= a1 }
    }

  implicit def AlmostEqualOneAndList[A: AlmostEqual]: AlmostEqual[OneAnd[List, A]] =
    AlmostEqual.by[List[A],OneAnd[List,A]](_.toList)

  implicit def AlmostEqualZipper[A: AlmostEqual]: AlmostEqual[Zipper[A]] =
    new AlmostEqual[Zipper[A]] {
      def almostEqual(a: Zipper[A], b: Zipper[A]): Boolean =
        (a.toList ~= b.toList) && a.index === b.index
    }

  implicit def AlmostEqualDisjunction[A: AlmostEqual, B: AlmostEqual]: AlmostEqual[\/[A,B]] =
    new AlmostEqual[\/[A, B]] {
      def almostEqual(a: \/[A, B], b: \/[A, B]): Boolean =
        (a, b) match {
          case (-\/(a0), -\/(b0)) => a0 ~= b0
          case (\/-(a0), \/-(b0)) => a0 ~= b0
          case _                  => false
        }
    }

  implicit def AlmostEqualOptsList[A: AlmostEqual]: AlmostEqual[OptsList[A]] =
    new AlmostEqual[OptsList[A]] {
      def almostEqual(a: OptsList[A], b: OptsList[A]): Boolean =
        a.toDisjunction ~= b.toDisjunction
    }

  implicit def AlmostEqualMap[A, B: AlmostEqual]: AlmostEqual[Map[A, B]] =
    new AlmostEqual[Map[A, B]] {
      def almostEqual(a: Map[A, B], b: Map[A, B]): Boolean = {
        (a.keySet == b.keySet) && a.keys.forall { k => a(k) ~= b(k) }
      }
    }

  implicit def AlmostEqualScalazMap[A: Order, B: AlmostEqual]: AlmostEqual[A ==>> B] =
    new AlmostEqual[A ==>> B] {
      def almostEqual(m0: A ==>> B, m1: A ==>> B): Boolean = {
        (m0.keySet == m1.keySet) && m0.intersectionWith(m1)(_ ~= _).all(identity)
      }
    }

  implicit val AlmostEqualGuideGrp: AlmostEqual[GuideGrp] =
    new AlmostEqual[GuideGrp] {
      import AutomaticGroup.{Active, Initial, Disabled}
      def almostEqual(a: GuideGrp, b: GuideGrp): Boolean =
        (a, b) match {
          case (ManualGroup(an, am), ManualGroup(bn, bm)) => (an === bn) && (am ~= bm)
          case (Active(am, apa), Active(bm, bpa))         => (am ~= bm) && (apa ~= bpa)
          case (Initial, Initial)                         => true
          case (Disabled, Disabled)                       => true
          case _                                          => false
        }
    }

  implicit val AlmostEqualAutomaticGroup: AlmostEqual[AutomaticGroup] =
    AlmostEqual.by[GuideGrp, AutomaticGroup](ag => ag: GuideGrp)

  implicit val AlmostEqualManualGroup: AlmostEqual[ManualGroup] =
    AlmostEqual.by[GuideGrp, ManualGroup](mg => mg: GuideGrp)

  implicit val AlmostEqualGuideGroup: AlmostEqual[GuideGroup] =
    AlmostEqual.by[GuideGrp, GuideGroup](_.grp)

  implicit val AlmostEqualGuideEnv: AlmostEqual[GuideEnv] =
    new AlmostEqual[GuideEnv] {
      def almostEqual(a: GuideEnv, b: GuideEnv): Boolean =
        (a.auto ~= b.auto) && (a.manual ~= b.manual)
    }

  implicit val AlmostEqualGuideEnvironment: AlmostEqual[GuideEnvironment] =
    AlmostEqual.by[GuideEnv, GuideEnvironment](_.guideEnv)


  implicit val SPTargetAlmostEqual: AlmostEqual[SPTarget] =
    AlmostEqual[Target].contramap(_.getTarget)

  implicit val UserTargetAlmostEqual: AlmostEqual[UserTarget] =
    new AlmostEqual[UserTarget] {
      override def almostEqual(a: UserTarget, b: UserTarget): Boolean =
        (a.`type` === b.`type`) && (a.target ~= b.target)
    }

  implicit val SingleAsterismAlmostEqual: AlmostEqual[Asterism.Single] =
    AlmostEqual[SPTarget].contramap[Asterism.Single](_.t)


  implicit val GhostTargetAlmostEqual: AlmostEqual[GhostAsterism.GhostTarget] =
    new AlmostEqual[GhostAsterism.GhostTarget] {
      override def almostEqual(a: GhostAsterism.GhostTarget, b: GhostAsterism.GhostTarget): Boolean =
        (a.spTarget ~= b.spTarget) && (a.explicitGuideFiberState === b.explicitGuideFiberState)
    }

  implicit val GhostSingleTargetAlmostEqual: AlmostEqual[GhostAsterism.SingleTarget] =
    new AlmostEqual[GhostAsterism.SingleTarget] {
      override def almostEqual(a: GhostAsterism.SingleTarget, b: GhostAsterism.SingleTarget): Boolean =
        (a.target ~= b.target) && (a.base ~= b.base)
    }

  implicit val GhostDualTargetAlmostEqual: AlmostEqual[GhostAsterism.DualTarget] =
    new AlmostEqual[GhostAsterism.DualTarget] {
      override def almostEqual(a: GhostAsterism.DualTarget, b: GhostAsterism.DualTarget): Boolean =
        (a.target1 ~= b.target1) && (a.target2 ~= b.target2) && (a.base ~= b.base)
    }

  implicit val GhostTargetPlusSkyAlmostEqual: AlmostEqual[GhostAsterism.TargetPlusSky] =
    new AlmostEqual[GhostAsterism.TargetPlusSky] {
      override def almostEqual(a: GhostAsterism.TargetPlusSky, b: GhostAsterism.TargetPlusSky): Boolean =
        (a.target ~= b.target) && (a.sky ~= b.sky) && (a.base ~= b.base)
    }
  implicit val GhostSkyPlusTargetAlmostEqual: AlmostEqual[GhostAsterism.SkyPlusTarget] =
    new AlmostEqual[GhostAsterism.SkyPlusTarget] {
      override def almostEqual(a: GhostAsterism.SkyPlusTarget, b: GhostAsterism.SkyPlusTarget): Boolean =
        (a.target ~= b.target) && (a.sky ~= b.sky) && (a.base ~= b.base)
    }

  implicit val GhostAsterismHighResolutionAlmostEqual: AlmostEqual[GhostAsterism.HighResolution] =
    new AlmostEqual[GhostAsterism.HighResolution] {
      override def almostEqual(a: GhostAsterism.HighResolution, b: GhostAsterism.HighResolution): Boolean =
        (a.ghostTarget ~= b.ghostTarget) && (a.sky ~= b.sky) && (a.base ~= b.base)
    }

  implicit val AsterismAlmostEqual: AlmostEqual[Asterism] =
    new AlmostEqual[Asterism] {
      def almostEqual(a: Asterism, b: Asterism): Boolean =
        (a, b) match {
          case (a: Asterism.Single,              b: Asterism.Single)              => a ~= b
          case (a: GhostAsterism.SingleTarget,   b: GhostAsterism.SingleTarget)   => a ~= b
          case (a: GhostAsterism.DualTarget,     b: GhostAsterism.DualTarget)     => a ~= b
          case (a: GhostAsterism.TargetPlusSky,  b: GhostAsterism.TargetPlusSky)  => a ~= b
          case (a: GhostAsterism.SkyPlusTarget,  b: GhostAsterism.SkyPlusTarget)  => a ~= b
          case (a: GhostAsterism.HighResolution, b: GhostAsterism.HighResolution) => a ~= b
          case _ => false
        }
    }

  implicit val AlmostEqualTargetEnvironment: AlmostEqual[TargetEnvironment] =
    new AlmostEqual[TargetEnvironment] {
      override def almostEqual(a: TargetEnvironment, b: TargetEnvironment): Boolean =
        (a.getAsterism ~= b.getAsterism) &&
          (a.getGuideEnvironment ~= b.getGuideEnvironment) &&
          (a.getUserTargets.asScala.toList ~= b.getUserTargets.asScala.toList)
    }
}
