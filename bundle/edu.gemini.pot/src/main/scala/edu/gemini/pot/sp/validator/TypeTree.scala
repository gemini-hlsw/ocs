package edu.gemini.pot.sp.validator

import edu.gemini.pot.sp._
import collection.JavaConverters._

case class TypeTree(node: NodeType[_ <: ISPNode], key:Option[SPNodeKey], children: List[TypeTree]) {
  def insert(key:SPNodeKey, n:NodeType[_ <: ISPNode]): TypeTree =
    insert(key, TypeTree(n, None, Nil))

  def insert(key:SPNodeKey, t:TypeTree): TypeTree =
    if (this.key.exists(_ == key)) copy(children = t :: children)
    else copy(children = children.map(_.insert(key, t)))

  def withoutKeys: TypeTree = copy(key = None, children = children.map(_.withoutKeys))

  private def show(indent: Int): String =
    s"${" " * indent}$node (${key.getOrElse("")}})\n${children.map(_.show(indent + 2)).mkString("\n")}"

  def show: String = show(0)
}

object TypeTree {
  def apply(n: ISPNode): TypeTree = {
    val children = n match {
      case c : ISPContainerNode => c.getChildren.asScala.toList.map(apply)
      case _                    => Nil
    }
    new TypeTree(NodeType.forNode(n), Option(n.getNodeKey), children)
  }

  private def apply(n: ISPNode, childTypeTrees: List[TypeTree]): TypeTree =
    TypeTree(NodeType.forNode(n), Option(n.getNodeKey), childTypeTrees)

  /**
   * Calculate a trimmed TypeTree with respect to the given contextKey.  The
   * contextKey specifies a node potentially contained in the tree rooted at
   * 'root'.  The idea is to return a trimmed TypeTree that can be used for
   * quick validations.  We trim away extra observations and groups of
   * observations that can have no impact on the node identified by contextKey
   * (which is typically the selected program node in the Observing Tool).
   */
  def validationTree(root: ISPNode, contextKey: SPNodeKey): Option[TypeTree] = {
    def canTrim(n: ISPNode): Boolean =
      n match {
        case _: ISPGroupContainer       => true
        case _: ISPObservationContainer => true
        case _                          => false
      }

    if (root.getNodeKey == contextKey) {
      Some(if (canTrim(root)) TypeTree(root, Nil) else TypeTree(root))
    } else root match {
      case c: ISPContainerNode =>
        val children = c.getChildren.asScala.toList
        (Option.empty[TypeTree]/:children) { (o, child) =>
          o.orElse(validationTree(child, contextKey).map { tt =>
            val childTypeTrees =
              if (canTrim(root)) List(tt)
              else children.map { child0 => if (child == child0) tt else apply(child0) }
            TypeTree(root, childTypeTrees)
          })
        }
      case _ => None
    }
  }
}