package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.memImpl.MemProgram
import edu.gemini.spModel.conflict.ConflictFolder
import edu.gemini.spModel.obs.ObservationStatus

import java.security.Permission

import edu.gemini.pot.sp.Conflict.Moved
import edu.gemini.pot.sp.Instrument
import edu.gemini.pot.sp.validator.Validator
import edu.gemini.pot.sp.version.VersionMap
import edu.gemini.pot.sp.{DataObjectBlob => DOB, _}
import edu.gemini.shared.util.IntegerIsIntegral._
import edu.gemini.shared.util.VersionComparison
import edu.gemini.shared.util.VersionComparison.{Conflicting, Same, Newer}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.sp.vcs2.NodeDetail.Obs
import edu.gemini.sp.vcs2.ObsEdit.{ObsUpdate, ObsDelete, ObsCreate}
import edu.gemini.sp.vcs2.VcsFailure.VcsException
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.template.TemplateGroup

import org.junit.Test
import org.scalatest.junit.JUnitSuite

import scala.collection.JavaConverters._

import scalaz._
import scalaz.syntax.functor._  // idea marks as unused but required for "as"

class MergeTest extends JUnitSuite {
  import edu.gemini.sp.vcs2.MergePropertyTest.NamedProperty

  private def keys(l: List[ISPNode]): Set[SPNodeKey] = l.map(_.key).toSet

  case class EditFacts(p: ISPProgram, startMap: Map[SPNodeKey, ISPNode]) {
    val nodeMap     = p.nodeMap
    val id          = p.getLifespanId
    val editedNodes = p.fold(List.empty[ISPNode]) { (lst, n) =>
      if (n.getVersion.clocks.contains(id)) n :: lst else lst
    }
    val editedKeys  = keys(editedNodes)

    val editedObservationKeys = (Set.empty[SPNodeKey]/:editedNodes) { (s,n) =>
        val o = (n +: n.ancestors).find {
          case o: ISPObservation => true
          case _                 => false
        }
        o.fold(s) { s + _.key }
    }

    def isEdited(n: ISPNode): Boolean = editedKeys.contains(n.key)

    def isParentEdited(n: ISPNode): Boolean = Option(n.getParent).exists(isEdited)

    def containsRemotelyEditedNode(n: ISPNode): Boolean =
      isEdited(n) || n.children.exists(containsRemotelyEditedNode)


    val dataObjectEditedNodes = editedNodes.filter { n =>
      startMap.get(n.key).exists { s => !DOB.same(s.getDataObject, n.getDataObject) }
    }

    val childrenEditedNodes = editedNodes.filter { n =>
      startMap.get(n.key).exists { s =>
        s.children.map(_.key).equals(n.children.map(_.key))
      }
    }

    val newNodes    = editedNodes.filterNot(n => startMap.contains(n.key))
    val newKeys     = keys(newNodes)
    val deletedKeys = p.getVersions.keySet &~ nodeMap.keySet
  }

  case class PropContext(fact: ISPFactory, sp: ISPProgram, lp: ISPProgram, rp: ISPProgram, diffs: ProgramDiff, tryMergePlan: TryVcs[MergePlan]) {
    val mergePlan = tryMergePlan.fold(f => sys.error(VcsFailure.explain(f, sp.getProgramID, "", None)), identity)
    val startMap  = sp.nodeMap
    val startId   = sp.getLifespanId

    val local  = new EditFacts(lp, startMap)
    val remote = new EditFacts(rp, startMap)

    val mergeContext = MergeContext(lp, diffs)

    def compare(k: SPNodeKey): VersionComparison =
      mergeContext.local.version(k).compare(mergeContext.remote.version(k))

    val mergeMap     = mergePlan.update.sFoldRight(Map.empty[SPNodeKey, MergeNode]) { (mn, m) =>
      m + (mn.key -> mn)
    }

    val modifiedKeys = mergePlan.update.flatten.collect {
      case Modified(k, _, _, _, _) => k
    }.toSet

    val unmodifiedKeys = mergePlan.update.flatten.collect {
      case Unmodified(k) => k
    }.toSet

    val deletedKeys = mergePlan.delete.map(_.key)

    // Find all the merge plan node keys with a particular type of conflict.
    // Note, the corrected merge plan may have different/fewer conflicts since
    // obs permission correction will reset inappropriately edited observations.
    def mergePlanConflictKeys(p: (Conflicts, SPNodeKey) => Boolean ): Set[SPNodeKey] =
      mergePlan.update.sFoldRight(Set.empty[SPNodeKey]) { (mn, s) =>
        mn match {
          case Modified(k, _, _, _, con) if p(con, k) => s + k
          case _                                      => s
        }
      }

    val obsEditsTry = ObsEdit.all(lp, diffs)
    val obsEdits    = obsEditsTry match {
      case \/-(edits) => edits
      case -\/(f)     => sys.error(s"could not extract edits: ${VcsFailure.explain(f, sp.getProgramID, "", None)}")
    }

    // WARNING: every time you run this action, you'll get distinct conflict
    // folder keys.  In particular keep that in mind when comparing with
    // updatedLocalProgram which when run will run correctedMergePlan
    val correctedMergePlan: VcsAction[MergePlan] =
      MergeCorrection(mergeContext)(mergePlan, (_: Permission) => VcsAction(true))

    val updatedLocalProgram: VcsAction[ISPProgram] = {
      val localCopy = fact.copyWithSameKeys(lp)
      for {
        mp <- correctedMergePlan
        _  <- mp.merge(fact, localCopy)
      } yield localCopy
    }
  }

  def mpKeys(mp: MergePlan): Set[SPNodeKey] = {
    val modKeys = mp.update.foldRight(Set.empty[SPNodeKey]) { (mn, s) => s + mn.key }
    val delKeys = mp.delete.map(_.key)
    modKeys ++ delKeys
  }

  def emptySet(pc: PropContext, s: Set[_]): Boolean = {
    if (s.nonEmpty) {
      Console.err.println(MergeNode.draw(pc.mergePlan.update))
      Console.err.println("# " + s.mkString("{", ", ", "}"))
    }
    s.isEmpty
  }

  def keysMatch(n1: String, ks1: Iterable[SPNodeKey], n2: String, ks2: Iterable[SPNodeKey]): Boolean = {
    val result = ks1.toSet == ks2.toSet
    if (!result) {
      Console.err.println("Keys don't match:")
      Console.err.println(f"\t$n1%20s: ${ks1.toList.sorted.mkString(", ")}")
      Console.err.println(f"\t$n2%20s: ${ks2.toList.sorted.mkString(", ")}")
    }
    result
  }

  val props = List[NamedProperty[PropContext]] (
    ("all diffs are accounted for in the MergePlan",
      (start, local, remote, pc) => {
        val diffKeys  = mpKeys(pc.diffs.plan)
        val mergeKeys = mpKeys(pc.mergePlan)

        val result = (diffKeys & mergeKeys) == diffKeys

        if (!result) {
          Console.err.println(MergeNode.draw(pc.mergePlan.update))
          Console.err.println("# diff - merge = " + (diffKeys &~ mergeKeys).mkString("{", ", ", "}"))
          Console.err.println("# merge - diff = " + (mergeKeys &~ diffKeys).mkString("{", ", ", "}"))
          val badKeys = (diffKeys &~ mergeKeys) ++ (mergeKeys &~ diffKeys)
          badKeys.foreach { k =>
            Console.err.println("# " + k)
            Console.err.println("\tin diff deleted?  " + pc.diffs.plan.delete.exists(m => m.key == k))
            Console.err.println("\tin diff update?   " + pc.diffs.plan.update.flatten.exists(mn => mn.key == k))
            Console.err.println("\tin merge deleted? " + pc.mergePlan.delete.exists(m => m.key == k))
            Console.err.println("\tin merge update?  " + pc.mergePlan.update.flatten.exists(mn => mn.key == k))
          }
        }

        result
      }
    ),

    ("no nodes are duplicated in the update tree",
      (start, local, remote, pc) => {
        val emptyCounts = Map.empty[SPNodeKey, Int].withDefaultValue(0)

        val keyCounts = pc.mergePlan.update.sFoldRight(emptyCounts) { (mn,m) =>
          val count = m(mn.key) + 1
          m.updated(mn.key, count)
        }

        val dups = keyCounts.filter { case (_,v) => v != 1 }
        if (dups.nonEmpty) {
          Console.err.println(MergeNode.draw(pc.mergePlan.update))
          Console.err.println("# Duplicates")
          dups.foreach(k => Console.err.println("\t" + k))
        }

        dups.isEmpty
      }
    ),

    ("any node present in both edited versions must be present in the merged result",
      (start, local, remote, pc) => {
        val commonKeys = pc.local.nodeMap.keySet & pc.diffs.plan.update.sFoldRight(Set.empty[SPNodeKey]) { (mn, s) => s + mn.key }
        val mergeKeys  = pc.mergePlan.update.flatten.map(_.key).toSet
        val result = (commonKeys & mergeKeys) == commonKeys

        if (!result) {
          Console.err.println(MergeNode.draw(pc.mergePlan.update))
          Console.err.println("# common - merge = " + (commonKeys &~ mergeKeys).mkString("{", ", ", "}"))
          val badKeys = (commonKeys &~ mergeKeys) ++ (mergeKeys &~ commonKeys)
          badKeys.foreach { k =>
            Console.err.println("# " + k)
            Console.err.println("\tin diff deleted?  " + pc.diffs.plan.delete.exists(m => m.key == k))
            Console.err.println("\tin diff update?   " + pc.diffs.plan.update.flatten.exists(mn => mn.key == k))
            Console.err.println("\tin local nodeMap? " + pc.local.nodeMap.contains(k))
            Console.err.println("\tin merge deleted? " + pc.mergePlan.delete.exists(m => m.key == k))
            Console.err.println("\tin merge update?  " + pc.mergePlan.update.flatten.exists(mn => mn.key == k))
          }
        }

        result
      }
    ),

    ("any node missing in both edited versions must be missing in the merged result",
      (start, local, remote, pc) => {
        val commonKeys = pc.local.deletedKeys & pc.diffs.plan.delete.map(_.key)
        emptySet(pc, commonKeys &~ pc.deletedKeys)
      }
    ),

    ("a node that is newer and still present in the local program must appear in merge tree",
      (start, local, remote, pc) => emptySet(pc, pc.local.editedKeys &~ pc.mergeMap.keySet)
    ),

    ("a node that is older and still present in the remote program must appear in the merge tree",
      (start, local, remote, pc) => emptySet(pc, pc.remote.editedKeys &~ pc.mergeMap.keySet)
    ),

    ("a locally deleted but remotely edited node must be present in the merged program",
      (start, local, remote, pc) => {
        val delLocalModRemote = pc.local.deletedKeys & pc.remote.editedKeys
        emptySet(pc, delLocalModRemote &~ pc.mergeMap.keySet)
      }
    ),

    ("a remotely deleted but locally edited node must be present in the merged program",
      (start, local, remote, pc) => {
        val delRemoteModLocal = pc.remote.deletedKeys & pc.local.editedKeys
        emptySet(pc, delRemoteModLocal &~ pc.mergeMap.keySet)
      }
    ),

    ("resurrected observations must be brought back to life in full with all their descendants",
      (start, local, remote, pc) => {
        val delRemoteModLocal = pc.remote.deletedKeys & pc.local.editedKeys
        val delLocalModRemote = pc.local.deletedKeys & pc.remote.editedKeys

        def matches(merged: Tree[MergeNode], n: ISPNode): Boolean = {
          val dobMatches = merged.rootLabel match {
            case Modified(_,_,dob,_,_) =>
              // Usually the data objects are the same.  If a child is moved
              // locally and then the parent is deleted locally and yet edited
              // remotely though, the edited child is recalled in the
              // resurrected observation.  We'll just make sure that the same
              // type of data object is found and not that they are identical.
              dob.getClass.getName == n.getDataObject.getClass.getName

            case Unmodified(k)         =>
              k == n.key
          }

          dobMatches && {
            val mChildren = merged.subForest
            val nChildren = n.children
            mChildren.size == nChildren.size && mChildren.zip(nChildren).forall { case (m, n0) => matches(m, n0) }
          }
        }

        def check(resurrectedNodes: Set[SPNodeKey], lookup: SPNodeKey => ISPNode): Boolean =
          resurrectedNodes.forall { k =>
            lookup(k) match {
              case o: ISPObservation =>
                val cmp = pc.correctedMergePlan.run.unsafePerformSync.getOrElse(sys.error("Couldn't run merge plan"))
                cmp.update.focus(k).exists(loc => matches(loc.tree, o))
              case _                 => true
            }
          }

        check(delRemoteModLocal, pc.local.nodeMap) && check(delLocalModRemote, pc.remote.nodeMap)
      }
    ),

    ("a locally resurrected node not inside a remotely edited parent must contain a node that is remotely edited",
      (start, local, remote, pc) => {
        val localDeletedRemotePresent = pc.local.deletedKeys & pc.remote.nodeMap.keySet
        val inEditedRemoteParent = localDeletedRemotePresent.filter { k =>
          pc.remote.isParentEdited(pc.remote.nodeMap(k))
        }

        val locallyResurrected = (pc.local.deletedKeys & pc.mergeMap.keySet) &~ inEditedRemoteParent
        emptySet(pc, locallyResurrected.map(pc.remote.nodeMap.apply).filterNot(pc.remote.containsRemotelyEditedNode))
      }
    ),

    ("a locally deleted node whose remote version contains no updates must stay deleted",
      (start, local, remote, pc) => {
        val localDeletions = pc.local.deletedKeys & pc.remote.nodeMap.keySet

        val mergeDeletions = localDeletions.filterNot { k =>
          val r = pc.remote.nodeMap(k)
          pc.remote.isParentEdited(r) || pc.remote.containsRemotelyEditedNode(r)
        }

        emptySet(pc, mergeDeletions &~ pc.mergePlan.delete.map(_.key))
      }
    ),

    ("a remotely deleted node whose local version contains no updates must stay deleted",
      (start, local, remote, pc) => {
        val remoteDeletions = pc.remote.deletedKeys & pc.local.nodeMap.keySet

        val mergeDeletions = remoteDeletions.filterNot { k =>
          val r = pc.local.nodeMap(k)
          pc.local.isParentEdited(r) || pc.local.containsRemotelyEditedNode(r)
        }

        emptySet(pc, mergeDeletions &~ pc.mergePlan.delete.map(_.key))
      }
    ),

    ("for any strictly newer local node, the matching Modified merge node data object must be the same",
      (start, local, remote, pc) =>
        pc.local.dataObjectEditedNodes.filter(n => pc.compare(n.key) == Newer).forall { n =>
          pc.mergeMap.get(n.key).exists {
            case Modified(_, _, dob, _, _) => DOB.same(n.getDataObject, dob)
            case Unmodified(_)             => false
          }
        }
    ),

    ("for any edited remote node, the matching Modified merge node data object must be the same",
      (start, local, remote, pc) =>
        pc.remote.dataObjectEditedNodes.filterNot(n => pc.compare(n.key) == Newer).forall { n =>
          pc.mergeMap.get(n.key).exists {
            case Modified(_, _, dob, _, _) => DOB.same(n.getDataObject, dob)
            case Unmodified(_)             => false
          }
        }
    ),

    ("all unmodified nodes in the merge tree correspond to existing local program nodes",
      (start, local, remote, pc) => emptySet(pc, pc.unmodifiedKeys &~ pc.local.nodeMap.keySet)
    ),

    ("no unmodified node contains a modified node",
      (start, local, remote, pc) => {
        val unmodifiedNodes = pc.unmodifiedKeys.toList.map(pc.local.nodeMap.apply)

        // all keys corresponding to Unmodified merge nodes or their descendants
        // in the local program
        val allUnmodified   = (Set.empty[SPNodeKey]/:unmodifiedNodes) { _ ++ _.keySet }

        emptySet(pc, allUnmodified & pc.modifiedKeys)
      }
    ),

    ("no duplicates after renumbering observations",
      (start, local, remote, pc) => {
        val empty = Map.empty[Int, SPNodeKey]
        val (localOnly, remoteOnly) = pc.mergePlan.update.foldObservations((empty, empty)) { case (m, i, _, (lo, ro)) =>
          val key = m.key
          (pc.local.p.getVersions.contains(key), pc.remote.p.getVersions.contains(key)) match {
            case (true, false) => (lo + (i -> key), ro)
            case (false, true) => (lo, ro + (i -> key))
            case _             => (lo, ro)
          }
        }

        localOnly.isEmpty || remoteOnly.isEmpty || {
          val localKeys = localOnly.values.toSet
          val remoteMax = remoteOnly.keySet.max

          ObsNumberCorrection(pc.mergeContext).apply(pc.mergePlan) match {
            case -\/(f) =>
              Console.err.println(VcsFailure.explain(f, start.getProgramID, "", None))
              false

            case \/-(mp) =>
              val t        = mp.update
              val emptyMap = Map.empty[Int, Set[SPNodeKey]].withDefaultValue(Set.empty[SPNodeKey])
              val obsMap   = t.foldRight(emptyMap) {
                case (Modified(k, _, _, Obs(n), _), m) => m.updated(n, m(n) + k)
                case (_, m)                            => m
              }

              val renumberedKeys = (Set.empty[SPNodeKey]/:((remoteMax + 1) to remoteMax + localKeys.size)) { (s, i) =>
                s ++ obsMap(i)
              }

              obsMap.values.forall(_.size == 1) && (localKeys == renumberedKeys)
          }
        }
      }
    ),

    ("merged program matches merge plan",
      (start, local, remote, pc) => {
        def dataObjectMatches(nDob: ISPDataObject, tDob: ISPDataObject): Boolean = {
          val same = DOB.same(nDob, tDob)
          if (!same) {
            Console.err.println("Data objects don't match: " + nDob.getType + ", " + tDob.getType)
          }
          same
        }

        def conflictsMatch(nConflicts: Conflicts, tConflicts: Conflicts): Boolean =
          ((nConflicts.dataObjectConflict.asScalaOpt, tConflicts.dataObjectConflict.asScalaOpt) match {
            case (None, None)       => true
            case (Some(n), Some(t)) => (n.perspective == t.perspective) &&
                                       dataObjectMatches(n.dataObject, t.dataObject)
            case _                  => false
          }) && nConflicts.notes.asScalaList.toSet == tConflicts.notes.asScalaList.toSet

        def childrenMatch(n: List[ISPNode], t: Stream[Tree[MergeNode]]): Boolean = {
          // The order won't be the same because the Tree[MergeNode] doesn't
          // worry about ordering amongst different types of objects (e.g.,
          // obs components before the obs log before the sequence node).
          val nMap = n.map(c => c.key -> c).toMap
          val tMap = t.map(c => c.key -> c).toMap

          val sameKeys = nMap.keySet == tMap.keySet
          if (!sameKeys) {
            Console.err.println("Children don't match: ")
            Console.err.println("\tn.children = " + n.map(_.key).mkString(", "))
            Console.err.println("\tt.children = " + t.map(_.key).mkString(", "))
          }

          sameKeys && nMap.values.forall { nc => matchesMergePlan(nc, tMap(nc.key)) }
        }

        def obsNumSame(n: ISPNode, det: NodeDetail): Boolean =
          (n, det) match {
            case (o: ISPObservation, Obs(num)) => o.getObservationNumber == num
            case (o: ISPObservation, _       ) => false
            case (_,                 Obs(num)) => false
            case _                             => true
          }

        def matchesMergePlan(n: ISPNode, t: Tree[MergeNode]): Boolean =
          t.rootLabel match {
            case Modified(k, _, dob, det, con) =>
              lazy val dobSame      = dataObjectMatches(n.getDataObject, dob)
              lazy val conSame      = conflictsMatch(n.getConflicts, con)
              lazy val childrenSame = childrenMatch(n.children, t.subForest)
              k == n.key && dobSame && conSame && childrenSame && obsNumSame(n, det)
            case Unmodified(k)          =>
              k == n.key
          }

        // This is like pc.updatedLocalProgram but it doesn't rerun the
        // correctedMergePlan action.  If you rerun the action, you will get
        // distinct keys for any conflict folders.
        def updatedLocalProgram(corrections: MergePlan): VcsAction[ISPProgram] = {
          val localCopy = pc.fact.copyWithSameKeys(pc.lp)
          corrections.merge(pc.fact, localCopy).as(localCopy)
        }

        val result = for {
          cmp <- pc.correctedMergePlan
          ulp <- updatedLocalProgram(cmp)
        } yield {
          val matches = matchesMergePlan(ulp, cmp.update)
          if (!matches) {
            Console.err.println("Merge Plan")
            Console.err.println(MergeNode.draw(cmp.update))
            Console.err.println("Merged Program")
            Console.err.println(drawNodeTree(ulp))
          }
          matches
        }

        import VcsAction._

        result.unsafeRun match {
          case -\/(failure)          =>
            Console.err.println(failure)
            false

          case \/-(matches)          =>
            matches
        }
      }
    ),

    ("merged program version map is newer or equal to the remote program",
      (start, local, remote, pc) => {
        // Note, because the ObsPermissionCorrection will *reset* a locally
        // updated observation that the user does not have permission to edit,
        // the merge plan might not be strictly newer than the local program.

        def isSameOrNewer(x: VersionMap, y: VersionMap): Boolean =
          VersionMap.compare(x, y) match {
            case Same | Newer => true
            case _            => false
          }

        pc.updatedLocalProgram.exists { ulp =>
          val updateVm = ulp.getVersions
          val remoteVm = pc.rp.getVersions
          isSameOrNewer(updateVm, remoteVm)
        }.unsafePerformSync
      }
    ),

    ("merged program passes validity checks",
      (start, local, remote, pc) => {
        import VcsAction._
        pc.updatedLocalProgram.unsafeRun match {
          case -\/(VcsException(ex)) =>
            Console.err.println("Unexpected error creating merged program")
            ex.printStackTrace()
            false

          case -\/(f) =>
            Console.err.println(s"Could not merge programs: $f")
            false

          case \/-(p) =>
            Validator.validate(p) match {
              case Left(v) =>
                Console.err.println(s"Merged program not valid: $v")
                false
              case Right(_) =>
                true
            }
        }
      }
    ),

    ("ObsEdit produces entries for all new local observations",
      (start, local, remote, pc) => {
        val localKeys    = pc.local.editedObservationKeys

        val newLocalKeys = localKeys.filterNot { k =>
          pc.remote.deletedKeys.contains(k) || pc.remote.nodeMap.contains(k)
        }

        val createKeys   = pc.obsEdits.collect {
          case ObsCreate(k, _) => k
        }

        keysMatch("newLocal", newLocalKeys, "create", createKeys)
      }
    ),

    ("ObsEdit produces entries for all deleted local observations",
      (start, local, remote, pc) => {
        val remoteKeys   =
          remote.fold(Set.empty[SPNodeKey]) { (s, n) =>
            n match {
              case _: ISPObservation => s + n.key
              case _                 => s

            }
          }

        val deletedLocalKeys = remoteKeys.intersect(pc.local.deletedKeys)

        val obsDeleteKeys   = pc.obsEdits.collect {
          case ObsDelete(k, _) => k
        }

        keysMatch("deletedLocal", deletedLocalKeys, "delete", obsDeleteKeys)
      }
    ),

    ("ObsEdit produces entries for all locally edited but remotely deleted nodes",
      (start, local, remote, pc) => {
        val localKeys = pc.local.editedObservationKeys

        val localEditRemoteDeleteKeys = localKeys.intersect(pc.remote.deletedKeys)

        val obsUpdateKeys = pc.obsEdits.collect {
          case ObsUpdate(k, _, None, _) => k
        }

        keysMatch("localEditRemDelete", localEditRemoteDeleteKeys, "update", obsUpdateKeys)
      }
    ),

    ("ObsEdit produces entries for all locally edited and remotely available nodes",
      (start, local, remote, pc) => {
        val localKeys = pc.local.editedObservationKeys

        val localEditKeys = localKeys.filter(pc.remote.nodeMap.contains)

        val obsUpdateKeys = pc.obsEdits.collect {
          case ObsUpdate(k, _, Some(_), _) => k
        }

        keysMatch("localEdit", localEditKeys, "update", obsUpdateKeys)
      }
    ),

    ("ObsEdit has no entries for only remote edited observations",
      (start, local, remote, pc) => {
        val localKeys  = pc.local.editedObservationKeys
        val remoteKeys = pc.remote.editedObservationKeys

        val remoteOnlyEditKeys = remoteKeys.filterNot { k =>
          localKeys.contains(k) || pc.local.deletedKeys.contains(k)
        }

        val keys = pc.obsEdits.collect {
          case e if remoteOnlyEditKeys.contains(e.key) => e.key
        }
        emptySet(pc, keys.toSet)
      }
    ),

    ("ObsEdit has no entries for new remote observations",
      (start, local, remote, pc) => {
        val remoteKeys = pc.remote.editedObservationKeys

        val newRemoteKeys = remoteKeys.filterNot { k =>
          pc.local.nodeMap.contains(k) || pc.local.deletedKeys.contains(k)
        }

        val keys = pc.obsEdits.collect {
          case e if newRemoteKeys.contains(e.key) => e.key
        }
        emptySet(pc, keys.toSet)
      }
    ),

    ("ObsEdit updates are all newer or conflicting",
      (start, local, remote, pc) => {
        pc.obsEdits.forall {
          case ObsUpdate(_, _, _, c) =>
            c.combined match {
              case Newer | Conflicting => true
              case _                   => false
            }
          case _ => true
        }
      }
    ),

    ("All template groups have unique version tokens",
      (start, local, remote, pc) => {
        pc.updatedLocalProgram.exists { ulp =>
          val tgs = Option(ulp.getTemplateFolder).toList.flatMap { tf =>
            tf.getTemplateGroups.asScala.toList
          }
          val vts = tgs.map(_.getDataObject.asInstanceOf[TemplateGroup].getVersionToken)
          vts.size == vts.toSet.size
        }.unsafePerformSync
      }
    ),

    ("Conflict folders are given strictly Newer version numbers",
      (start, local, remote, pc) =>
        pc.updatedLocalProgram.exists { ulp =>
          val conflicts = ulp.fold(List.empty[ISPNode]) { (lst, n) =>
            n.getDataObject match {
              case _: ConflictFolder => n :: lst
              case _                 => lst
            }
          }

          conflicts.forall { c =>
            VersionComparison.compare(ulp.getVersions(c.key), local.getVersions(c.key)) == Newer
          }
        }.unsafePerformSync
    ),

    ("Every merged node with a conflict note has a newer version number",
      (start, local, remote, pc) => {
        pc.updatedLocalProgram.exists { ulp =>

          val conflictKeys = ulp.fold(List.empty[SPNodeKey]) { (lst, n) =>
            if (n.getConflicts.nonEmpty()) n.key :: lst else lst
          }

          conflictKeys.forall { k =>
            def stat(p: ISPProgram): Option[ObservationStatus] =
              p.findDescendant(_.key == k).flatMap(d => Option(d.getContextObservation)).map(o => ObservationStatus.computeFor(o))

            // Obs permission corrections will throw everything off because they
            // will restore the local observation to duplicate the remote
            // version. To keep it simple we will just ignore anything that is a
            // possible status violation.
            val possibleStatusCorrection = {
              val us = stat(ulp)
              val ls = stat(local)
              (us != ls) || us.exists(_.isGreaterThan(ObservationStatus.READY)) || ls.exists(_.isGreaterThan(ObservationStatus.READY))
            }

            (VersionComparison.compare(ulp.getVersions(k), local.getVersions(k)) == Newer) || possibleStatusCorrection
          }
        }.unsafePerformSync
      }
    ),

    ("When both local and remote data objects are edited, a conflict is added",
      (start, local, remote, pc) => {
        // can't intersect pc.xx.dataObjectEditedNodes because in the actual
        // merge we don't have 3 way information.  we can't tell when the data
        // object itself has been updated vs the children.  all we can do is know
        // that the version data is conflicting and the data objects differ

        // compute the keys that should have a data object conflict
        val l = pc.local.editedNodes.map(_.key).toSet
        val r = pc.remote.editedNodes.map(_.key).toSet
        val expected = (l & r).filter { k =>
          !DOB.same(pc.local.nodeMap(k).getDataObject, pc.remote.nodeMap(k).getDataObject)
        }

        // find the merge plan nodes that *do* have a data object conflict
        val actual = pc.mergePlanConflictKeys((con: Conflicts, _: SPNodeKey) => con.dataObjectConflict.isDefined)

        expected == actual
      }
    ),

    ("Conflicts are added for conflicting moves",
      (start, local, remote, pc) => {
        val mergeParents = pc.mergePlan.update.foldTree(Map.empty[SPNodeKey, SPNodeKey]) { (p,m) =>
          (m/:p.subForest) { (m0, c) => m0 + (c.key -> p.key) }
        }

        val expected = pc.local.editedNodes.flatMap(p => p.children.map(c => (c.key, p.key, mergeParents.get(c.key))).collect {
          case (child, localParent, Some(mergeParent)) if localParent != mergeParent => child -> mergeParent
        })

        val actual = pc.mergePlan.update.sFoldRight(List.empty[(SPNodeKey, SPNodeKey)]) { (mn, l) =>
          mn match {
            case Modified(k, _, _, _, con) =>
              con.notes.asScalaList.collect {
                case m: Moved => m.nodeKey -> m.getDestinationKey
              } ++ l
            case _                         => l
          }
        }

        expected.toSet == actual.toSet
      }
    ),

    ("Updated local program has no observations with numbers greater than the program data max obs counter",
      (start, local, remote, pc) => {
        pc.updatedLocalProgram.exists { ulp =>
          val allObsNumbers = ulp.getAllObservations.asScala.map(_.getObservationNumber)
          val max = if (allObsNumbers.isEmpty) 0 else allObsNumbers.max

          val newObs = pc.fact.createObservation(ulp, Instrument.none, null)
          newObs.getObservationNumber > max
        }.unsafePerformSync
      }
    )
  )

  @Test
  def testAllMergeProperties(): Unit = {
    def mkPropContext(fact: ISPFactory, start: ISPProgram, local: ISPProgram, remote: ISPProgram): PropContext = {
      val diffs = ProgramDiff.compare(remote, local.getVersions, removedKeys(local))
      val mc    = MergeContext(local, diffs)
      PropContext(fact, start, local, remote, diffs, PreliminaryMerge.merge(mc))
    }

    new MergePropertyTest(mkPropContext).checkAllProperties(props)
  }
}
