package edu.gemini.sp.vcs

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.Conflict._
import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.gemini.init.ObservationNI
import edu.gemini.spModel.obs.{ObsPhase2Status, SPObservation}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.util.SPTreeUtil
import edu.gemini.util.security.permission.ObsMergePermission
import edu.gemini.util.security.policy.ImplicitPolicy

import scala.Some
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._
import java.security.Principal

//
// Pre-process a science program merge, modifying the existing program if
// necessary to handle security constraint violations.  The idea is to look for
// observations that have been modified inappropriately for the incoming
// observation status and principal.  For example, if the PI has edited an
// observation in PHASE II that was set to READY without his knowledge.
//
// These cases cannot be merged so we will duplicate the existing observation
// (giving it new node keys and a new observation id) and mark it with a
// conflict that refers to the original observation key.  The original
// observation, in turn, is set to match exactly the incoming observation
// including the version numbers.
//
// The problem with handling them while doing the merge itself is that the
// logic of the "isKeepExisting" check can be altered if there is a security
// issue. Doing here in a pre-process step also keeps the remaining logic of
// the Merge algorithm free of security details involving specifics of
// particular nodes like observation status.
//

object MergeSecurity {

//  private case class ObsEdit(perm: ObsPermission, comparison: Option[Int])

  private case class ObsPair(in: Option[ISPObservation], ex: Option[ISPObservation]) {
    // at least one of them exists
    assert(in.orElse(ex).isDefined)

    // if they both exist they have the same key
    assert(in.map(_.getNodeKey).forall(key => ex.forall(_.getNodeKey.equals(key))))

    def key = in.orElse(ex).get.getNodeKey

    def perm = ObsMergePermission(in, ex)

    // isMissing (as opposed to deleted).  If the observation was never seen
    // in the local program there's nothing to merge or check.  It was checked
    // in successfully and we just need to include it.  An obs we had seen
    // before though means an obs that was deleted.
    def isMissingObs(exVm: VersionMap): Boolean =
      ex.isEmpty && in.map(_.getNodeKey).exists(nodeVersions(exVm,_).isEmpty)
  }

  private object ObsPair {
    def pairs(in: ISPProgram, ex: ISPProgram): List[ObsPair] = {
      def obsList(p: ISPProgram): List[ISPObservation] = p.getAllObservations.asScala.toList
      val inList = obsList(in)
      val exList = obsList(ex)

      val exMap = (Map.empty[SPNodeKey, ObsPair]/:exList) { (m, o) =>
        m + (o.getNodeKey -> ObsPair(None, Some(o)))
      }

      val pairs = (exMap/:inList) { (m, o) =>
        val key  = o.getNodeKey
        val pair = m.get(key).map(_.copy(in = Some(o))).getOrElse(ObsPair(Some(o), None))
        m + (key -> pair)
      }
      pairs.values.toList
    }
  }

  /**
   * Obtains a list of ObsPermission for each attempted modification made in
   * the existing program (relative to the incoming program) that cannot be
   * legally executed.
   *
   * @param in incoming program
   * @param ex existing program
   *
   * @return list of ObsMergePermissions that fail
   */
  def violations(odb: IDBDatabaseService, in: ISPProgram, ex: ISPProgram, user: Set[Principal]): List[ObsMergePermission] = {

    def hasPermission(perm: ObsMergePermission): Boolean =
      ImplicitPolicy.hasPermission(odb, user, perm).unsafePerformIO()

    val exVm = ex.getVersions
    ObsPair.pairs(in, ex).filterNot(_.isMissingObs(exVm)).map(_.perm).filterNot(hasPermission)
  }

  private class Processor(odb: IDBDatabaseService, in: ISPProgram, ex: ISPProgram, user: Set[Principal]) {
    lazy val cp = new EmptyNodeCopier(odb.getFactory, ex)

    /**
     * Applies fixes to the existing program to correct the permission check
     * violations.
     */
    def repair() {
      violations(odb, in, ex, user) foreach {
        case ObsMergePermission(None, Some(exObs))        => repairCreate(exObs)
        case ObsMergePermission(Some(inObs), Some(exObs)) => repairUpdate(inObs, exObs)
        case ObsMergePermission(Some(inObs), None)        => repairDelete(inObs)
      }
    }

    private def repairCreate(exObs: ISPObservation) {
      val dataObj = exObs.getDataObject.asInstanceOf[SPObservation]
      dataObj.setPhase2Status(ObsPhase2Status.PI_TO_COMPLETE)
      dataObj.setExecStatusOverride(edu.gemini.shared.util.immutable.None.instance())
      exObs.setDataObject(dataObj)

      exObs.addConflictNote(new CreatePermissionFail(exObs.getNodeKey))
    }

    private def replaceChild(parent: ISPNode, oldKey: SPNodeKey, newChild: ISPNode) {
      parent.children = parent.children map { child =>
        if (child.getNodeKey == oldKey) newChild else child
      }
    }

    private def replaceMovedNodes(inObs: ISPObservation, exObsOpt: Option[ISPObservation]) {

      // Get all the nodes in inObs that are elsewhere in the existing program
      // (if any).  Usually there will be none.
      val movedNodeKeys: Set[SPNodeKey] = {
        def keys(inSet: Set[SPNodeKey], n: ISPNode): Set[SPNodeKey] =
          ((inSet + n.getNodeKey)/:n.children) { keys }

        keys(Set.empty, inObs) &~ ~(exObsOpt map { exObs =>
          keys(Set.empty, exObs)
        })
      }

      // For each one, replace it with a new copy of that node (with a new key)
      // but with the same data object, conflicts, and children.  We can't have
      // two different nodes with the same key in different parts of the program
      movedNodeKeys foreach { key =>
        Option(SPTreeUtil.findByKey(ex, key)) foreach { node =>

          val nodeCopy = cp(node, preserveKeys = false).get
          nodeCopy.setDataObject(node.getDataObject)
          nodeCopy.setConflicts(node.getConflicts)
          nodeCopy.children = node.children

          replaceChild(node.getParent, node.getNodeKey, nodeCopy)
        }
      }
    }

      // Update the versions of the incoming observation nodes to show no change
      // to the observation that was copied from the incoming program
    private def resetObsVv(inObs: ISPObservation) {
        val inVm = in.getVersions
        val exVm = ex.getVersions

        def setVv(vm: VersionMap, inNode: ISPNode): VersionMap = {
          val key = inNode.getNodeKey
          (vm.updated(key, nodeVersions(inVm, key))/:inNode.children) { setVv }
        }

        ex.setVersions(setVv(exVm, inObs))
    }

    private def conflictFolder(p: ISPContainerNode): ISPConflictFolder = {
      val folder = Option(p.getConflictFolder) getOrElse {
        odb.getFactory.createConflictFolder(ex, null)
      }
      p.setConflictFolder(folder)
      folder
    }

    private def repairUpdate(inObs: ISPObservation, exObs: ISPObservation) {
      // First handle the case where we've moved one of the nodes from the
      // observation to elsewhere in the existing program.
      replaceMovedNodes(inObs, Some(exObs))

      // Make a copy of the incoming observation (deep copy, preserve keys) and
      // a copy of the existing observation (deep copy, new keys)
      val inObsCopy = odb.getFactory.createObservationCopy(ex, inObs, true)
      val exObsCopy = odb.getFactory.createObservationCopy(ex, exObs, false)
      ObservationNI.reset(exObsCopy, true)

      // Add a conflict note to the existing observation to make it clear what
      // happened.
      exObsCopy.addConflictNote(new UpdatePermissionFail(exObs.getNodeKey))

      val parent = exObs.getParent
      replaceChild(parent, exObs.getNodeKey, inObsCopy)

      val folder = conflictFolder(parent)
      folder.children = folder.children :+ exObsCopy

      resetObsVv(inObs)
    }

    private def repairDelete(inObs: ISPObservation) {
      // First handle the case where we've moved one of the nodes from the
      // observation to elsewhere in the existing program.
      replaceMovedNodes(inObs, None)

      // Make a copy of the incoming observation (deep copy, preserve keys)
      val inObsCopy = odb.getFactory.createObservationCopy(ex, inObs, true)

      // Add a conflict note
      inObsCopy.addConflictNote(new DeletePermissionFail(inObsCopy.getNodeKey))

      resetObsVv(inObs)

      def insertIntoParent(newExNode: ISPNode, inNode: ISPNode): ISPNode = {
        val inParent = inNode.getParent
        val exParent = Option(SPTreeUtil.findByKey(ex, inParent.getNodeKey)) getOrElse {
          val p = cp(inParent).get
          p.setDataObject(inParent.getDataObject) // copy data object from incoming
          insertIntoParent(p, inParent)
        }
        exParent.children = orderedInsert(exParent.children, newExNode, inParent.children)(_.getNodeKey)
        newExNode
      }

      val exJvm = ex.getVersions
      insertIntoParent(inObsCopy, inObs)
      ex.setVersions(exJvm)
    }
  }

  /**
   * Modifies the existing program to address permissions issues (and add
   * corresponding conflict notes).
   *
   * @param odb observing database in which new nodes are created if necessary
   * @param in incoming program, which is left unmodified
   * @param ex existing program, which is potentially modified to remove
   *           permissions issues
   */
  def process(odb: IDBDatabaseService, in: ISPProgram, ex: ISPProgram, user: Set[Principal]): Unit =
    new Processor(odb, in, ex, user).repair()
}

