package edu.gemini.pit.ui.action

import java.awt.event.KeyEvent

import edu.gemini.ui.workspace.scala.RichShell
import edu.gemini.pit.model.Model
import java.io.File

class NewAction(shell: RichShell[Model], handler: ((Model, Option[File]) => Unit)) extends ShellAction(shell, "New", Some(KeyEvent.VK_N)) {

  override def apply() {
    handler(Model.empty, None)
  }
  
  //    // TODO: close first empty window
  //    if (shell.model.isDefined) new CloseAction(shell)()
  //    if (shell.model.isEmpty) shell.model = Some(Model.empty)
  //  }

}
