package jsky.app.ot.viewer.action

import edu.gemini.pot.sp.{Conflicts, ISPNode}
import jsky.app.ot.vcs.{VcsStateEvent, VcsIcon, ConflictNavigator}
import jsky.app.ot.viewer.SPViewer

import javax.swing.{KeyStroke, Action, Icon}
import java.awt.event.{InputEvent, ActionEvent, KeyEvent}
import scala.swing.{Component, Dialog, Reactor}

object VcsShowConflictAction {
  sealed trait Direction {
    def name: String
    def icon: Icon
    def key: Int
    def preposition: String
    def find: (ISPNode, ISPNode) => Option[ISPNode]
    def title: String = "%s Conflict".format(name.capitalize)
  }

  case object Prev extends Direction {
    val name = "prev"
    val icon = VcsIcon.ConflictPrev
    val key  = KeyEvent.VK_P
    val preposition = "before"
    val find = ConflictNavigator.prev _
  }

  case object Next extends Direction {
    val name = "next"
    val icon = VcsIcon.ConflictNext
    val key  = KeyEvent.VK_N
    val preposition = "after"
    val find = ConflictNavigator.next _
  }
}

import VcsShowConflictAction._

abstract class VcsShowConflictAction(dir: Direction, viewer: SPViewer) extends AbstractViewerAction(viewer, dir.title, dir.icon) with Reactor {
  putValue(Action.SHORT_DESCRIPTION, "Show the %s conflict, if any, %s the selected node.".format(dir.name, dir.preposition))
  putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(dir.key, AbstractViewerAction.platformEventMask() | InputEvent.SHIFT_DOWN_MASK))
  putValue(AbstractViewerAction.SHORT_NAME, "Conflict")

  listenTo(viewer.getVcsStateTracker)
  reactions += {
    case VcsStateEvent(_,_,_,conflicts) => setEnabled(isEnabledFor(conflicts))
  }

  def actionPerformed(evt: ActionEvent) {
    def warnAndFixInvisibleNode(invisibles: List[ISPNode]): Boolean = {
      val parent  = Component.wrap(viewer)
      val message = "You have conflicting changes in a node that you are not allowed to edit.  Click OK to accept the database version."
      val title   = "Edit Permission Issue"
      if (Dialog.showConfirmation(parent, message, title, Dialog.Options.OkCancel, Dialog.Message.Warning) == Dialog.Result.Ok) {
        invisibles.foreach(_.setConflicts(Conflicts.EMPTY))
        true
      } else false
    }

    Option(viewer.getRoot) foreach { root =>
      val tree = viewer.getTree

      // Warn and fix conflicts for any invisible nodes or stop. :-/
      val continue = ConflictNavigator.allConflictNodes(root).filter(n => Option(tree.getTreeNode(n)).isEmpty) match {
        case Nil => true
        case ns  => warnAndFixInvisibleNode(ns)
      }

      if (continue) {
        val selected = Option(viewer.getNode).getOrElse(root)
        dir.find(root, selected) foreach { conflictNode =>
          val treeNode = Option(tree.getTreeNode(conflictNode))
          treeNode.foreach { tn => tree.setSelectedNode(tn) }
        }
      }
    }
  }

  // OCSINF-360.  I'm not a fan of this change, but it was requested to make
  // this button enabled always when there is a conflict, even if there is no
  // "next" conflict.  That way it shows up red as an indicator that there are
  // conflicts.  Unfortunately it also means you can click on it and it takes
  // you back to the same node, which seems like nothing happens. :-/
  private def isEnabledFor(conflicts: List[ISPNode]): Boolean =
    conflicts.size > 0
//  (conflicts.size > 1) || !conflicts.filterNot(_ == viewer.getTree.getSelectedNode).isEmpty

  override def computeEnabledState: Boolean =
    Option(viewer).exists(v => isEnabledFor(v.getVcsStateTracker.conflicts))
}

final class VcsShowPrevConflictAction(viewer: SPViewer) extends VcsShowConflictAction(Prev, viewer)
final class VcsShowNextConflictAction(viewer: SPViewer) extends VcsShowConflictAction(Next, viewer)

