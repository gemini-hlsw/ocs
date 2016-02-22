package edu.gemini.spModel.target.env

import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPTarget

import scalaz._, Scalaz._ //{@>, NonEmptyList, ==>>}

/** A trait for common target collection operations across `GuideEnv` and the
  * various `GuideGrp`s.
  */
trait TargetCollection[Repr] {
  def cloneTargets(r: Repr): Repr
  def containsTarget(r: Repr, t: SPTarget): Boolean
  def removeTarget(r: Repr, t: SPTarget): Repr
  def targets(r: Repr): GuideProbe ==>> NonEmptyList[SPTarget]
}

object TargetCollection {
  implicit class TargetCollectionSyntax[Repr](value: Repr) {
    def cloneTargets(implicit tc: TargetCollection[Repr]): Repr =
      tc.cloneTargets(value)

    def containsTarget(t: SPTarget)(implicit tc: TargetCollection[Repr]): Boolean =
      tc.containsTarget(value, t)

    def removeTarget(t: SPTarget)(implicit tc: TargetCollection[Repr]): Repr =
      tc.removeTarget(value, t)

    def targets(implicit tc: TargetCollection[Repr]): GuideProbe ==>> NonEmptyList[SPTarget] =
      tc.targets(value)

    /** Returns a List of all SPTargets in this collection, with duplicates
      * if the same target appears in more than one nested collection.
      */
    def targetList(implicit tc: TargetCollection[Repr]): List[SPTarget] =
      targets.foldrWithKey(List.empty[SPTarget]) { (_, nel, lst) =>
        nel.toList ++ lst
      }
  }

  def wrapping[A, B: TargetCollection](l: A @> B): TargetCollection[A] =
    new TargetCollection[A] {
      override def cloneTargets(a: A): A =
        l.mod(_.cloneTargets, a)

      override def containsTarget(a: A, t: SPTarget): Boolean =
        l.get(a).containsTarget(t)

      override def removeTarget(a: A, t: SPTarget): A =
        l.mod(_.removeTarget(t), a)

      override def targets(a: A): GuideProbe ==>> NonEmptyList[SPTarget] =
        l.get(a).targets
    }
}