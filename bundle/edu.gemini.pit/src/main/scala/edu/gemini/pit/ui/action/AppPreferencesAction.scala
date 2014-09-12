package edu.gemini.pit.ui.action

import edu.gemini.ui.workspace.scala.RichShell
import edu.gemini.pit.ui.editor.AppPreferencesEditor
import swing.UIElement
import edu.gemini.pit.model.{AppPreferences, Model}

class AppPreferencesAction(shell: RichShell[Model]) extends ShellAction(shell, "Preferences ...", None, 0) {

  override def apply() {
    for {
      m <- shell.model
      p <- AppPreferencesEditor.open(UIElement.wrap(shell.peer), AppPreferences.current)
    } AppPreferences.current = p
  }

}

