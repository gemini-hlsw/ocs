package jsky.app.ot.vcs

import edu.gemini.pot.sp.ISPNode
import edu.gemini.spModel.rich.pot.sp._

/**
 * Support for finding the next and previous conflicts.
 */
object ConflictNavigator {
  private def nextConflictNode(nodes: Stream[ISPNode], selected: ISPNode, direction: String): Option[ISPNode] = {
    val (before, after) = nodes.span(_.getNodeKey != selected.getNodeKey)

    def nextAfter: Option[ISPNode] = after match {
      case _ #:: tail => tail.find(_.hasConflicts)
      case _ => None
    }

    def wrapAround: Option[ISPNode] = before.find(_.hasConflicts)

    nextAfter orElse wrapAround
  }

  def hasConflicts(root: ISPNode): Boolean = root.toStream.exists(_.hasConflicts)

  def allConflictNodes(root: ISPNode): List[ISPNode] =
    root.toStream.filter(_.hasConflicts).toList

  def next(root: ISPNode, selected: ISPNode): Option[ISPNode] =
    nextConflictNode(root.toStream, selected, "next")

  def nextOrNull(root: ISPNode, selected: ISPNode): ISPNode =
    next(root, selected).orNull

  def prev(root: ISPNode, selected: ISPNode): Option[ISPNode] =
    nextConflictNode(root.toStream.reverse, selected, "previous")

  def prevOrNull(root: ISPNode, selected: ISPNode): ISPNode =
    prev(root, selected).orNull
}
