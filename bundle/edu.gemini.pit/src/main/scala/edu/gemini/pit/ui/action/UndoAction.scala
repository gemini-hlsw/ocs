package edu.gemini.pit.ui.action

import java.awt.event.KeyEvent

import edu.gemini.ui.workspace.scala.RichShell
import edu.gemini.pit.model.Model

class UndoAction(shell: RichShell[Model]) extends ShellAction(shell, "Undo", Some(KeyEvent.VK_Z)) {

  enabledWhen { shell.canUndo && !shell.model.exists(_.proposal.isSubmitted) }

  override def apply() {
    shell.undo()
  }

}