package edu.gemini.pit.ui.action

import edu.gemini.model.p1.immutable.ProposalIo
import edu.gemini.pit.model.Model
import edu.gemini.ui.workspace.scala.RichShell

import swing.Dialog

/**
 * Validate the proposal upon demand.
 */
class ValidateAction(shell: RichShell[Model]) extends ShellAction(shell, "Validate Proposal") {// with Bound[Model, Proposal] {
  override def apply() {
    shell.model foreach { m =>
      val (msg, tipe) = ProposalIo.validate(m.proposal) match {
        case Right(_)  => ("Proposal Validates.", Dialog.Message.Info)
        case Left(err) => ("Validation Error: " + err, Dialog.Message.Error)
      }
      Dialog.showMessage(null, msg, "Proposal Validation Result", tipe)
    }
  }
}