package edu.gemini.sp.vcs2

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version.{EmptyNodeVersions, EmptyVersionMap, LifespanId, NodeVersions, VersionMap}
import edu.gemini.shared.util.VersionComparison
import edu.gemini.sp.vcs2.NodeDetail.Obs
import edu.gemini.spModel.conflict.ConflictFolder
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.rich.pot.sp._

import scala.annotation.tailrec
import scalaz._
import Scalaz._
import scalaz.Tree.Node

/** MergeNodes form a tree with potential links into an existing science
  * program.  There are two types of MergeNode, [[edu.gemini.sp.vcs2.Modified]]
  * and [[edu.gemini.sp.vcs2.Unmodified]]s.  `Modified` describes a
  * potential update to an existing science program node (or the definition of a
  * node missing locally). `Unmodified` is just a `MergeNode` wrapper for an
  * existing science program node.
  */
sealed trait MergeNode {
  def key: SPNodeKey
  def isModified: Boolean
}


final case class Modified(key: SPNodeKey,
                          nv: NodeVersions,
                          dob: ISPDataObject,
                          detail: NodeDetail,
                          conflicts: Conflicts) extends MergeNode {

  def this(n: ISPNode) =
    this(n.key, n.getVersion, n.getDataObject, NodeDetail(n), n.getConflicts)

  def withDataObjectConflict(dob: ISPDataObject): Modified = {
    val doc = new DataObjectConflict(DataObjectConflict.Perspective.LOCAL, dob)
    copy(conflicts = conflicts.withDataObjectConflict(doc))
  }

  def withConflictNote(n: Conflict.Note): Modified =
    copy(conflicts = conflicts.withConflictNote(n))

  def isModified = true
}

/** Contains any relevant details about the node that don't appear in its data
  * object or list of children.
  */
sealed trait NodeDetail

object NodeDetail {
  case object Empty extends NodeDetail
  final case class Obs(number: Int) extends NodeDetail

  def apply(n: ISPNode): NodeDetail = n match {
    case o: ISPObservation => Obs(o.getObservationNumber)
    case _                 => Empty
  }
}

final case class Unmodified(key: SPNodeKey) extends MergeNode {
  def isModified = false
}


object MergeNode {
  def unmodified(n: ISPNode): MergeNode =
    Unmodified(n.key)

  def modified(key: SPNodeKey, nv: NodeVersions, dob: ISPDataObject, detail: NodeDetail, conflicts: Conflicts): MergeNode =
    Modified(key, nv, dob, detail, conflicts)

  def modified(n: ISPNode): MergeNode = new Modified(n)

  def modifiedTree(root: ISPNode): Tree[MergeNode] =
    Node(modified(root), root.children.map(modifiedTree).toStream)

  implicit class TreeOps[A](t: Tree[A]) {
    /** A `foldRight` with strict evaluation of the `B` value of `f`. */
    def sFoldRight[B](z: B)(f: (A, B) => B): B =
      foldTree(z)((t0, b) => f(t0.rootLabel, b))

    // more problems with foldRight.  throws stack overflow for giant programs
//      t.foldRight(z) { (a, b) => f(a,b) }

    // TODO: what's the proper way to do this?
    /** A fold that provides access to the children. */
    def foldTree[B](z: B)(f: (Tree[A], B) => B): B = {
      @tailrec
      def go(rem: List[Tree[A]], res: B): B =
        rem match {
          case Nil      => res
          case t2 :: ts => go(t2.subForest.toList ++ ts, f(t2, res))
        }

      go(List(t), z)
    }

    def sFoldMap[B: Monoid](f: A => B): B =
      sFoldRight(Monoid[B].zero) { (a, b) => f(a) |+| b }
  }

  implicit class TreeLocOps[A](tl: TreeLoc[A]) {
    /** Deletes the current node and selects the parent.  Unlike the
      * `TreeLoc.delete` function, this function will always select the parent
      * node. */
    //noinspection MutatorLikeMethodIsParameterless
    def deleteNodeFocusParent: Option[TreeLoc[A]] = {
      def combine(ls: Stream[Tree[A]], rs: Stream[Tree[A]]) =
        ls.foldLeft(rs)((a, b) => b #:: a)

      tl.parents match {
        case (pls, v, prs) #:: ps => Some(TreeLoc.loc(Node(v, combine(tl.lefts, tl.rights)), pls, prs, ps))
        case Stream.Empty         => None
      }
    }
  }

  implicit class MergeTreeOps(t: Tree[MergeNode]) {
    def key: SPNodeKey = t.rootLabel.key

    def keySet: Set[SPNodeKey] =
      t.sFoldRight(Set.empty[SPNodeKey]) { (n, s) => s + n.key }

    /** A fold over observations contained in this tree node (if any).
      * Doesn't descend into the observation itself.
      */
    def foldObservations[B](z: B)(f: (Modified, Int, Stream[Tree[MergeNode]], B) => B): B = {
      def go(rem: List[Tree[MergeNode]], res: B): B =
        rem match {
          case Nil        => res
          case (t2 :: ts) => t2.rootLabel match {
            case m@Modified(_, _, _, Obs(n), _) => go(ts, f(m, n, t2.subForest, res))
            case _                              => go(t2.subForest.toList ++ ts, res)
          }
        }

      go(List(t), z)
    }

    def vm: VersionMap =
      t.sFoldRight(EmptyVersionMap) { (n, vm) =>
        n match {
          case Modified(k, nv, _, _, _) => vm + (k -> nv)
          case _                        => vm
        }
      }

    def compare(that: Tree[MergeNode]): VersionComparison = VersionMap.compare(vm, that.vm)

    def focus(k: SPNodeKey): TryVcs[TreeLoc[MergeNode]] = t.loc.findNode(k)

    def mRootLabel: TryVcs[Modified] =
      t.rootLabel match {
        case m: Modified => m.right
        case l           => TryVcs.fail(s"Expected Modified label for ${l.key}")
      }

    def mModifyLabel(f: Modified => Modified): TryVcs[Tree[MergeNode]] =
      t.rootLabel match {
        case m: Modified => Node(f(m): MergeNode, t.subForest).right
        case _           => TryVcs.fail(s"Expected Modified label for $key")
      }

    def incr(lifespanId: LifespanId): TryVcs[Tree[MergeNode]] =
      mModifyLabel(m => m.copy(nv = m.nv.incr(lifespanId)))
  }

  implicit class MergeZipperOps(z: TreeLoc[MergeNode]) {
    def key: SPNodeKey = z.getLabel.key

    def findNode(k: SPNodeKey): TryVcs[TreeLoc[MergeNode]] =
      z.find(_.key === k).toTryVcs(s"Could not find descendant $k")

    def incr(lifespanId: LifespanId): TryVcs[TreeLoc[MergeNode]] =
      mModifyLabel(m => m.copy(nv = m.nv.incr(lifespanId)))

    // Determine the index of the focused node in its parent, if any.
    def childIndex: TryVcs[Int] =
      z.parent.map { _.tree.subForest.indexWhere(_.key === key) }.filter(_ >= 0).toTryVcs(s"Could not find child index of $key")

    // Ensure that the node at the focus is a `Modified` node, converting an
    // `Unmodified` node to `Modified` if necessary.  It should be the case that
    // all `Unmodified` nodes appear in the `nodeMap`.  If not, the conversion
    // will fail with a left.
    def asModified(nodeMap: Map[SPNodeKey, ISPNode]): TryVcs[TreeLoc[MergeNode]] =
      mapAsModified(nodeMap)(identity)

    // Update the given node, converting it to a Modified node if necessary.
    // Sorry, this is a bit awkward.
    def mapAsModified(nodeMap: Map[SPNodeKey, ISPNode])(f: Modified => Modified): TryVcs[TreeLoc[MergeNode]] =
      z.getLabel match {
        case m: Modified   =>
          TryVcs(z.modifyLabel(_ => f(m)))

        case Unmodified(k) =>
          nodeMap.get(k).toTryVcs(s"Unmodified node $k not found in node map.").map { n =>
            z.modifyTree { _ =>
              val mod = f(Modified(n.key, n.getVersion, n.getDataObject, NodeDetail(n), n.getConflicts))
              Node(mod, n.children.map(c => unmodified(c).leaf).toStream)
            }
          }
      }

    def mModifyLabel(f: Modified => Modified): TryVcs[TreeLoc[MergeNode]] =
      z.tree.mModifyLabel(f).map { t => z.modifyTree(_ => t) }

    def getOrCreateConflictFolder(lifespanId: LifespanId, nodeMap: Map[SPNodeKey, ISPNode]): TryVcs[TreeLoc[MergeNode]] = {
      def isConflictFolder(t: Tree[MergeNode]): Boolean =
        t.rootLabel match {
          case Modified(_,_, _: ConflictFolder, _, _) => true
          case Unmodified(k)                          => nodeMap.get(k).exists { n =>
            n.getDataObject.getType == SPComponentType.CONFLICT_FOLDER
          }
          case _                                      => false
        }

      //noinspection MutatorLikeMethodIsParameterless
      def addConflictFolder: TryVcs[TreeLoc[MergeNode]] = {
        val k = new SPNodeKey
        val m = modified(k, EmptyNodeVersions.incr(lifespanId), new ConflictFolder, NodeDetail.Empty, Conflicts.EMPTY.withConflictNote(new Conflict.ConflictFolder(k)))
        incr(lifespanId).map(_.insertDownFirst(m.leaf))
      }

      z.findChild(isConflictFolder).fold(addConflictFolder)(_.asModified(nodeMap))
    }

    def addDataObjectConflict(dob: ISPDataObject): TryVcs[TreeLoc[MergeNode]] =
      mModifyLabel { _.withDataObjectConflict(dob) }

    def addConflictNote(n: Conflict.Note): TryVcs[TreeLoc[MergeNode]] =
      mModifyLabel { _.withConflictNote(n) }
  }

  private def showNodeVersions(nv: NodeVersions): String =
    nv.clocks.keySet.toList.sortBy(_.toString).map { k =>
      s"${k.toString.take(4)} -> ${nv(k)}"
    }.mkString("{", ", ", "}")

  private def showDetail(nd: NodeDetail): String =
    nd match {
      case NodeDetail.Empty  => ""
      case NodeDetail.Obs(n) => s"Obs($n)"
    }

  implicit val ShowNode = Show.shows[MergeNode] {
    case Modified(k, nv, dob, det, con) => s"m $k (${dob.getType}) ${showNodeVersions(nv)} ${showDetail(det)} ${con.shows}"
    case Unmodified(k)                  => s"u $k"
  }

  def draw(t: Tree[MergeNode]): String =
    t.drawTree.zipWithIndex.collect { case (s0, n) if n % 2 == 0 => s0 }.mkString("\n")
}

