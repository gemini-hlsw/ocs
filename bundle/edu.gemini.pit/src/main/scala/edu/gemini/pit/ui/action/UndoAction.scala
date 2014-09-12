package edu.gemini.pit.ui.action

import java.awt.event.KeyEvent

import edu.gemini.ui.workspace.scala.RichShell
import edu.gemini.pit.model.Model
import edu.gemini.pit.model.AppMode._

class UndoAction(shell: RichShell[Model]) extends ShellAction(shell, "Undo", Some(KeyEvent.VK_Z)) {

  enabledWhen { shell.canUndo && (isTAC || shell.model.map(!_.proposal.isSubmitted).getOrElse(true)) }

  override def apply() {
    shell.undo()
  }

}