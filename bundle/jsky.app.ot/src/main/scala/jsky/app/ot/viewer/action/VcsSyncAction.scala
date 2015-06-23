package jsky.app.ot.viewer.action

import jsky.app.ot.vcs._
import jsky.app.ot.vcs.VcsStateEvent
import edu.gemini.sp.vcs.ProgramStatus
import ProgramStatus._
import jsky.app.ot.viewer.SPViewer

import java.awt.event.KeyEvent
import javax.swing.Icon
import javax.swing.Action.{ACCELERATOR_KEY, SHORT_DESCRIPTION, SMALL_ICON}
import scala.util.Try

import scala.swing.Reactor

// NOTE: This class is slated for deletion in favor of vcs2.VcsSyncAction..

/**
 * Updates program from the remote database and then commits local changes if
 * there were no conflicts.
 */
final class VcsSyncAction(viewer: SPViewer) extends AbstractVcsAction(viewer, "Sync changes to this program with the database", VcsIcon.UpToDate, VcsSyncOp) with Reactor {
  putValue(AbstractViewerAction.SHORT_NAME, "Old Sync")
  putValue(SHORT_DESCRIPTION, "Sync program with any changes from the database.")
  putValue(ACCELERATOR_KEY, AbstractVcsAction.keystroke(KeyEvent.VK_S))

  private def isShowIcons: Boolean =
    Try { viewer.getParentFrame.getToolBar.isShowPictures }.getOrElse(true)

  listenTo(viewer.getVcsStateTracker)
  reactions += {
    case VcsStateEvent(pid, peer, status, conflicts) =>
      def iconFor(s: ProgramStatus): Option[Icon] = s match {
        case Unknown        => Some(VcsIcon.BrokenLink)
        case PendingSync    => Some(VcsIcon.PendingSync)
        case PendingUpdate  => Some(VcsIcon.PendingUpdate)
        case PendingCheckIn => Some(VcsIcon.PendingCheckIn)
        case _              => None
      }

      val newIcon = pid.flatMap { p =>
        peer.fold[Option[Icon]](Some(VcsIcon.NoPeer)) { _ => None } // TODO: delete me
      }.getOrElse(VcsIcon.UpToDate)

      if (isShowIcons) putValue(SMALL_ICON, newIcon)

      setEnabled(pid.isDefined && conflicts.isEmpty)
  }

  override def computeEnabledState =
    super.computeEnabledState && viewer.getVcsStateTracker.conflicts.isEmpty
}