package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.auxfile.api.AuxFileException
import jsky.util.gui.Resources

import scala.collection.JavaConverters._
import scala.swing.{Component, Dialog}

class RemoveAction(c: Component, model: AuxFileModel) extends AuxFileAction("Remove", c, model) {
  icon    = Resources.getIcon("eclipse/remove.gif")
  toolTip = "Delete the selected file attachment."

  override def interpret(ex: AuxFileException) =
     s"Sorry, there was an error while removing files: '${ex.getMessage}'"

  override def currentEnabledState: Boolean = super.currentEnabledState &&
    model.currentSelection.exists(!_.isEmpty)

  private def confirmed: Boolean = {
    Dialog.showOptions(c, "Remove selected files on the server?", "Remove Files?", Dialog.Options.YesNo, Dialog.Message.Question, null, List("Remove", "Cancel"), 1) match {
      case Dialog.Result.Yes => true
      case Dialog.Result.Ok  => true
      case _ => false
    }
  }

  override def apply() {
    exec(model.currentSelection.filter(_ => confirmed)) { (client, pid, selection) =>
      client.delete(pid, selection.map(_.getName).asJavaCollection)
    }
  }
}
