package edu.gemini.spModel.target.env

import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPTarget

import scalaz.NonEmptyList

/** A trait for common target collection operations across `GuideEnv` and the
  * various `GuideGrp`s.
  */
trait TargetCollection[Repr] {
  def cloneTargets(r: Repr): Repr
  def containsTarget(r: Repr, t: SPTarget): Boolean
  def removeTarget(r: Repr, t: SPTarget): Repr
  def targets(r: Repr): Map[GuideProbe, NonEmptyList[SPTarget]]
}

object TargetCollection {
  implicit class TargetCollectionSyntax[Repr](value: Repr) {
    def cloneTargets(implicit tc: TargetCollection[Repr]): Repr =
      tc.cloneTargets(value)

    def containsTarget(t: SPTarget)(implicit tc: TargetCollection[Repr]): Boolean =
    tc.containsTarget(value, t)

    def removeTarget(t: SPTarget)(implicit tc: TargetCollection[Repr]): Repr =
      tc.removeTarget(value, t)

    def targets(implicit tc: TargetCollection[Repr]): Map[GuideProbe, NonEmptyList[SPTarget]] =
      tc.targets(value)
  }
}