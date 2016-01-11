package edu.gemini.spModel.target.env

import edu.gemini.spModel.target.SPTarget

/** A trait for common target collection operations across `GuideEnv` and the
  * various `GuideGrp`s.
  */
trait TargetCollection[Repr] {
  def cloneTargets(r: Repr): Repr
  def containsTarget(r: Repr, t: SPTarget): Boolean
  def removeTarget(r: Repr, t: SPTarget): Repr
}

object TargetCollection {
  implicit class TargetCollectionSyntax[Repr](value: Repr) {
    def cloneTargets(implicit tc: TargetCollection[Repr]): Repr =
      tc.cloneTargets(value)

    def containsTarget(t: SPTarget)(implicit tc: TargetCollection[Repr]): Boolean =
    tc.containsTarget(value, t)

    def removeTarget(t: SPTarget)(implicit tc: TargetCollection[Repr]): Repr =
      tc.removeTarget(value, t)
  }
}