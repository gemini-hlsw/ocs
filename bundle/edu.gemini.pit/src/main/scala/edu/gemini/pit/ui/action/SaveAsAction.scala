package edu.gemini.pit.ui.action

import java.awt.event.InputEvent
import java.awt.event.KeyEvent

import edu.gemini.ui.workspace.scala.RichShell
import edu.gemini.pit.model.Model
import edu.gemini.shared.gui.Chooser

class SaveAsAction(shell: RichShell[Model]) extends ShellAction(shell, "Save As...", Some(KeyEvent.VK_S), InputEvent.SHIFT_DOWN_MASK) {

  enabledWhen { shell.model.isDefined }

  override def apply() {
    applyBoolean()
  }

  // This is used by the SubmitView and is called from SaveAction; if save fails then submission won't happen.
  def applyBoolean():Boolean = {
    for {
      m <- shell.model
      f <- new Chooser[SaveAsAction]("defaultDir", shell.peer).chooseSave("Proposal Documents", ".xml")
    } yield {
      shell.file = Some(f)
      new SaveAction(shell, true).applyBoolean()
    }
  } getOrElse false

}

