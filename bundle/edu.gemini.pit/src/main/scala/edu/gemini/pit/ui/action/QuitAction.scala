package edu.gemini.pit.ui.action

import java.awt.event.KeyEvent

import edu.gemini.ui.workspace.scala._
import edu.gemini.pit.model.Model

class QuitAction(shell: RichShell[Model]) extends ShellAction(shell, "Quit", Some(KeyEvent.VK_Q)) {

  override def apply() {

    // This will close each shell, prompting a yes/no/cancel option for those with unsaved changes.
    // If no shells are left after this process, the workspace will shut down the OSGi container.
    enrichShellContext(shell.context).workspace.close()

  }

}
