package edu.gemini.sp.vcs

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.sp.vcs.VcsLocking.MergeOp
import edu.gemini.sp.vcs.VcsFailure._
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._
import edu.gemini.spModel.data.ISPDataObject
import java.security.Principal

private[vcs] object Merge extends MergeOp[ISPProgram] {
  def apply(odb: IDBDatabaseService, input: ISPProgram, existing: ISPProgram, user: Set[Principal]): TryVcs[ISPProgram] = {
    MergeSecurity.process(odb, input, existing, user)

    // Capture the existing and and input version maps before they are modified
    val vmE = existing.getVersions
    val vmI = input.getVersions

    val mergeNodeMap = MergeNode.map(input, existing)
    val cp = EmptyNodeCopier(odb.getFactory, existing)
    val root = mergeNodeMap(input.getNodeKey)
    val plan = root.plan(mergeNodeMap, cp)

    def addDeletedNodes(vm: VersionMap): VersionMap = {
      val missingKeys = (vmE.keySet ++ vmI.keySet).filterNot(vm.contains)
      (vm /: missingKeys) {
        (vm0, key) =>
          vm0.updated(key, nodeVersions(vmE, key) sync nodeVersions(vmI, key))
      }
    }

    // Edit the existing program according to the given known-to-be-valid merge plan.
    def doEdits(plan: MergePlan): TryVcs[ISPProgram] = {
      val vm = addDeletedNodes(plan.apply(EmptyVersionMap))
      existing.renumberObservationsToMatch(input)
      TemplateGroupNumbering.renumber(existing, input)
      existing.setVersions(vm)
      existing.right
    }

    MergeValidity(existing, odb).process(plan).fold(
      msg  => Unexpected(msg).left,
      plan => doEdits(plan)
    )
  }

  sealed trait NodeStatus

  /**
   * Science program node status relative to a particular science program.
   * Nodes are either present in the program, were present but now deleted, or
   * else were never in the program before.
   */
  object NodeStatus {

    /** Present somewhere in the Science Program. */
    case object Present extends NodeStatus

    /** Was in the Science Program before but now has been deleted. */
    case object Deleted extends NodeStatus

    /** Was never in the Science Program. */
    case object Missing extends NodeStatus

  }

  /**
   * A Science Program node with its node versions.  For deleted or missing
   * nodes, the ISPNode will be None.
   */
  case class VersionedSpNode(sp: Option[ISPNode], vvOpt: Option[NodeVersions]) {
    // vv: NodeVersions) {

    import edu.gemini.sp.vcs.Merge.NodeStatus._

    def status: NodeStatus = sp.cata(_ => Present, if (vvOpt.isEmpty) Missing else Deleted)

    def vv: NodeVersions = vvOpt | EmptyNodeVersions
  }

  type MergeNodeMap = Map[SPNodeKey, MergeNode]

  /**
   * Every science program node in either the incoming program or the existing
   * program (or both) has one MergeNode that associates the incoming and
   * existing nodes with their version information.  MergeNodes are only created
   * when at least one of the incoming or existing program contains the
   * science program node (i.e., they can never hold two VersionedSpNodes
   * which each have a None ISPNode).
   */
  case class MergeNode(in: VersionedSpNode, ex: VersionedSpNode) {
    // At least one of "in" or "ex" has an associated sp node.
    assert(in.sp.isDefined || ex.sp.isDefined)

    // If both are defined, then they have the same key.
    assert {
      (for (inSp <- in.sp; exSp <- ex.sp) yield (inSp.getNodeKey, exSp.getNodeKey)) forall {
        case (inKey, exKey) => inKey == exKey
      }
    }

    import edu.gemini.sp.vcs.Merge.NodeStatus._

    // The key matches so it can come from either the incoming (remote) or
    // existing (local) node.
    val key: SPNodeKey = in.sp.map(_.getNodeKey) | ex.sp.get.getNodeKey
    val comp: Option[Int] = in.vv.tryCompareTo(ex.vv)

    def isKeepExisting = comp.forall(_ <= 0) || in.status == Deleted

    private def parent(mnm: MergeNodeMap, vn: VersionedSpNode): Option[MergeNode] =
      for {
        sp <- vn.sp
        p <- Option(sp.getParent)
      } yield mnm(p.getNodeKey)

    def inParent(mnm: MergeNodeMap): Option[MergeNode] = parent(mnm, in)

    def exParent(mnm: MergeNodeMap): Option[MergeNode] = parent(mnm, ex)

    private def children(mnm: MergeNodeMap, vn: VersionedSpNode): List[MergeNode] =
      vn.sp.map(_.children.map(child => mnm(child.getNodeKey))) | Nil

    def inChildren(mnm: MergeNodeMap): List[MergeNode] = children(mnm, in)

    def exChildren(mnm: MergeNodeMap): List[MergeNode] = children(mnm, ex)

    // Counts as modified if vv0 is newer than vv1 or else if they are in
    // conflict (i.e., tryCompareTo returns None), or if any children are
    // modified
    private def isModified(vv0: MergeNode => NodeVersions,
                           vv1: MergeNode => NodeVersions,
                           c: MergeNode => List[MergeNode]): Boolean =
      if (vv0(this).tryCompareTo(vv1(this)).forall(_ > 0)) true
      else c(this) exists {
        _.isModified(vv0, vv1, c)
      }

    def isInModified(mnm: MergeNodeMap) = isModified(_.in.vv, _.ex.vv, _.inChildren(mnm))

    def isExModified(mnm: MergeNodeMap) = isModified(_.ex.vv, _.in.vv, _.exChildren(mnm))

    /**
     * Applies a merge operation to this node to combine the incoming and
     * existing nodes into a MergePlan, which is a pending change to the
     * existing science program.  Nothing is actually modified as a result of
     * this method, only a tree of changes to be applied (i.e., instructions)
     * is created.
     */
    final def plan(mnm: MergeNodeMap, cp: EmptyNodeCopier): MergePlan = {
      val childMerger = ChildMerger(this, mnm, cp)

      if (ex.status == Deleted) childMerger.useIncoming
      else if (in.status == Deleted) childMerger.copyExisting
      else comp match {
        case Some(i) if i <= 0 => childMerger.useExisting
        case Some(i) if i > 0 => childMerger.useIncoming
        case None => childMerger.conflict
      }
    }
  }

  object MergeNode {

    /**
     * Creates a Map from SPNodeKey to MergeNode for all science program nodes
     * in either the incoming (remote) or existing (local) program (or both).
     */
    def map(in: ISPProgram, ex: ISPProgram): MergeNodeMap = {
      val inMap = nodeMap(in)
      val inVm = in.getVersions
      val exMap = nodeMap(ex)
      val exVm = ex.getVersions

      def addTo(mnm: MergeNodeMap, key: SPNodeKey): MergeNodeMap =
        if (mnm.contains(key)) mnm
        else {
          val inVersioned = VersionedSpNode(inMap.get(key), inVm.get(key))
          val exVersioned = VersionedSpNode(exMap.get(key), exVm.get(key))
          mnm + (key -> MergeNode(inVersioned, exVersioned))
        }

      val inMnm = (Map.empty[SPNodeKey, MergeNode] /: inMap.keys) {
        addTo
      }
      (inMnm /: exMap.keys) {
        addTo
      }
    }
  }

  case class ChildMerger(mn: MergeNode, mnm: MergeNodeMap, cp: EmptyNodeCopier) {

    import edu.gemini.sp.vcs.Merge.NodeStatus._

    private val spNode = mn.ex.sp | (mn.in.sp.map {
      n =>
        val res = cp(n).get
        res.setDataObject(n.getDataObject)
        res
    } | sys.error("both existing and incoming nodes empty"))

    private val vv = mn.in.vv sync mn.ex.vv

    private def dataObject(vsn: VersionedSpNode) = vsn.sp.get.getDataObject

    private def inDataObject = dataObject(mn.in)

    private def exDataObject = dataObject(mn.ex)

    private lazy val inChildren = mn.inChildren(mnm)
    private lazy val inKeys = inChildren.map(_.key).toSet
    private lazy val exChildren = mn.exChildren(mnm)
    private lazy val exKeys = exChildren.map(_.key).toSet
    private lazy val commonKeys = inKeys & exKeys
    private lazy val onlyInKeys = inKeys -- commonKeys
    private lazy val onlyExKeys = exKeys -- commonKeys

    private lazy val onlyIn = inChildren filter {
      c => onlyInKeys.contains(c.key)
    }
    private lazy val onlyEx = exChildren filter {
      c => onlyExKeys.contains(c.key)
    }
    private lazy val newIn = onlyIn filter {
      _.ex.status == Missing
    }
    private lazy val newEx = onlyEx filter {
      _.in.status == Missing
    }


    // Children of the incoming node that were deleted in the existing program
    // and yet were modified remotely.
    private lazy val deletedButModifiedIn = onlyIn filter {
      c =>
        c.ex.status == Deleted && c.isInModified(mnm)
    }

    // Children of existing node that were deleted in the incoming program and
    // yet were modified locally.
    private lazy val deletedButModifiedEx = onlyEx filter {
      c =>
        c.in.status == Deleted && c.isExModified(mnm)
    }

    private def insert(intoList: List[MergeNode], child: MergeNode, positionList: List[MergeNode]): List[MergeNode] =
      orderedInsert(intoList, child, positionList)(_.key)

    val MOVED: (MergeNode => Conflict.Note) = mn =>
      new Conflict.Moved(mn.key, mn.inParent(mnm).get.key)

    val RESURRECTED_LOCAL_DELETE: (MergeNode => Conflict.Note) = mn =>
      new Conflict.ResurrectedLocalDelete(mn.key)

    private def conflictNotes(flicks: (MergeNode => Conflict.Note, List[MergeNode])*): List[Conflict.Note] =
      flicks.toList flatMap {
        case (f, lst) => lst.map(f)
      }

    private def replacedRemoteDeleteNotes(lst: List[MergePlan]): List[Conflict.Note] =
      lst collect {
        case MergePlan(MergePlan.Node(sp, _, _, _, Some(_)), _) => new Conflict.ReplacedRemoteDelete(sp.getNodeKey)
      }

    private def toMergePlan(children: List[MergeNode]): List[MergePlan] =
      (children :\ List.empty[MergePlan]) {
        (child, resList) => child.plan(mnm, cp) :: resList
      }

    // useExisting when existing node is newer (or no change) or else incoming
    // node is deleted
    def useExisting: MergePlan = {
      // Take the existing children, but remove any that are in parents where
      // the incoming node prevails.
      val (keep, moved) = exChildren partition {
        _.inParent(mnm).forall(_.isKeepExisting)
      }

      // Keep any nodes deleted in the existing program yet modified remotely
      val children = toMergePlan {
        (keep /: deletedButModifiedIn) {
          insert(_, _, inChildren)
        }
      }

      val flicks = if (mn.in.status == Deleted) Conflicts.EMPTY
      else conflicts(conflictNotes(MOVED -> moved, RESURRECTED_LOCAL_DELETE -> deletedButModifiedIn))

      MergePlan(spNode, vv, exDataObject, flicks, children)
    }

    // Take the useExisting result but replacing the node with a copy that has
    // unique keys and a starting version vector.
    def copyExisting: MergePlan = useExisting.replaceWithCopy(cp)

    // useIncoming when incoming node is newer or else existing is deleted
    def useIncoming: MergePlan = {
      // Keep any nodes deleted in the incoming program yet modified locally
      val children = toMergePlan {
        (inChildren /: deletedButModifiedEx) {
          insert(_, _, exChildren)
        }
      }

      val flicks = if (mn.ex.status == Deleted) Conflicts.EMPTY
      else conflicts(replacedRemoteDeleteNotes(children))

      MergePlan(spNode, vv, inDataObject, flicks, children)
    }

    // conflict when there are changes in both versions and both versions are
    // present (i.e., not deleted)
    def conflict: MergePlan = {
      val children = toMergePlan {
        // Start with the list of incoming children that are present in both
        // the incoming and existing child lists, or new in incoming.
        val keep = commonKeys ++ newIn.map(_.key)
        val c0 = inChildren.filter(c => keep.contains(c.key))

        // Add any that were deleted in the incoming program but were modified in
        // existing.
        val c1 = (c0 /: deletedButModifiedEx) {
          insert(_, _, exChildren)
        }

        // Add any that were deleted in the existing program but were modified in
        // incoming.
        val c2 = (c1 /: deletedButModifiedIn) {
          insert(_, _, inChildren)
        }

        // Add any that are new in the existing program.
        (c2 /: newEx) {
          insert(_, _, exChildren)
        }
      }

      val flicks0 = conflicts(
        conflictNotes(RESURRECTED_LOCAL_DELETE -> deletedButModifiedIn),
        replacedRemoteDeleteNotes(children)
      )

      def isDataObjectSame = (for {
        inDo <- mn.in.sp.map(_.getDataObject)
        exDo <- mn.ex.sp.map(_.getDataObject)
      } yield DataObjectBlob.same(inDo, exDo)).getOrElse(false)

      def mkDoc = new DataObjectConflict(DataObjectConflict.Perspective.LOCAL, exDataObject)

      type Mergeable = ISPMergeable[ISPDataObject]
      def mergedDataObject = (inDataObject, exDataObject) match {
        case (inM: Mergeable, exM: Mergeable) => Option(inM.mergeOrNull(exM))
        case _ => None
      }

      def withDataObjectConflict = (inDataObject, flicks0.withDataObjectConflict(mkDoc))

      val (dataObject, flicks) =
        if (isDataObjectSame) (inDataObject, flicks0)
        else if (flicks0.isEmpty) mergedDataObject.map((_, flicks0)).getOrElse(withDataObjectConflict)
        else withDataObjectConflict

      MergePlan(spNode, vv, dataObject, flicks, children)
    }
  }

}