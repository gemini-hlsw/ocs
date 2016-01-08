package edu.gemini.spModel.target.env

import edu.gemini.spModel.target.SPTarget

/** A trait for common target collection operations across `GuideEnv` and the
  * various `GuideGrp`s.
  */
trait TargetCollection[Repr] {
  def removeTarget(r: Repr, t: SPTarget): Repr
  // def cloneTargets(implicit tc: TargetCollection[Repr]): Repr
  // def containsTarget(r: Repr, t: SPTarget): Repr
  // .. etc ..
}

object TargetCollection {
  implicit class TargetCollectionSyntax[Repr](value: Repr) {
    def removeTarget(t: SPTarget)(implicit tc: TargetCollection[Repr]): Repr =
      tc.removeTarget(value, t)
  }
}