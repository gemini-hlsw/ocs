package edu.gemini.sp.vcs.diff

import edu.gemini.sp.vcs.diff.VcsFailure.Unmergeable

import scalaz._
import Scalaz._

object MergeCorrection {

  type TryCorrect[A] = Unmergeable \/ A

  implicit class OptionOps[A](o: Option[A]) {
    def toTryCorrect(msg: => String): TryCorrect[A] =
      o.toRightDisjunction(Unmergeable(msg))
  }

  /**
   * A `CorrectionFunction` is just a function that modifies an `MergePlan` to
   * correct some aspect of the merge.
   */
  type CorrectionFunction = MergePlan => TryCorrect[MergePlan]

  def all(mc: MergeContext): List[CorrectionFunction] =
    List(
      ObsNumberCorrection(mc),
      ValidityCorrection(mc)
    )

  def apply(mc: MergeContext): CorrectionFunction = (mp: MergePlan) =>
    (mp.right[Unmergeable]/:all(mc)) { _.flatMap(_) }
}
