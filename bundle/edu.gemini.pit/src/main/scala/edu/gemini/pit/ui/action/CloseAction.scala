package edu.gemini.pit.ui.action

import java.awt.event.KeyEvent

import edu.gemini.ui.workspace.scala.RichShell
import edu.gemini.pit.model.Model

class CloseAction(shell: RichShell[Model]) extends ShellAction(shell, "Close", Some(KeyEvent.VK_W)) {

  override def apply() {
    shell.close()
  }

}
