package jsky.app.ot.viewer.action

import java.util.concurrent.atomic.AtomicBoolean

import edu.gemini.pot.client.SPDB
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.shared.util.VersionComparison._
import edu.gemini.sp.vcs2.VcsAction._
import edu.gemini.sp.vcs2.VcsFailure
import edu.gemini.spModel.core.{ Peer, SPProgramID }
import edu.gemini.spModel.util.DBProgramInfo

import jsky.app.ot.vcs.{ VcsIcon, VcsOtClient, VcsStateEvent }
import jsky.app.ot.viewer.{ SPViewer, ViewerManager }
import java.awt.event.ActionEvent

import javax.swing.Action._
import javax.swing.JComponent

import scala.swing.{ Component, Dialog, Reactor }
/**
  *
  */
final class RevertChangesAction(viewer: SPViewer) extends AbstractViewerAction(viewer, "Revert Changes...", VcsIcon.Revert) with Reactor {
  import RevertChangesAction._

  putValue(AbstractViewerAction.SHORT_NAME, "Revert")
  putValue(SHORT_DESCRIPTION, "Revert changes to this program since the last sync.")

  private def pid: Option[SPProgramID] =
    for {
      r <- Option(viewer.getRoot)
      p <- Option(r.getProgramID)
    } yield p

  listenTo(viewer.getVcsStateTracker)

  reactions += {
    case VcsStateEvent(pid, peer, status, conflicts) =>
      setEnabled(computeEnabledState)
  }

  override def computeEnabledState: Boolean =
    viewer.getVcsStateTracker.currentState.status.exists {
      case Newer | Conflicting => true
      case Same  | Older       => false
    }

  override def actionPerformed(evt: ActionEvent): Unit = {
    def success(p: ISPProgram): Unit =
      ViewerManager.open(p, viewer)

    def fail(pid: SPProgramID, loc: Peer)(f: VcsFailure): Unit = {
      val msg = VcsFailure.explain(f, pid, "revert", Some(loc))
      Dialog.showMessage(scalaComponent(evt).orNull, msg, "Error", Dialog.Message.Error)
    }

    for {
      client  <- VcsOtClient.ref
      program <- Option(viewer.getRoot)
      pid     <- Option(program.getProgramID)
      peer    <- client.peer(pid)
      if client.reg.allRegistrations.isDefinedAt(pid) && userConfirms(evt)
    } {
      client.revert(pid, peer, new AtomicBoolean(false)).unsafeRun.fold(fail(pid, peer),success)
    }
  }

}

object RevertChangesAction {

  private val ConfirmationMessage =
    """This action will delete all local changes made
      |since the last sync and restore the program
      |from the remote database.
      |
      |Continue?
    """.stripMargin

  private def scalaComponent(evt: ActionEvent): Option[Component] =
    evt.getSource match {
      case j: JComponent            => Some(Component.wrap(j))
      case s: scala.swing.Component => Some(s)
      case _                        => None
    }

  private def userConfirms(evt: ActionEvent): Boolean =
    scalaComponent(evt).exists { c =>
      Dialog.showConfirmation(
        c,
        ConfirmationMessage,
        "Confirm Revert Changes",
        Dialog.Options.YesNo,
        Dialog.Message.Warning
      ) == Dialog.Result.Ok
    }

}