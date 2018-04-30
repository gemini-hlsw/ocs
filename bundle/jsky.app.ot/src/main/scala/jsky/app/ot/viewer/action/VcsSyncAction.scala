package jsky.app.ot.viewer.action

import edu.gemini.pot.sp.version._
import edu.gemini.shared.util.VersionComparison
import edu.gemini.shared.util.VersionComparison.{Conflicting, Newer, Older}
import edu.gemini.sp.vcs2.{OptionOps, _}
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.util.security.auth.keychain.Action._
import jsky.app.ot.OT
import jsky.app.ot.vcs.{VcsSyncDialog, VcsOtClient, VcsIcon, VcsPeerSelectionDialog, VcsStateEvent}
import jsky.app.ot.viewer.SPViewer

import java.awt.event.{ActionEvent, KeyEvent}
import javax.swing.Action._
import javax.swing.{Icon, JComponent, KeyStroke}

import scala.swing.{Component, Reactor}
import scalaz.\/
import scalaz.syntax.std.option._

object VcsSyncAction {
  def scalaComponent(evt: ActionEvent): Option[Component] =
    evt.getSource match {
      case j: JComponent            => Some(Component.wrap(j))
      case s: scala.swing.Component => Some(s)
      case _                        => None
    }
}

final class VcsSyncAction(viewer: SPViewer) extends AbstractViewerAction(viewer, "Sync Program Changes", VcsIcon.UpToDate) with Reactor {
  putValue(AbstractViewerAction.SHORT_NAME, "Sync")
  putValue(SHORT_DESCRIPTION, "Sync program with any changes from the database.")
  putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, AbstractViewerAction.platformEventMask()))

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
    \/.fromTryCatchNonFatal(viewer.getParentFrame.getToolBar.isShowPictures) | true

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

  def actionPerformed(evt: ActionEvent): Unit = doAction(evt)

  def doAction(evt: ActionEvent): TryVcs[(ProgramLocationSet, VersionMap)] =
    for {
      pd  <- pid \/> VcsFailure.MissingId
      cl  <- VcsOtClient.ref.toTryVcs("OT not initialized with VcsOtClient")
      comp = VcsSyncAction.scalaComponent(evt)
      _   <- (peer orElse promptAndSetPeer(comp)).toTryVcs("Could not determine peer")
      res <- VcsSyncDialog.open(pd, cl, comp)
    } yield res
}
