package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPNode, SPNodeKey}
import edu.gemini.pot.sp.validator._
import edu.gemini.pot.sp.version.LifespanId
import edu.gemini.sp.vcs.diff.MergeCorrection._
import edu.gemini.sp.vcs.diff.VcsFailure.Unmergeable

import scalaz._
import Scalaz._

/** Corrects merge plans that form invalid science programs by introducing
  * conflict nodes. */
class ValidityCorrection(lifespanId: LifespanId, nodeMap: Map[SPNodeKey, ISPNode]) extends CorrectionFunction {

  // Applies corrections to invalid science programs, shunting problem nodes off
  // into a conflict folder.  The Validator finds one issue at a time so we
  // find and fix an issue and then start over with the updated MergePlan until
  // there are no more issues.
  def apply(mp: MergePlan): TryVcs[MergePlan] =
    toTypeTree(mp.update).flatMap { tt =>
      Validator.validate(tt) match {
        case Left(v)  => correct(mp, v).flatMap(apply)
        case Right(_) => mp.right
      }
    }

  private def toTypeTree(t: Tree[MergeNode]): TryVcs[TypeTree] =
    t.rootLabel match {
      case Unmodified(k)          =>
        // We need the types of the immediate children of a node, even if not
        // modified.
        nodeMap.get(k).toTryVcs(s"Could not find unmodified node: $k").map { n =>
          TypeTree(NodeType.forNode(n), Some(k), Nil)
        }

      case Modified(k, _, dob, _) =>
        NodeType.forComponentType(dob.getType).toTryVcs(s"Unusable node type: ${dob.getType}").flatMap { nt =>
          t.subForest.traverseU(toTypeTree).map { cs =>
            TypeTree(nt, Some(k), cs.toList)
          }
        }
    }

  // Moves the sub-tree rooted at the key associated with the violation into a
  // conflict folder of the parent node.
  private def correct(mp: MergePlan, v: Violation): TryVcs[MergePlan] = {
    def key(v: Violation): TryVcs[SPNodeKey] =
      v match {
        case CardinalityViolation(nt, Some(k), _) => k.right

        case cv@CardinalityViolation(_, None, _)  =>
          Unmergeable(s"Cardinality violation with missing node key: $cv").left

        case DuplicateKeyViolation(k)             =>
          Unmergeable(s"Duplicate program node key found: $k").left
      }

    val z = new MergeZipper(lifespanId, nodeMap)

    for {
      k   <- key(v)
      l   <- z.focus(mp.update, k)
      p0  <- l.deleteNodeFocusParent.toTryVcs("Validity constraint violation for root node")
      p1  <- z.incr(p0)
      cf0  = z.conflictFolder(p1)
      cf1 <- z.incr(cf0)
    } yield mp.copy(update = cf1.insertDownLast(l.tree).toTree)
  }

}

object ValidityCorrection {
  def apply(mc: MergeContext): ValidityCorrection =
    new ValidityCorrection(mc.local.prog.getLifespanId, mc.local.nodeMap)
}
