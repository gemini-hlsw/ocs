package jsky.app.ot.viewer.action

import edu.gemini.pot.client.SPDB

import jsky.app.ot.OT
import jsky.app.ot.vcs.{SyncAllDialog, VcsGui, VcsIcon}
import jsky.app.ot.viewer.SPViewer

import java.awt.event.ActionEvent
import javax.swing.Action

final class SyncAllAction(viewer: SPViewer) extends AbstractViewerAction(viewer, "Sync All", VcsIcon.UpToDate) {
  putValue(AbstractViewerAction.SHORT_NAME, "Sync All")
  putValue(Action.SHORT_DESCRIPTION, "Sync all programs that you've checked out from a remote database.")

  override def actionPerformed(evt: ActionEvent): Unit =
    VcsGui.registrar.foreach { reg =>
      SyncAllDialog.syncAll(viewer, OT.getKeyChain, reg, SPDB.get())
    }

  override def computeEnabledState: Boolean = VcsGui.registrar.isDefined
}
