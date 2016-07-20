package edu.gemini.pit.ui.util

import java.awt.event.ActionEvent
import javax.swing.{AbstractAction, Action, KeyStroke}

import edu.gemini.shared.Platform
import edu.gemini.ui.workspace.scala._

abstract class ShellAction[A](shell: RichShell[A], caption: String, key: Option[Int] = None, mask: Int = 0) extends AbstractAction(caption) with (() => Unit) {

  key.foreach { k => putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(k, Platform.MENU_ACTION_MASK | mask)) }

  final def actionPerformed(e: ActionEvent) {
    apply()
  }

  def apply() {
    println("TODO: implement " + getValue(Action.NAME))
  }

  def enabledWhen(f: => Boolean) {
    shell.listen {
      setEnabled(f)
    }
  }

}
