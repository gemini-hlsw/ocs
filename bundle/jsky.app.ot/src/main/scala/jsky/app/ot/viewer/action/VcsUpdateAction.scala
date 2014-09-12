package jsky.app.ot.viewer.action

import jsky.app.ot.viewer.SPViewer
import jsky.app.ot.vcs.{VcsUpdateOp, VcsIcon}

import java.awt.event.KeyEvent
import javax.swing.Action

/**
 * Updates the local program with any changes in the remote database.
 */
final class VcsUpdateAction(viewer: SPViewer) extends AbstractVcsAction(viewer, "Fetch updates to program", VcsIcon.Update, VcsUpdateOp) {
  putValue(AbstractViewerAction.SHORT_NAME, "Fetch")
  putValue(Action.SHORT_DESCRIPTION, "Fetch updates to program from the database.")
}
