package edu.gemini.sp.vcs

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.validator.TypeTree
import edu.gemini.pot.sp.version._
import edu.gemini.spModel.rich.pot.sp._

import scalaz.{Node=>_, _}
import edu.gemini.spModel.data.ISPDataObject

/**
 * Result of a merge.  It is a pending modification to the associated
 * science program node.  A result must be applied to actually modify the
 * program node.  Note, we don't edit the existing program in place during
 * a merge to avoid editing one of the inputs to the merge function as it is
 * working.  The science program nodes referenced by the merge plan may point
 * to the existing program that will be updated.
 */

object OldMergePlan {

  def apply(sp: ISPNode, vv: NodeVersions, dataObj: ISPDataObject, conflicts: Conflicts, children: List[OldMergePlan]): OldMergePlan =
    OldMergePlan(Node(sp, vv, dataObj, conflicts), children)

  /**
   * Data for a single node in the MergePlan (minus its children).  The "sp"
   * node is eventually updated using the data object and conflicts found here.
   * This is broken out of the MergePlan itself to facilitate creating a Zipper
   * for the MergePlan tree (see the MergePlan.Zipper.Crumb).
   *
   * @param sp program node
   * @param vv version vector that should be associated with the node
   * @param dataObj data object to apply to the node
   * @param conflicts conflicts to apply to the node
   */
  case class Node(sp: ISPNode, vv: NodeVersions, dataObj: ISPDataObject, conflicts: Conflicts, copyOf: Option[(SPNodeKey, NodeVersions)] = None) {

    // Makes a copy of the node so that it has a new key and a starting version vector
    final def replaceWithCopy(cp: EmptyNodeCopier): Node = {
      val cpSp = cp(sp, preserveKeys = false).get
      val cpVv = EmptyNodeVersions + (sp.getLifespanId -> 1)
      copy(sp = cpSp, vv = cpVv, copyOf = Some(sp.getNodeKey -> vv))
    }
  }

  // Converter for old-style lenses
  object Lens0 {
    def apply[A,B](f: A => B, g: (A, B) => A):Lens[A,B] =
      Lens(a => Store(b => g(a, b), f(a)))
  }

  object Node {
    val vv: Lens[Node, NodeVersions]     = Lens0(_.vv,        (node,vv)  => node.copy(vv = vv))
    val conflicts: Lens[Node, Conflicts] = Lens0(_.conflicts, (node,c)   => node.copy(conflicts = c))
    val obj: Lens[Node, ISPDataObject]  = Lens0(_.dataObj,   (node,obj) => node.copy(dataObj = obj))
  }

  val node: Lens[OldMergePlan, Node]                = Lens0(_.node,     (mp, n) => mp.copy(node = n))
  val children: Lens[OldMergePlan, List[OldMergePlan]] = Lens0(_.children, (mp, c) => mp.copy(children = c))

  // A zipper for easily manipulating a MergePlan after it is created by the
  // Merge
  object Zipper {
    case class Crumb(parent: Node, prev: List[OldMergePlan], next: List[OldMergePlan])

    val focus: Lens[Zipper, OldMergePlan] = Lens0(_.focus, (z, mp) => z.copy(focus = mp))
    val focusVv        = focus andThen OldMergePlan.node andThen Node.vv
    val focusConflicts = focus andThen OldMergePlan.node andThen Node.conflicts
    val focusDataObj   = focus andThen OldMergePlan.node andThen Node.obj
    val focusChildren  = focus andThen OldMergePlan.children
  }

  import Zipper._

  case class Zipper(focus: OldMergePlan, crumbs: List[Zipper.Crumb] = Nil) {

    def top: Zipper = up.map(_.top).getOrElse(this)

    def up: Option[Zipper] = crumbs match {
      case Crumb(parent, prev, next) :: tl =>
        Some(Zipper(OldMergePlan(parent, (focus :: prev).reverse ++ next), tl))
      case _                               => None
    }

    // down to the first child, if any
    def down: Option[Zipper] = focus.children match {
      case hd :: tl => Some(Zipper(hd, Crumb(focus.node, Nil, tl) :: crumbs))
      case _        => None
    }

    // next sibling, if any
    def next: Option[Zipper] = crumbs match {
      case Crumb(parent, prev, nextHd :: nextTl) :: tl =>
        Some(Zipper(nextHd, Crumb(parent, focus :: prev, nextTl) :: tl))
      case _                                           => None
    }

    // prev sibling if any
    def prev: Option[Zipper] = crumbs match {
      case Crumb(parent, prevHd :: prevTl, next) :: tl =>
        Some(Zipper(prevHd, Crumb(parent, prevTl, focus :: next) :: tl))
      case _                                           => None
    }

    // Find any descendant that matches the predicate.  Does a DFS.
    def find(f: OldMergePlan => Boolean): Option[Zipper] = {
      if (f(focus)) Some(this)
      else down.flatMap(_.find(f)) orElse next.flatMap(_.find(f))
    }

    private def findNext(f: OldMergePlan => Boolean): Option[Zipper] =
      if (f(focus)) Some(this)
      else next.flatMap(_.findNext(f))

    // Find a direct descendant that matches the predicate
    def findChild(f: OldMergePlan => Boolean): Option[Zipper] =
      down flatMap { _.findNext(f) }

    def set(newFocus: OldMergePlan): Zipper        = copy(focus = newFocus)
    def incr(id: LifespanId): Zipper            = focusVv.mod(_.incr(id), this)
    def addNote(note: Conflict.Note): Zipper    = focusConflicts.mod(_.withConflictNote(note), this)
    def setDataObj(obj: ISPDataObject): Zipper = focusDataObj.set(this, obj)

    /** Deletes the focus and moves up to its former parent, if any. */
    def delete: Option[Zipper]           = crumbs match {
      case Crumb(parent, prev, next) :: tl               =>
        Some(Zipper(OldMergePlan(parent, prev.reverse ++ next), tl))
      case _                                           => None
    }

    private def modFocusChildren(f: List[OldMergePlan] => List[OldMergePlan]) =
      focusChildren.mod(f, this)

    def prependChild(child: OldMergePlan): Zipper = modFocusChildren(child :: _)
    def appendChild(child: OldMergePlan): Zipper  = modFocusChildren(_ :+ child)

    def prepend(mergePlan: OldMergePlan): Option[Zipper] = crumbs match {
      case Crumb(parent, prev, next) :: tl =>
        Some(copy(crumbs = Crumb(parent, mergePlan :: prev, next) :: tl))
      case _ => None
    }

    def append(mergePlan: OldMergePlan): Option[Zipper] = crumbs match {
      case Crumb(parent, prev, next) :: tl =>
          Some(copy(crumbs = Crumb(parent, prev, mergePlan :: next) :: tl))
      case _ => None
    }

    def seq(ops: (Zipper => Option[Zipper])*): Option[Zipper] =
      (Option(this)/:ops) { _ flatMap _ }
  }
}

import OldMergePlan._

case class OldMergePlan(node: Node, children: List[OldMergePlan] = Nil) {
  def sp        = node.sp
  def vv        = node.vv
  def dataObj   = node.dataObj
  def conflicts = node.conflicts
  def key       = sp.getNodeKey

  /**
   * Does the dirty work of modifying the existing program and version map
   * according to the plan.
   * <em>WARNING:</em> side effects galore.
   */
  def apply(vm0: VersionMap): VersionMap = {
    val sending = sp.isSendingEvents
    try {
      // Turn off event sending so that ISPEventMonitors aren't triggered while
      // building up the program.  They make their own edits to the data objects
      // based upon context which is changing while the merge progresses.
      sp.setSendingEvents(false)

      sp.setDataObject(dataObj)
      val vm2 = (vm0/:children) { (vm1,child) => child.apply(vm1) }.updated(key, vv)
      sp.children = children map { _.sp }
      sp.setConflicts(sp.getConflicts.merge(conflicts))
      node.copyOf.fold(vm2) { case (k, v) => vm2.updated(k, v) }
    } finally {
      sp.setSendingEvents(sending)
    }
  }

  final def replaceWithCopy(cp: EmptyNodeCopier): OldMergePlan = copy(node = node.replaceWithCopy(cp))

  def toTypeTree: TypeTree = TypeTree(sp).copy(children = children.map(_.toTypeTree))
}