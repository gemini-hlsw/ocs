package jsky.app.ot.util

import scalaz._
import edu.gemini.pot.sp.{ISPProgram, ISPNode, SPNodeKey}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.rich.pot.sp._
import collection.JavaConverters._

/**
 * Browser history that tracks root nodes, and within each roots tracks selected nodes. This structure maintains the
 * following invariants:
 * <ul>
 * <li>No root node appears more than once.
 * <li>Within a given root, no [child] node appears more than once.
 * </ul>
 */
class History private(db: IDBDatabaseService, pz: Option[PZip]) {

  def this(db: IDBDatabaseService) = this(db, None)

  /** Return the root ancestor of the currently focused node, if any. */
  def root: Option[ISPProgram] =
    pz.flatMap(_.root(db))

  def rootOrNull: ISPProgram =
    root.orNull

  /** Return the currently focused node, if any. */
  def node: Option[ISPNode] =
    pz.flatMap(_.node(db))

  def nodeOrNull: ISPNode =
    node.orNull

  /** Returns a new History, cleaned of any references that are invalid due to deletion. */
  def preen: History =
    new History(db, pz.flatMap(_.preen(db)))

  /** Returns a RootEntry for each root node. Useful for constructing a "go to" menu. */
  def rootEntries: List[RootEntry] =
    pz.fold(List[RootEntry]())(_.rootEntries(db))

  /** Returns a RootEntry for each root node. Useful for constructing a "go to" menu. */
  def rootEntriesAsJava: java.util.List[RootEntry] =
    rootEntries.asJava

  /** Returns a new History focused on the given node. */
  def go(node: ISPNode): History =
    if (pz.exists(_.zipper.focus.zipper.focus == node.getNodeKey)) this
    else {
      val (rk, nk) = (node.getProgram.getNodeKey, node.getNodeKey)
      new History(db, pz.map(_.go(rk, nk)) orElse Some(PZip.initial(rk, nk)))
    }

  /** Returns a new History focused on the given node, iff the node's root ancestor exists in this history. */
  def find(node: ISPNode): Option[History] = {
    val (rk, nk) = (node.getProgram.getNodeKey, node.getNodeKey)
    pz.flatMap(_.find(rk, nk)).map(z => new History(db, Some(z)))
  }

  def findOrNull(node: ISPNode): History =
    find(node).orNull

  /** Returns a new History focused on the current node in the previous root (if any). */
  def prev: Option[History] =
    nav(_.prev)

  def prevOrNull: History =
    prev.orNull

  /** Returns a new History focused on the current node in the next root (if any). */
  def next: Option[History] =
    nav(_.next)

  def nextOrNull: History =
    next.orNull

  /** Returns a new History focused on the previous node (if any). */
  def prevNode: Option[History] =
    nav(_.prevNode)

  def prevNodeOrNull: History =
    prevNode.orNull

  /** Returns a new History focused on the next node (if any). */
  def nextNode: Option[History] =
    nav(_.nextNode)

  def nextNodeOrNull: History =
    nextNode.orNull

  /** Returns a new History with the current focused program removed. */
  def delete: History =
    nav(_.delete).getOrElse(empty)

  /** Returns a new History with the specified root removed. */
  def delete(r: ISPProgram): History = root match {
    case None => this
    case Some(x) if x == r => delete
    case Some(x) => find(r).map(_.delete.go(x)).getOrElse(this)
  }

  def empty: History =
    new History(db)

  /** Returns true if there exists a previous root node. */
  def hasPrev: Boolean =
    isDefined(_.prev)

  /** Returns true if there exists a next root node. */
  def hasNext: Boolean =
    isDefined(_.next)

  /** Returns true if there exists a previous node within the current root. */
  def hasPrevNode: Boolean =
    isDefined(_.prevNode)

  /** Returns true if there exists a next node within the current root. */
  def hasNextNode: Boolean =
    isDefined(_.nextNode)

  // Helpers

  private def nav(f: PZip => Option[PZip]): Option[History] =
    pz.flatMap(f).map(z => new History(db, Some(z)))

  private def isDefined(f: PZip => Option[PZip]): Boolean =
    pz.flatMap(f).isDefined

  override def toString: String =
    s"History(${db.getUuid}, $pz)"

}


/** An entry composed of a root node and one of its children. */
case class RootEntry(root: ISPProgram, node: ISPNode) {
  require(node.getProgram == root, "Bogus RootEntry (node is not a child of root)")
}


/** A zipper over nodes within a program. */
case class NZip(root: SPNodeKey, zipper: Zipper[SPNodeKey]) {

  private def nav(f: Zipper[SPNodeKey] => Option[Zipper[SPNodeKey]]): Option[NZip] =
    f(zipper).map(z0 => copy(zipper = z0))

  def next: Option[NZip] =
    nav(_.next)

  def prev: Option[NZip] =
    nav(_.previous)

  /** Navigate to the specified node, discarding the future. */
  def go(k: SPNodeKey): NZip = {
    val z0 = zipper.insert(k)
    val z1 = z0.copy(lefts = z0.lefts.filterNot(_ == k), rights = Stream.empty)
    copy(zipper = z1)
  }

  def root(db: IDBDatabaseService): Option[ISPProgram] =
    Option(db.lookupProgram(root))

  def node(db: IDBDatabaseService): Option[ISPNode] =
    rootEntry(db).map(_.node)

  def rootEntry(db: IDBDatabaseService): Option[RootEntry] =
    for {
      r <- root(db)
      n <- r.findDescendant(_.getNodeKey == zipper.focus)
    } yield RootEntry(r, n)

  def preen(db: IDBDatabaseService): Option[NZip] =
    node(db) match {
      case None => zipper.delete.flatMap(NZip(root, _).preen(db))
      case Some(n) => root(db) map { r =>

        def isChild(k: SPNodeKey): Boolean =
          r.findDescendant(_.getNodeKey == k).isDefined

        copy(zipper = zipper.copy(lefts = zipper.lefts.toList.filter(isChild).toStream, rights = zipper.rights.toList.filter(isChild).toStream))
      }
    }

}

object NZip {

  def initial(root: SPNodeKey, node: SPNodeKey): NZip =
    NZip(root, Zipper(Stream.empty, node, Stream.empty))

}

/** A zipper over program zippers. */
case class PZip(zipper: Zipper[NZip]) {

  // Accessors

  def root(db: IDBDatabaseService): Option[ISPProgram] =
    zipper.focus.root(db)

  def node(db: IDBDatabaseService): Option[ISPNode] =
    zipper.focus.node(db)

  def preen(db: IDBDatabaseService): Option[PZip] =
    zipper.focus.preen(db) match {
      case None => zipper.delete.flatMap(PZip(_).preen(db))
      case Some(f) => Some(PZip(Zipper(zipper.lefts.toList.flatMap(_.preen(db)).toStream, f, zipper.rights.toList.flatMap(_.preen(db)).toStream)))
    }

  def rootEntries(db: IDBDatabaseService): List[RootEntry] =
    zipper.toStream.flatMap(_.rootEntry(db)).toList

  /** Returns the focused node in each program. */
  def nodes(db: IDBDatabaseService): List[ISPNode] =
    zipper.toStream.toList.flatMap(_.node(db))

  // Program navigation

  private def nav(f: Zipper[NZip] => Option[Zipper[NZip]]): Option[PZip] =
    f(zipper).map(z0 => copy(zipper = z0))

  def delete: Option[PZip] =
    nav(_.delete)

  def next: Option[PZip] =
    nav(_.next)

  def prev: Option[PZip] =
    nav(_.previous)

  // Node navigation

  private def navNode(f: NZip => Option[NZip]): Option[PZip] =
    f(zipper.focus).map(e => PZip(zipper.update(e)))

  def nextNode: Option[PZip] =
    navNode(_.next)

  def prevNode: Option[PZip] =
    navNode(_.prev)

  // Teleportation

  def go(root: SPNodeKey, node: SPNodeKey): PZip =
    find(root, node).getOrElse(copy(zipper = zipper.insert(NZip.initial(root, node))))

  def find(root: SPNodeKey, node: SPNodeKey): Option[PZip] =
    zipper.findZ(_.root == root).map(_.modify(_.go(node))).map(PZip(_))

}

object PZip {

  def initial(root: SPNodeKey, node: SPNodeKey): PZip =
    PZip(Zipper(Stream.empty, NZip.initial(root, node), Stream.empty))

}