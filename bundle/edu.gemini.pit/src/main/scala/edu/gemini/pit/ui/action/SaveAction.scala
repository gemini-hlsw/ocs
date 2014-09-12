package edu.gemini.pit.ui.action

import java.awt.event.KeyEvent

import edu.gemini.ui.workspace.scala.RichShell
import javax.swing.JOptionPane
import edu.gemini.pit.model.Model
import javax.swing.JOptionPane._
import scala.Some

class SaveAction(shell:RichShell[Model], newFile:Boolean = false) extends ShellAction(shell, "Save", Some(KeyEvent.VK_S)) {

  enabledWhen {
    true
//    shell.isModified || !shell.canUndo // handle initial model
  }

  override def apply() {
    applyBoolean()
  }

  // This is used by the SubmitView; if save fails then submission won't happen.
  def applyBoolean():Boolean = {
    for (m <- shell.model) yield {
      shell.file match {
        case None    => new SaveAsAction(shell).applyBoolean()
        case Some(f) => try {
          def confirmOverwrite(wasRolled:Boolean):Boolean = (!wasRolled || newFile) || YES_OPTION == showConfirmDialog(shell.peer,
            "The current proposal is from a previous version of Gemini PIT.\n" +
            "Saving or Submitting will update it and you won't be able to open it with older versions of PIT.\n" +
            "Do you want to update?",
            "Confirm Update", WARNING_MESSAGE, YES_NO_OPTION)
          if (confirmOverwrite(shell.isRolled )) {
            m.save(f)
            shell.checkpoint()
            true
          } else {
            false
          }
        } catch {
          case e:Exception =>
            e.printStackTrace()
            alert(e.getMessage)
            false
        }
      }
    }
  } getOrElse false


  private def alert(s:String) {
    JOptionPane.showMessageDialog(shell.peer, s, "Open Failed", JOptionPane.ERROR_MESSAGE)
  }

}
