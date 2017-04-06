package edu.gemini.spModel.rich.pot.sp

import edu.gemini.pot.sp.{SPNodeKey, ISPContainerNode, ISPNode}
import edu.gemini.spModel.data.ISPDataObject

import scala.annotation.tailrec
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

final class RichNode(val node: ISPNode) extends AnyVal {
  /** An alias for `getNodeKey`. */
  def key: SPNodeKey = node.getNodeKey

  def dataObject: Option[ISPDataObject] = Option(node.getDataObject)

  def dataObject_=(obj: ISPDataObject): Unit =
    node.setDataObject(obj)

  def dataObject_=(obj: Option[ISPDataObject]): Unit =
    node.setDataObject(obj.orNull)

  def title: String =
    ~(for(d <- dataObject; t <- Option(d.getTitle)) yield t)

  def title_=(t: String): Unit =
    dataObject.foreach { obj =>
      obj.setTitle(t)
      node.setDataObject(obj)
    }

  def children: List[ISPNode] =
    node match {
      case c: ISPContainerNode => c.getChildren.asScala.toList
      case _                   => Nil
    }

  def children_=(lst: List[ISPNode]): Unit =
    node match {
      case c: ISPContainerNode =>
        // For all new children in the list that are currently children of other
        // containers, first remove them from their container nodes.

        // Get lists of children that share some other parent
        val parentMap = lst.filter(child => Option(child.getParent).exists(_ != c)).groupBy(_.getParent.key)
        parentMap.values.foreach { childList =>
          val parent    = childList.head.getParent
          val childKeys = childList.map(_.key).toSet

          // remove them from their current parent
          parent.setChildren(parent.children.filter(c => !childKeys.contains(c.key)).asJava)
        }

        c.setChildren(lst.asJava)
      case _ =>
        // TODO: this seems a bit poor
        if (lst.nonEmpty) sys.error("Adding children to a non-container node: " + node)
    }

  def toStream: Stream[ISPNode] =
    node match {
      case c: ISPContainerNode => c #:: c.children.toStream.map(_.toStream).flatten
      case n                   => Stream(n)
    }

  def nel: NonEmptyList[ISPNode] = {
    val s = toStream
    NonEmptyList(s.head, s.tail: _*)  // always contains the current node ...
  }

  /**
   * Does a DFS search starting at the tree rooted by this node and stopping at
   * the first descendant for which the predicate matches.
   * @param p predicate applied to each node
   */
  def findDescendant(p: ISPNode => Boolean): Option[ISPNode] =
    toStream.find(p)

  def ancestors: Stream[ISPNode] =
    Option(node.getParent).fold(Stream.empty[ISPNode]) { p => p #:: p.ancestors }

  /**
   * Searches the tree up from the current node all the way to the root of the
   * science program finding the first ancestor for which the predicate matches.
   * @param p predicate applied to each node
   */
  def findAncestor(p: ISPNode => Boolean): Option[ISPNode] =
    ancestors.find(p)

  /** Performs a pre-order depth-first traversal over the program tree,
    * combining nodes to produce the result according to `op`.
    */
  def fold[A](z: A)(op: (A, ISPNode) => A): A = {
    @tailrec def go(rem: List[ISPNode], res: A): A =
      rem match {
        case Nil     => res
        case n :: ns => go(n.children ++ ns, op(res, n))
      }

    go(List(node), z)
  }

  def exists(p: ISPNode => Boolean): Boolean = {
    @tailrec def go(rem: List[ISPNode]): Boolean =
      rem match {
        case Nil     => false
        case n :: ns => p(n) || go(n.children ++ ns)
      }
    go(List(node))
  }

  def forall(p: ISPNode => Boolean): Boolean = !exists(!p(_))

  /** Gets a DFS-ordered program node Zipper focused at this node, filtered by
    * the given filter. If the focus node doesn't pass the filter, the next
    * closest node to the right is selecting, wrapping around if necessary.
    */
  def zipper(filter: ISPNode => Boolean): Option[Zipper[ISPNode]] = {
    def go(root: ISPNode): Option[Zipper[ISPNode]] = {
      val (l,r) = root.toStream.span(_.getNodeKey =/= node.getNodeKey)
      val lʹ    = l.filter(filter)
      val rʹ    = r.filter(filter)

      (lʹ, rʹ) match {
        case (_, h #:: t) => Some(Zipper(lʹ.reverse, h, t ))
        case (h #:: t, _) => Some(Zipper(Stream.empty,  h, t))
        case _            => None
      }
    }

    for {
      p <- Option(node.getProgram)
      z <- go(p)
    } yield z
  }
}
