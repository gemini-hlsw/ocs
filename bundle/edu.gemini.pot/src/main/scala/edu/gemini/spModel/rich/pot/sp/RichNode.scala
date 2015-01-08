package edu.gemini.spModel.rich.pot.sp

import edu.gemini.pot.sp.{ISPContainerNode, ISPNode}
import edu.gemini.spModel.data.ISPDataObject

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

final class RichNode(val node: ISPNode) extends AnyVal {
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
        val parentMap = lst.filter(child => Option(child.getParent).exists(_ != c)).groupBy(_.getParent.getNodeKey)
        parentMap.values.foreach { childList =>
          val parent    = childList.head.getParent
          val childKeys = childList.map(_.getNodeKey).toSet

          // remove them from their current parent
          parent.setChildren(parent.children.filter(c => !childKeys.contains(c.getNodeKey)).asJava)
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
}
