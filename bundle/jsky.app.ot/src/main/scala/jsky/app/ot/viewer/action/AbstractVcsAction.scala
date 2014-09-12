package jsky.app.ot.viewer.action

import edu.gemini.sp.vcs.VcsServer
import edu.gemini.spModel.core.{Peer, SPProgramID}
import jsky.app.ot.OT
import jsky.app.ot.vcs.{VcsPeerSelectionDialog, VcsGui, VcsProgressDialog, VcsGuiOp}
import jsky.app.ot.vcs.vm.VmStore
import jsky.app.ot.viewer.SPViewer
import edu.gemini.util.security.auth.keychain.Action._

import java.awt.event.ActionEvent
import javax.swing.{KeyStroke, Icon, JComponent}

import scala.swing.{Swing, Component}
import scalaz._
import Scalaz._

object AbstractVcsAction {
  def scalaComponent(evt: ActionEvent): Option[Component] =
    evt.getSource match {
      case j: JComponent            => Some(Component.wrap(j))
      case s: scala.swing.Component => Some(s)
      case _                        => None
    }

  class TrackerUpdatingGuiOp(viewer: SPViewer, op: VcsGuiOp) extends VcsGuiOp {
    def name = op.name

    def apply(id: SPProgramID, gui: VcsGuiOp.Ui, server: VcsServer): VcsGuiOp.Result = {
      val result = op(id, gui, server)

      result foreach { _ foreach { case (remoteJvm,_) =>
        Swing.onEDT { VmStore.update(id, remoteJvm) }
//        Option(viewer.getVcsStateTracker) foreach { tracker =>
//          tracker.updateRemoteVersions(Some(remoteJvm))
//        }
      }}

      result
    }
  }

  def keystroke(key: Int): KeyStroke =
    KeyStroke.getKeyStroke(key, AbstractViewerAction.platformEventMask())
}

import AbstractVcsAction._

abstract class AbstractVcsAction(viewer: SPViewer, longName: String, icon: Icon, op: VcsGuiOp) extends AbstractViewerAction(viewer, longName, icon) {
  protected def pid: Option[SPProgramID] =
    for {
      r <- Option(viewer.getRoot)
      p <- Option(r.getProgramID)
    } yield p

  protected def peer(p: SPProgramID): Option[Peer] =
    VcsGui.registrar.flatMap(_.registration(p))

  protected def peer: Option[Peer] = pid.flatMap(peer(_))

  protected def allPeers: List[Peer] =
    OT.getKeyChain.peers.unsafeRun.fold(_ => Nil, _.toList)

  // side-effects galore
  private def promptAndSetPeer(c: Option[Component]): Option[Peer] = {
    pid.foreach { p => VcsPeerSelectionDialog.promptAndSet(c, p, allPeers) }
    peer
  }

  override def computeEnabledState: Boolean = pid.isDefined && allPeers.size > 0

  def actionPerformed(evt: ActionEvent): Unit = {
    (peer orElse promptAndSetPeer(scalaComponent(evt))).foreach { _ => doAction(evt) }
  }

  def doAction(evt: ActionEvent): VcsGuiOp.Result =
    Option(viewer.getRoot) flatMap  { prog =>
      VcsProgressDialog.doVcsOp(prog, viewer, scalaComponent(evt), new TrackerUpdatingGuiOp(viewer,op))
    }
}

