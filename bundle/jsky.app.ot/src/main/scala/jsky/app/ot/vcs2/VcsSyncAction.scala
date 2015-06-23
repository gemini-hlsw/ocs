package jsky.app.ot.vcs2

import edu.gemini.shared.util.VersionComparison
import edu.gemini.shared.util.VersionComparison.{Newer, Older, Conflicting}
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.util.security.auth.keychain.Action._


import jsky.app.ot.OT
import jsky.app.ot.vcs.{VcsPeerSelectionDialog, VcsIcon, VcsStateEvent}
import jsky.app.ot.viewer.SPViewer
import jsky.app.ot.viewer.action.{AbstractVcsAction, AbstractViewerAction}
import jsky.app.ot.viewer.action.AbstractVcsAction.scalaComponent

import java.awt.event.{ActionEvent, KeyEvent}
import javax.swing.Action._
import javax.swing.Icon

import scala.swing.{Component, Reactor}

import scalaz.\/
import scalaz.syntax.std.option._

final class VcsSyncAction(viewer: SPViewer) extends AbstractViewerAction(viewer, "Sync changes to this program with the database", VcsIcon.UpToDate) with Reactor {
  putValue(AbstractViewerAction.SHORT_NAME, "New Sync")
  putValue(SHORT_DESCRIPTION, "Sync program with any changes from the database.")
  putValue(ACCELERATOR_KEY, AbstractVcsAction.keystroke(KeyEvent.VK_S))

  private def pid: Option[SPProgramID] =
    for {
      r <- Option(viewer.getRoot)
      p <- Option(r.getProgramID)
    } yield p

  private def peer: Option[Peer] =
    for {
      pd <- pid
      c  <- VcsOtClient.ref
      pr <- c.peer(pd)
    } yield pr

  protected def allPeers: List[Peer] =
    OT.getKeyChain.peers.unsafeRun.fold(_ => Nil, _.toList)

  private def promptAndSetPeer(c: Option[Component]): Option[Peer] = {
    pid.foreach { p => VcsPeerSelectionDialog.promptAndSet(c, p, allPeers) }
    peer
  }

  private def isShowIcons: Boolean =
    \/.fromTryCatch(viewer.getParentFrame.getToolBar.isShowPictures) | true

  listenTo(viewer.getVcsStateTracker)

  reactions += {
    case VcsStateEvent(pid, peer, status, conflicts) =>
      def iconFor(s: Option[VersionComparison]): Option[Icon] = s match {
        case None              => Some(VcsIcon.BrokenLink)
        case Some(Conflicting) => Some(VcsIcon.PendingSync)
        case Some(Older)       => Some(VcsIcon.PendingUpdate)
        case Some(Newer)       => Some(VcsIcon.PendingCheckIn)
        case _                 => None
      }

      val newIcon = pid.flatMap { p =>
        peer.fold(Option(VcsIcon.NoPeer: Icon)) { _ => iconFor(status) }
      } | VcsIcon.UpToDate

      if (isShowIcons) putValue(SMALL_ICON, newIcon)

      setEnabled(pid.isDefined && conflicts.isEmpty)
  }

  override def computeEnabledState =
    pid.isDefined && allPeers.size > 0 && viewer.getVcsStateTracker.conflicts.isEmpty

  def actionPerformed(evt: ActionEvent): Unit =
    for {
      pd  <- pid
      cl  <- VcsOtClient.ref
      comp = scalaComponent(evt)
      pr  <- peer orElse promptAndSetPeer(comp)
    } VcsSyncDialog.open(pd, cl, comp)
}
