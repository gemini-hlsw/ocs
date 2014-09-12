package edu.gemini.spModel.rich.pot.sp

import edu.gemini.pot.sp.{ISPContainerNode, ISPNode}
import edu.gemini.spModel.data.ISPDataObject
import scala.collection.JavaConverters._

class RichRemoteNode(node: ISPNode) {
  def dataObject: Option[ISPDataObject] = Option(node.getDataObject.asInstanceOf[ISPDataObject])

  def dataObject_=(obj: ISPDataObject) {
    node.setDataObject(obj)
  }

  def dataObject_=(obj: Option[ISPDataObject]) {
    node.setDataObject(obj.orNull)
  }

  def title: String =
    (for {
      d <- dataObject
      t <- Option(d.getTitle)
    } yield t).getOrElse("")

  def title_=(t: String) {
    dataObject foreach { obj =>
      obj.setTitle(t)
      node.setDataObject(obj)
    }
  }

  def children: List[ISPNode] = node match {
    case c: ISPContainerNode => c.getChildren.asScala.toList
    case _ => Nil
  }

  def children_=(lst: List[ISPNode]) {
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
        if (!lst.isEmpty) sys.error("Adding children to a non-container node: " + node)
    }
  }

  def toStream: Stream[ISPNode] = node match {
    case c: ISPContainerNode => c #:: c.children.toStream.map(_.toStream).flatten
    case n => Stream(n)
  }

  /**
   * Does a DFS search starting at the tree rooted by this node and stopping at
   * the first descendant for which the predicate matches.
   * @param p predicate applied to each node
   */
  def findDescendant(p: ISPNode => Boolean): Option[ISPNode] =
    toStream.find(p)

  /*
  {
    def dfs0(c: List[ISPNode]): Option[ISPNode] = c match {
      case h :: t => h.findDescendant(p) orElse dfs0(t)
      case _      => None
    }

    if (p(node)) Some(node)
    else dfs0(children)
  }
  */

  /**
   * Searches the tree up from the current node all the way to the root of the
   * science program finding the first ancestor for which the predicate matches.
   * @param p predicate applied to each node
   */
  def findAncestor(p: ISPNode => Boolean): Option[ISPNode] =
    if (p(node)) Some(node)
    else Option(node.getParent).flatMap(_.findAncestor(p))

  /*  Maybe just use toStream ...
  def exists(p: ISPNode => Boolean): Boolean = p(node) || (node match {
    case c: ISPContainerNode => c.children exists { _.exists(p) }
    case _ => false
  })

  def forall(p: ISPNode => Boolean): Boolean = !node.exists(n => !p(n))
  */
}
