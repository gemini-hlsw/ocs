package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.sp.version.EmptyNodeVersions
import edu.gemini.sp.vcs2.MergeCorrection.CorrectionFunction
import edu.gemini.spModel.rich.pot.sp.SpNodeKeyEqual

import scalaz._
import Scalaz._

case class ObsResurrectionCorrection(mc: MergeContext) extends CorrectionFunction {

  val lifespanId = mc.local.prog.getLifespanId

  override def apply(mp: MergePlan): TryVcs[MergePlan] = {
    val obsKeys = mp.update.foldObservations(Set.empty[SPNodeKey]) { (mod, _, _, s) => s + mod.key }
    val remKeys = obsKeys.filter { k => mc.local.isDeleted(k) && mc.remote.isPresent(k) }
    val locKeys = obsKeys.filter { k => mc.local.isPresent(k) && mc.remote.isDeleted(k) }

    for {
      mp0 <- (mp.right[VcsFailure]/:remKeys)  { (mpx, k) => mpx.flatMap(completeRemote(mc, _, k)) }
      mp1 <- (mp0.right[VcsFailure]/:locKeys) { (mpx, k) => mpx.flatMap(completeLocal(mc, _, k))  }
    } yield mp1
  }

  def completeRemote(mc: MergeContext, mp: MergePlan, obsKey: SPNodeKey): TryVcs[MergePlan] =
    for {
      obs      <- mp.update.focus(obsKey)
      children <- mc.remote.get(obsKey).map(_.subForest).toTryVcs("Couldn't find resurrected remote observation")
      upd      <- complete(mc, mp, obs, children)
    } yield mp.copy(update = upd.toTree)

  def completeLocal(mc: MergeContext, mp: MergePlan, obsKey: SPNodeKey): TryVcs[MergePlan] =
    for {
      obs      <- mp.update.focus(obsKey)
      lobs     <- mc.local.get(obsKey).toTryVcs("Couldn't find resurrected local observation")
      children = MergeNode.modifiedTree(lobs).subForest
      upd      <- complete(mc, mp, obs, children)
    } yield mp.copy(update = upd.toTree)

  def complete(mc: MergeContext, mp: MergePlan, loc: TreeLoc[MergeNode], children: Stream[Tree[MergeNode]]): TryVcs[TreeLoc[MergeNode]] = {
    val usedKeys = mp.update.keySet

    val resurrectedChildren = loc.tree.subForest.map { child => child.key -> child }.toMap

    // Strip the node of all children.
    val loc2 = loc.modifyTree { t => Tree(t.rootLabel) }

    // Add children back, one at a time.  If it is a resurrected child, we can
    // add it straight away.  If it was deleted but not present anywhere else,
    // it can also be added.  Otherwise it was moved somewhere else in the tree
    // so we have to duplicate it.
    val loc3 = (children :\ \/.right[VcsFailure, TreeLoc[MergeNode]](loc2)) { (c,tl) =>
      tl.flatMap { l =>
        val origChildKey = c.key
        val c2 = resurrectedChildren.getOrElse(origChildKey, {
          if (!usedKeys(origChildKey)) Tree(c.rootLabel)
          else Tree(c.rootLabel match { // Duplicate child with a new node key.
            case m: Modified => m.copy(key = new SPNodeKey(), nv = EmptyNodeVersions.incr(lifespanId))
            case u           => u
          })
        })

        for {
          l0 <- if (origChildKey =/= c2.key) l.incr(lifespanId) else TryVcs(l)
          l1 <- complete(mc, mp, l0.insertDownFirst(c2), c.subForest)
        } yield l1
      }
    }

    loc3.flatMap(_.parent.toTryVcs("Replace node has no parent"))
  }

}
