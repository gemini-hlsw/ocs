package edu.gemini.pit.ui.action

import java.awt.event.KeyEvent

import edu.gemini.ui.workspace.scala.RichShell
import edu.gemini.pit.model.Model

class RedoAction(shell: RichShell[Model]) extends ShellAction(shell, "Redo", Some(KeyEvent.VK_Y)) {

  enabledWhen { shell.canRedo && !shell.model.exists(_.proposal.isSubmitted) }

  override def apply() {
    shell.redo()
  }

}