package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.auxfile.api.AuxFileException
import scala.collection.JavaConverters._
import scala.swing.{TextField, Component}

class DescribeAction(c: Component, model: AuxFileModel, tf: TextField) extends AuxFileAction("Describe", c, model) {
  override def interpret(ex: AuxFileException) =
     s"Sorry, there was a problem adding the description: '${ex.getMessage}'"

   override def currentEnabledState: Boolean = super.currentEnabledState &&
     model.currentSelection.exists(!_.isEmpty)

  override def apply() {
    exec(model.currentSelection) { (client, pid, selection) =>
      client.setDescription(pid, selection.map(_.getName).asJavaCollection, tf.text.trim)
    }
  }
}
