package jsky.app.ot.vcs

import edu.gemini.pot.client.SPDB
import edu.gemini.pot.sp.{ISPNode, ISPProgram}
import edu.gemini.sp.vcs._
import edu.gemini.spModel.core.SPProgramID
import jsky.app.ot.util.Resources
import jsky.app.ot.vcs.VcsGui.ProgramVcsServer
import jsky.app.ot.vcs.VcsGui.VcsServerNotFound
import jsky.app.ot.viewer.SPViewer
import jsky.app.ot.viewer.action.ResolveConflictsAction

import scala.swing._
import scala.swing.event.ActionEvent
import scala.swing.GridBagPanel.Anchor._
import scala.swing.GridBagPanel.Fill._
import scala.swing.Swing._

import java.awt.Color
import javax.swing.UIManager


// SW: this class kind of got out of hand.  I'm sorry.  It needs to be rewritten.

/**
 * Support for performing VCS operations with a modal dialog to show progress.
 */
object VcsProgressDialog {

  /**
   * Performs the given VcsGuiOp on the given node, which must be a science
   * program (or else a warning message is displayed and the operation exits).
   *
   * @param node program node upon which the operation is performed
   * @param comp component relative to which the dialog should be displayed
   * @param op VCS operation to perform
   */
  def doVcsOp(node: ISPNode, viewer: SPViewer, comp: Option[Component], op: VcsGuiOp): VcsGuiOp.Result =
    // If this is a program node and if that program has an id, do the
    // operation.  Otherwise, show a warning message.
    VcsGui.server(node) match {
      case Left(VcsServerNotFound(title, message)) =>
        warn(comp, title, message)
        scala.None
      case Right(ProgramVcsServer(pid, server))  =>
        val d = new VcsProgressDialog(pid, viewer, comp, server, op)
        d.pack()
        d.setLocationRelativeTo(comp.orNull)
        d.visible = true
        d.result
    }

  private def warn(parent: Option[Component], title: String, message: String) {
    Dialog.showMessage(parent = parent.orNull, message = message, title = title,
                       messageType = Dialog.Message.Error)
  }

  private def smallerFont(c: Component, reduction: Int): Font =
      c.font.deriveFont(c.font.getSize2D - reduction)

  // Make a multi-line label by doctoring a JTextArea until it works more or
  // less like a text label.
  private def makeNote(rows: Int, cols: Int, text: String, reduction: Int = 0): TextArea =
    new TextArea(rows, cols) {
      editable = false
      opaque   = false
      lineWrap = true
      wordWrap = true
      font     = smallerFont(this, reduction)
      text     = text
      peer.setHighlighter(null)
    }

  private lazy val separatorBorder =
    CompoundBorder(MatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY), EmptyBorder(10, 0 , 0, 0))
}

import VcsProgressDialog._

class VcsProgressDialog(id: SPProgramID, viewer: SPViewer, comp: Option[Component], server: VcsServer, op: VcsGuiOp) extends Dialog {

  def this(prog: ISPProgram, viewer: SPViewer, comp: Option[Component], server: VcsServer, op: VcsGuiOp) {
    this(prog.getProgramID, viewer, comp, server, op)
  }

  title     = "Update %s".format(id)
  modal     = true
  resizable = false

  val dialog = this

  object statusIcon extends Label { icon = Resources.getIcon("spinner16.gif") }

  val statusMsg   = makeNote(1, 35, "")

  object resolveButton extends Button("Accept All Remote") {
    visible = false
  }

  object closeButton extends Button("Cancel") {
    reactions += {
      case e: ActionEvent =>
        text    = "Cancelling..."
        enabled = false
        worker.cancel()
    }
  }

  // The VcsWorker will run in a background thread updating the GUI on the
  // Swing EDT.
  private class VcsWorker extends Runnable {
    val gui = new VcsGuiOp.Ui {
      def isCancelled: Boolean = VcsWorker.this.isCancelled
      def report(msg: String) { publish(msg) }
    }

    private var cancelled: Boolean = false

    def cancel() { synchronized { cancelled = true } }
    def isCancelled: Boolean = synchronized { cancelled }

    def publish(msg: String) {
      Swing.onEDT { statusMsg.text = msg }
    }

    private def done(result: VcsGuiOp.Result) {
      val cancelled = isCancelled

      lazy val conflictedProg = result.flatMap(_.toEither.left.toOption).collect {
        case OldVcsFailure.HasConflict => SPDB.get().lookupProgramByID(id)
      }

      lazy val errorMessage = result.flatMap(_.toEither.left.toOption map { failure =>
        op.explanation.lift(failure).getOrElse(OldVcsFailure.explain(failure, id, op.name, VcsGui.peer(id)))
      })

      Swing.onEDT {
        if (cancelled) closeAndDispose(scala.None)
        else if (errorMessage.isEmpty) closeAndDispose(result)
        else {
          conflictedProg.foreach { p =>
            resolveButton.visible = true
            resolveButton.reactions += {
              case e: ActionEvent =>
                ResolveConflictsAction.resolveAll(p)
                closeAndDispose(result)

                // try again but let this instance die :-/
                Swing.onEDT {
                  VcsProgressDialog.doVcsOp(p, viewer, comp, op)
                }
            }
          }

          val message = conflictedProg.fold(errorMessage.get) { _ =>
            "Your program has been updated but it contains edits that conflict with the database. Press 'Accept All Remote' to automatically accept all the remote changes, or 'Cancel and Review' to review the conflicts."
          }
          conflictedProg.foreach { _ =>
            statusMsg.rows = 4
            dialog.pack()
          }

          val label = conflictedProg.fold("Ok") { _ => "Cancel and Review" }

          statusIcon.icon  = UIManager.getIcon("OptionPane.warningIcon")
          statusMsg.text   = message
          closeButton.text = label
          closeButton.reactions += { case e: ActionEvent => closeAndDispose(result) }
        }
      }
    }

    def run() {
      done(op(id, gui, server))
    }
  }

  private val worker = new VcsWorker
  var result: VcsGuiOp.Result = scala.None

  private def closeAndDispose(res: VcsGuiOp.Result) {
    result = res
    close()
    dispose()
  }

  private val statusPanel = new GridBagPanel {
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

  private val buttonPanel = new GridBagPanel {
    border = separatorBorder

    layout(HGlue) = new Constraints() {
      gridx   = 0
      weightx = 1.0
      fill    = Horizontal
    }

    layout(resolveButton) = new Constraints() {
      gridx = 1
      insets = new Insets(0,0,0,5)
    }

    layout(closeButton) = new Constraints() { gridx  = 2 }
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

  new Thread(worker, "VCS Worker: " + id).start()
}
