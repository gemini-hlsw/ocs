package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{SPComponentType, ISPNode, SPNodeKey}
import edu.gemini.pot.sp.validator._
import edu.gemini.pot.sp.version.{EmptyNodeVersions, LifespanId}
import edu.gemini.spModel.conflict.ConflictFolder
import edu.gemini.spModel.rich.pot.sp._
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
  def apply(mp: MergePlan): TryCorrect[MergePlan] =
    toTypeTree(mp.update).flatMap { tt =>
      Validator.validate(tt) match {
        case Left(v)  => fix(mp, v).flatMap(apply)
        case Right(_) => mp.right
      }
    }

  private def toTypeTree(t: Tree[MergeNode]): TryCorrect[TypeTree] =
    t.rootLabel match {
      case Unmodified(k)          =>
        // We need the types of the immediate children of a node, even if not
        // modified.
        nodeMap.get(k).toTryCorrect(s"Could not find unmodified node: $k").map { n =>
          TypeTree(NodeType.forNode(n), Some(k), Nil)
        }

      case Modified(k, _, dob, _) =>
        NodeType.forComponentType(dob.getType).toTryCorrect(s"Unusable node type: ${dob.getType}").flatMap { nt =>
          t.subForest.traverseU(toTypeTree).map { cs =>
            TypeTree(nt, Some(k), cs.toList)
          }
        }
    }

  private def fix(mp: MergePlan, v: Violation): TryCorrect[MergePlan] = {
    def key(v: Violation): TryCorrect[SPNodeKey] =
      v match {
        case CardinalityViolation(nt, Some(k), _) => k.right

        case cv@CardinalityViolation(_, None, _)  =>
          Unmergeable(s"Cardinality violation with missing node key: $cv").left

        case DuplicateKeyViolation(k)             =>
          Unmergeable(s"Duplicate program node key found: $k").left
      }

    def zip(k: SPNodeKey): TryCorrect[TreeLoc[MergeNode]] =
      mp.update.loc.find(_.getLabel.key === k).toTryCorrect(s"Couldn't find node involved in validate constraint violation: $k")

    for {
      k <- key(v)
      z <- zip(k)
      f <- fix(mp, z)
    } yield f
  }

  // Moves the tree at the focus of the given zipper into a conflict folder of
  // the parent node.
  private def fix(mp: MergePlan, z: TreeLoc[MergeNode]): TryCorrect[MergePlan] = {

    def incr(z: TreeLoc[MergeNode]): TryCorrect[TreeLoc[MergeNode]] =
      z.getLabel match {
        case m: Modified => \/-(z.modifyLabel(_ => m.copy(nv = m.nv.incr(lifespanId))))
        case _           => -\/(Unmergeable("Could not increment version of unmodified node"))
      }

    def isConflictFolder(t: Tree[MergeNode]): Boolean =
      t.rootLabel match {
        case Modified(_, _, _: ConflictFolder, _) => true
        case Unmodified(k)                        => nodeMap.get(k).exists { n =>
          n.getDataObject.getType == SPComponentType.CONFLICT_FOLDER
        }
        case _                                    => false
      }

    // Guarantee that the focus refers to a Modified merge node, converting
    // an Unmodified node to Modified if necessary.
    def asModified(t: TreeLoc[MergeNode]): TreeLoc[MergeNode] =
      t.getLabel match {
        case m: Modified => t
        case _           => t.modifyTree { mn =>
          val spNode   = nodeMap(mn.key)
          val conflict = MergeNode.modified(spNode)
          conflict.node(spNode.children.map(c => MergeNode.unmodified(c).leaf): _*)
        }
      }

    def addConflictFolder(t: TreeLoc[MergeNode]): TreeLoc[MergeNode] = {
      val k   = new SPNodeKey()
      val nv  = EmptyNodeVersions
      val dob = new ConflictFolder()
      val mn  = MergeNode.modified(k, nv, dob, NodeDetail.Empty)
      t.insertDownFirst(mn.leaf)
    }

    for {
      p0  <- z.deleteNodeFocusParent.toTryCorrect("Validity constraint violation for root node")
      p1  <- incr(p0)
      cf0  = p1.findChild(isConflictFolder).fold(addConflictFolder(p1))(asModified)
      cf1 <- incr(cf0)
    } yield mp.copy(update = cf1.insertDownLast(z.tree).toTree)
  }
}

object ValidityCorrection {
  def apply(mc: MergeContext): ValidityCorrection =
    new ValidityCorrection(mc.local.prog.getLifespanId, mc.local.nodeMap)
}
