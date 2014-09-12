package jsky.app.ot.viewer.action

import jsky.app.ot.viewer.SPViewer
import jsky.app.ot.vcs.{VcsCommitOp, VcsIcon}

import java.awt.event.KeyEvent
import javax.swing.Action

/**
 * Commits changes to the remote database.
 */
final class VcsCommitAction(viewer: SPViewer) extends AbstractVcsAction(viewer, "Store changes to program", VcsIcon.Commit, VcsCommitOp) {
  putValue(AbstractViewerAction.SHORT_NAME, "Store")
  putValue(Action.SHORT_DESCRIPTION, "Store changes to this program to the database.")

  override def computeEnabledState =
    super.computeEnabledState && viewer.getVcsStateTracker.conflicts.isEmpty
}
