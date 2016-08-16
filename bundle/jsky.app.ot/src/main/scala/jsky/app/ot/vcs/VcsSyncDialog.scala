package jsky.app.ot.vcs

import edu.gemini.pot.client.SPDB
import edu.gemini.pot.sp.version.VersionMap
import edu.gemini.sp.vcs2._
import edu.gemini.sp.vcs2.VcsAction.VcsActionOps
import edu.gemini.sp.vcs2.VcsFailure.{Cancelled, HasConflict, NotFound}
import edu.gemini.spModel.core.SPProgramID
import jsky.app.ot.viewer.action.ResolveConflictsAction
import java.awt.Color
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.UIManager

import jsky.util.gui.Resources

import scala.swing.GridBagPanel.Anchor.West
import scala.swing.GridBagPanel.Fill.Horizontal
import scala.swing.Swing._
import scala.swing._
import scala.swing.event.ActionEvent
import scalaz.Scalaz._
import scalaz._


object VcsSyncDialog {
  val ConflictMessage =
    """Your program contains edits that conflict with the database.  Press
      |'Accept All Remote' to automatically accept all the remote changes, or
      |'Cancel and Review' to review the conflicts.
    """.stripMargin.map(c => (c === '\n').fold(' ', c))

  def open(pid: SPProgramID, client: VcsOtClient, comp: Option[Component]): TryVcs[(ProgramLocationSet, VersionMap)] = {
    val d = new VcsSyncDialog(pid, client, comp)
    d.title = s"Sync $pid"
    d.pack()
    d.setLocationRelativeTo(comp.orNull)
    d.visible = true
    d.result | VcsFailure.Cancelled.left
  }

  private def warn(parent: Option[Component], title: String, message: String) {
    Dialog.showMessage(parent = parent.orNull, message = message, title = title,
                       messageType = Dialog.Message.Error)
  }

  // Make a multi-line label by doctoring a JTextArea until it works more or
  // less like a text label.
  private def makeNote(rows: Int, cols: Int, text0: String): TextArea =
    new TextArea(rows, cols) {
      editable = false
      opaque   = false
      lineWrap = true
      wordWrap = true
      text     = text0
      peer.setHighlighter(null)
    }

  private lazy val separatorBorder =
    CompoundBorder(MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY), EmptyBorder(10, 0 , 0, 0))
}

class VcsSyncDialog(pid: SPProgramID, client: VcsOtClient, comp: Option[Component]) extends Dialog { dialog =>

  import VcsSyncDialog._

  modal         = true
  resizable     = false

  var result: Option[TryVcs[(ProgramLocationSet, VersionMap)]] = None

  val cancelled: AtomicBoolean = new AtomicBoolean(false)

  object statusIcon extends Label { icon = Resources.getIcon("spinner16.gif") }

  val statusMsg = makeNote(1, 35, s"Synchronizing $pid ...")

  object resolveButton extends Button("Accept All Remote") {
    visible = false
  }

  object closeButton extends Button("Cancel") {
    reactions += {
      case e: ActionEvent =>
        enabled = false
        cancelled.set(true)
        closeAndDispose(-\/(Cancelled))
    }
  }

  def closeAndDispose(res: TryVcs[(ProgramLocationSet, VersionMap)]): Unit = {
    result = Some(res)
    close()
    dispose()
  }

  val handleResult: TryVcs[(ProgramLocationSet, VersionMap)] => Unit = {
    case \/-(a)           =>
      closeAndDispose(\/-(a))

    case -\/(Cancelled)   =>
      closeAndDispose(-\/(Cancelled))

    case -\/(NotFound(_)) =>
      client.peer(pid).foreach { peer =>
        client.add(pid, peer).forkAsync(vm => Swing.onEDT(handleResult(vm.map((ProgramLocationSet.RemoteOnly, _)))))
      }

    case -\/(HasConflict) =>
      Option(SPDB.get().lookupProgramByID(pid)).foreach { p =>
        statusIcon.icon  = UIManager.getIcon("OptionPane.warningIcon")
        statusMsg.rows   = 4
        statusMsg.text   = ConflictMessage
        closeButton.text = "Cancel and Review"

        resolveButton.visible = true
        resolveButton.reactions += {
          case _: ActionEvent =>
            ResolveConflictsAction.resolveAll(p)
            closeAndDispose(-\/(HasConflict))

            // Try again but let this instance die
            Swing.onEDT { VcsSyncDialog.open(pid, client, comp) }
        }

        dialog.pack()
      }

    case -\/(failure)     =>
      statusIcon.icon  = UIManager.getIcon("OptionPane.warningIcon")
      statusMsg.text   = VcsFailure.explain(failure, pid, "sync", client.peer(pid))
      closeButton.text = "Ok"
      closeButton.reactions += { case _: ActionEvent => closeAndDispose(-\/(failure)) }
  }

  object statusPanel extends GridBagPanel {
    layout(statusIcon) = new Constraints() {
      gridx  = 0
      gridy  = 0
      anchor = West
    }

    layout(statusMsg) = new Constraints() {
      gridx   = 1
      anchor  = West
      fill    = Horizontal
      weightx = 1.0
      insets  = new Insets(0,5,0,0)
    }
  }

  object buttonPanel extends GridBagPanel {
    border = separatorBorder

    layout(HGlue) = new Constraints() {
      gridx   = 0
      weightx = 1.0
      fill    = Horizontal
    }

    layout(resolveButton) = new Constraints() {
      gridx  = 1
      insets = new Insets(0,0,0,5)
    }

    layout(closeButton) = new Constraints() {
      gridx = 2
    }
  }

  contents = new GridBagPanel {
    border = EmptyBorder(20,10,10,10)

    layout(statusPanel) = new Constraints() {
      gridy  = 0
      anchor = West
    }

    layout(buttonPanel) = new Constraints() {
      gridy   = 1
      fill    = Horizontal
      weightx = 1.0
      insets  = new Insets(10,0,0,0)
    }
  }

  peer.getRootPane.setDefaultButton(closeButton.peer)

  client.sync(pid, cancelled).forkAsync(r => Swing.onEDT(handleResult(r)))
}
