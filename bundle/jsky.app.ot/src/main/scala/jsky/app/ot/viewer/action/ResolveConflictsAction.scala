package jsky.app.ot.viewer.action

import edu.gemini.pot.sp.{Conflicts, ISPNode, ISPConflictFolder, ISPProgram}
import jsky.app.ot.vcs.ConflictNavigator
import jsky.app.ot.viewer.{ViewerManager, SPViewer}
import java.awt.event.ActionEvent
import javax.swing.Action

/**
 * Resolve all conflicts.
 */
object ResolveConflictsAction {
  def resolveAll(p: ISPProgram): Unit = {
    val conflicts = ConflictNavigator.allConflictNodes(p)
    conflicts.foreach {
      case f: ISPConflictFolder =>
        Option(f.getParent).foreach(_.removeConflictFolder())
      case n: ISPNode =>
        n.setConflicts(Conflicts.EMPTY)
    }

    // Clear the conflict window if this is the program being edited
    for {
      v <- ViewerManager.find(p)
      c <- Option(v.getProgram)
    } if (c == p) v.updateConflictToolWindow()
  }
}

import ResolveConflictsAction.resolveAll

class ResolveConflictsAction(viewer: SPViewer) extends AbstractViewerAction(viewer, "Accept all remote conflicts") {
  putValue(Action.SHORT_DESCRIPTION, "Automatically acknowledge and resolve all conflicts in the current program.")

  private def program: Option[ISPProgram] =
    Option(viewer.getCurrentEditor).flatMap(ed => Option(ed.getProgram))

  override def actionPerformed(evt: ActionEvent): Unit = {
    program.foreach { resolveAll }
  }

  override def computeEnabledState: Boolean =
    program.exists(ConflictNavigator.hasConflicts)
}