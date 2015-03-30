package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPNode, SPComponentType, SPNodeKey}
import edu.gemini.pot.sp.version._
import edu.gemini.sp.vcs.diff.VcsFailure.Unmergeable
import edu.gemini.spModel.conflict.ConflictFolder
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._

class MergeZipper(lifespanId: LifespanId, nodeMap: Map[SPNodeKey, ISPNode]) {

  def focus(t: Tree[MergeNode], k: SPNodeKey): TryVcs[TreeLoc[MergeNode]] =
    t.loc.find(_.getLabel.key === k).toTryVcs(s"Couldn't find program node $k")

  def incr(z: TreeLoc[MergeNode]): TryVcs[TreeLoc[MergeNode]] =
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

  def conflictFolder(t: TreeLoc[MergeNode]): TreeLoc[MergeNode] =
    t.findChild(isConflictFolder).fold(addConflictFolder(t))(asModified)

}
