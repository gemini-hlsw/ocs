package jsky.app.ot.viewer.action

import edu.gemini.pot.client.SPDB

import jsky.app.ot.OT
import jsky.app.ot.vcs.{VcsOtClient, SyncAllDialog, VcsIcon}
import jsky.app.ot.viewer.SPViewer

import java.awt.event.ActionEvent
import javax.swing.Action

final class SyncAllAction(viewer: SPViewer) extends AbstractViewerAction(viewer, "Sync All", VcsIcon.UpToDate) {
  putValue(AbstractViewerAction.SHORT_NAME, "Sync All")
  putValue(Action.SHORT_DESCRIPTION, "Sync all programs that you've checked out from a remote database.")

  override def actionPerformed(evt: ActionEvent): Unit =
    VcsOtClient.ref.foreach { c =>
      SyncAllDialog.syncAll(viewer, OT.getKeyChain, c.reg, SPDB.get())
    }

  override def computeEnabledState: Boolean = VcsOtClient.ref.isDefined
}
