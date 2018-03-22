package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.Conflict._
import edu.gemini.pot.sp.version.{EmptyNodeVersions, VersionMap}
import edu.gemini.pot.sp._
import edu.gemini.sp.vcs2.MergeCorrection._
import edu.gemini.sp.vcs2.ObsEdit.{Obs, ObsUpdate, ObsDelete, ObsCreate}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.gemini.obscomp.SPProgram.Active
import edu.gemini.spModel.obs.{ObsPhase2Status, SPObservation}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.too.{Too, TooType}

import scalaz._
import Scalaz._

// To replace:
// * edu.gemini.util.security.policy.MergeValidator
// * edu.gemini.util.security.policy.ImplicitPolicy that deals with ObsMergePermission
// * edu.gemini.util.security.permission.ObsMergePermission

/**
 * Corrects the `MergePlan` to address any observation edits that are
 * inappropriate for the permissions held by the current user.
 */
class ObsPermissionCorrection(
        local: ISPProgram,
        nodeMap: Map[SPNodeKey, ISPNode],
        remoteDiffs: ProgramDiff) extends CorrectionAction {

  val lifespanId = local.getLifespanId

  import ObsPermissionCorrection._

  def apply(mp: MergePlan, hasPermission: PermissionCheck): VcsAction[MergePlan] = {

    def extractActive: TryVcs[Active] =
      remoteDiffs.plan.update.rootLabel match {
        case Modified(_, _, sp: SPProgram, _, _) => TryVcs(sp.getActive)
        case Unmodified(_)                       => safeActiveStatus(local)
      }

    def invalidEdits(mp: MergePlan, allEdits: List[ObsEdit], validator: ObsEditValidator): List[ObsEdit] = {
      val present = mp.update.keySet

      def survivedMerge(oe: ObsEdit): Boolean = oe match {
        case od: ObsDelete => !present.contains(od.key)
        case _             =>  present.contains(oe.key)
      }

      allEdits.filter(oe => !validator.isLegal(oe) && survivedMerge(oe))
    }

    def correctOne(mp: MergePlan, edit: ObsEdit): TryVcs[MergePlan] =
      edit match {
        case ObsCreate(k, _)                                            =>
          correctCreate(mp, k, new CreatePermissionFail(k))

        case ObsDelete(k, Obs(_, obs))                                  =>
          correctDelete(mp, k, obs)

        case ObsUpdate(k, _, None, _)                                   =>
          correctCreate(mp, k, new UpdatePermissionFail(k)) // same behavior needed

        case ObsUpdate(k, Obs(_, localObs), Some(Obs(_, remoteObs)), _) =>
          correctUpdate(mp, k, localObs, remoteObs)
      }

    def correctAll(mp: MergePlan, invalidEdits: List[ObsEdit]): TryVcs[MergePlan] =
      (mp.right[VcsFailure]/:invalidEdits) { (mp0,edit) => mp0.flatMap(correctOne(_, edit)) }

    for {
      edits     <- ObsEdit.all(local, remoteDiffs).liftVcs
      pid       <- safePid(local).liftVcs
      too       <- safeToo(local).liftVcs
      active    <- extractActive.liftVcs
      validator <- ObsEditValidator(pid, hasPermission, too, active)
      invalid    = invalidEdits(mp, edits, validator)
      corrected <- correctAll(mp, invalid).liftVcs
    } yield corrected
  }


  // Reset the status of the merged observation to Phase2.
  private def correctCreate(mp: MergePlan, k: SPNodeKey, cn: Conflict.Note): TryVcs[MergePlan] =
    for {
      l0 <- mp.update.focus(k)
      l1 <- resetStatus(l0)
      l2 <- l1.addConflictNote(cn)
    } yield mp.copy(update = l2.toTree)

  // Reinsert the deleted observation as it exists in the remote database.
  private def correctDelete(mp: MergePlan, k: SPNodeKey, remoteObs: Tree[MergeNode]): TryVcs[MergePlan] = {
    // Get all the ancestors of the deleted obs in the remote merge tree, going
    // from the obs up to the root.
    def ancestors: TryVcs[List[SPNodeKey]] =
      remoteDiffs.plan.update.loc.findNode(k).map {
        _.parents.map { case (_, p, _) => p.key }.toList
      }

    // Find the closest ancestor of the deleted node that still exists in the
    // merge plan.  At the very least this will be the root program node itself.
    def parent(as: List[SPNodeKey], root: TreeLoc[MergeNode]): TryVcs[TreeLoc[MergeNode]] =
      as match {
        case Nil     => TryVcs.fail("Could not find deleted obs insertion point")
        case f :: gf => root.find(_.key === f).fold(parent(gf, root)) { \/-(_) }
      }

    for {
      as   <- ancestors
      p0   <- parent(as, mp.update.loc)
      p1   <- p0.incr(lifespanId)
      obs0 <- restoreRemote(p1, remoteObs, None)
      obs1 <- obs0.addConflictNote(new DeletePermissionFail(k))
      resurrectedKeys = remoteObs.keySet
    } yield MergePlan(obs1.toTree, mp.delete.filterNot(m => resurrectedKeys.contains(m.key)))
  }

  private def correctUpdate(mp: MergePlan, k: SPNodeKey, localObs: Tree[MergeNode], remoteObs: Tree[MergeNode]): TryVcs[MergePlan] = {
    val maxLocal  = mp.update.foldObservations(none[Int]) { (_, i, _, n) => some(n.fold(i)(_ max i)) }
    val maxRemote = remoteDiffs.maxObsNumber
    val obsNumber = (0 :: maxLocal.toList ++ maxRemote.toList).max + 1

    for {
    // Find the observation that was updated and get its index
      l     <- mp.update.focus(k)
      i     <- l.childIndex

      // Delete the merged version and replace it with the version from the
      // remote database.
      p0    <- l.deleteNodeFocusParent.toTryVcs("Updated orphan observation")
      p1    <- p0.incr(lifespanId)
      robs  <- restoreRemote(p1, remoteObs, some(i))
      p2    <- robs.parent.toTryVcs("Replaced observation has no parent")

      // Add a conflict folder and place the local version of the obs there.
      // This observation will have the same number as the remote version so we
      // reset it with `obsNumber`.
      cf0   <- p2.getOrCreateConflictFolder(lifespanId, nodeMap)
      cf1   <- cf0.incr(lifespanId)
      lobs0 <- restoreLocal(cf1, localObs, remoteDiffs.plan.vm(local), obsNumber)
      lobs1 <- lobs0.addConflictNote(new UpdatePermissionFail(k))
      resurrectedKeys = robs.tree.keySet ++ lobs1.tree.keySet
    } yield MergePlan(lobs1.toTree, mp.delete.filterNot(m => resurrectedKeys.contains(m.key)))
  }

  private def resetStatus(obs: TreeLoc[MergeNode]): TryVcs[TreeLoc[MergeNode]] =
    (obs.getLabel match {
      case m@Modified(_, _, o: SPObservation, _, _) =>
        val o0 = o.copy <| (_.setPhase2Status(ObsPhase2Status.PI_TO_COMPLETE))
        TryVcs(m.copy(dob = o0))
      case mn =>
        TryVcs.fail(s"Expected a modified observation but got $mn")
    }).map { mod => obs.modifyLabel(_ => mod) }.flatMap(loc => loc.incr(lifespanId))

  // We're going to add the tree `localObs` to `parent` so it cannot contain any
  // keys that are currently in use in the program. We'll have to replace any
  // duplicate keys that we find with new nodes that have new keys.
  //
  // We update the version counter if we have created a new node or if one of
  // the children is updated.  Regardless, we sync the observation's node's
  // version vectors with the versions in the remote program's version map to
  // mark that all changes have been accounted for.
  private def restoreLocal(parent: TreeLoc[MergeNode], localObs: Tree[MergeNode], vm: VersionMap, obsNumber: Int): TryVcs[TreeLoc[MergeNode]] = {
    val dups = parent.root.toTree.keySet & localObs.keySet
    val upd  = localObs.cobind { t =>
      val isDuplicate = dups.contains(t.rootLabel.key)
      val childKeys   = t.subForest.map(_.key).toSet

      t.rootLabel match {
        case Modified(k, nv, dob, det, con) =>
          val hasDuplicate = (childKeys & dups).nonEmpty

          val (k0,nv0) = if (isDuplicate) (new SPNodeKey, EmptyNodeVersions.incr(lifespanId))
                         else (k, nv.sync(vm.getOrElse(k, EmptyNodeVersions)))

          val nv1      = if (hasDuplicate) nv0.incr(lifespanId) else nv0

          MergeNode.modified(k0, nv1, dob, det, con).right

        case un@Unmodified(k)          =>
          if (isDuplicate) TryVcs.fail(s"Cannot add duplicate unmodified node $k")
          else un.right
      }
    }.sequenceU

    upd.flatMap { obs =>
      for {
        obs0 <- obs.mModifyLabel { _.copy(detail = NodeDetail.Obs(obsNumber)) }
        obs1 <- resetStatus(parent.insertDownLast(obs0))
      } yield obs1
    }
  }

  // Restores a remote observation as it is in the remote program without any
  // local changes.  Because one or more sub-components could have been moved
  // out of the locally edited version of the observation, we must identify any
  // such components and replace them with new, freshly minted copies that have
  // new node keys and an initial node version map.
  private def restoreRemote(parent: TreeLoc[MergeNode], remoteObs: Tree[MergeNode], index: Option[Int]): TryVcs[TreeLoc[MergeNode]] = {
    def modifiedCopy(loc: TreeLoc[MergeNode]): TryVcs[TreeLoc[MergeNode]] =
      loc.mapAsModified(nodeMap) {
        case Modified(_, _, dob, detail, _) => Modified(new SPNodeKey, EmptyNodeVersions, dob, detail, Conflicts.EMPTY)
      }

    def replaceOne(tryRoot: TryVcs[TreeLoc[MergeNode]], dup: SPNodeKey): TryVcs[TreeLoc[MergeNode]] =
      for {
        loc <- tryRoot
        dn  <- loc.findNode(dup)
        rn0 <- modifiedCopy(dn)
        rn1 <- rn0.incr(lifespanId)
        p0  <- rn1.parent.toTryVcs(s"Missing parent: ${rn1.key}")
        p1  <- p0.incr(lifespanId)
      } yield p1.root

    def replaceAll(dups: Set[SPNodeKey]): TryVcs[TreeLoc[MergeNode]] =
      (TryVcs(parent.root)/:dups) { replaceOne }.flatMap { _.findNode(parent.key) }

    val dups = parent.root.tree.keySet & remoteObs.keySet
    val upd  = if (dups.isEmpty) TryVcs(parent) else replaceAll(dups)

    upd.flatMap { loc =>
      index.fold(TryVcs(loc.insertDownFirst(remoteObs))) { i =>
        loc.insertDownAt(i, remoteObs).toTryVcs(s"Couldn't restore remote observation ${remoteObs.key} at position $i")
      }
    }
  }

}

object ObsPermissionCorrection {
  def apply(mc: MergeContext): ObsPermissionCorrection =
    new ObsPermissionCorrection(mc.local.prog, mc.local.nodeMap, mc.remote.diff)

  private def safePid(p: ISPProgram): TryVcs[SPProgramID] =
    safeGet(p.getProgramID, "Program missing id.")

  private def safeToo(p: ISPProgram): TryVcs[TooType] =
    safeGet(Too.get(p), "Could not determine ToO type.")

  private def safeActiveStatus(p: ISPProgram): TryVcs[Active] =
    safeGet(p.getDataObject.asInstanceOf[SPProgram].getActive, "Could not determine whether program is active.")
}
