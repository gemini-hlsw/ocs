package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.auxfile.api.AuxFileException
import jsky.util.gui.Resources

import scala.swing.Component

class UpdateAction(c: Component, model: AuxFileModel) extends AuxFileAction("Update", c, model) {
  icon    = Resources.getIcon("eclipse/refresh.gif")
  toolTip = "Refresh the list of file attachments."

  override def interpret(ex: AuxFileException) =
    s"Sorry, an error occurred while updating the file attachment data: '${ex.getMessage}'"

  override def apply() {
    exec(Some(())) { (_, _, _) => () }
  }
}
