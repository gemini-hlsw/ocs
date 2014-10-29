package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.auxfile.api.{AuxFile, AuxFileException}
import jsky.app.ot.OTOptions

import scala.collection.JavaConverters._
import scala.swing.Component

class CheckAction(c: Component, model: AuxFileModel) extends AuxFileAction("Mark Checked", c, model) {
  reactions += {
    case AuxFileStateEvent(e) =>
      title = "Mark " + (if (e.exists(s => !s.selection.isEmpty && allChecked(s.selection))) "Unchecked" else "Checked")
  }

  private def allChecked(lst: List[AuxFile]) = lst.forall(_.isChecked)

  override def interpret(ex: AuxFileException) =
    s"Sorry, there was a problem checking files: '${ex.getMessage}'"

  override def currentEnabledState: Boolean = super.currentEnabledState &&
    model.currentPid.exists(pid => OTOptions.isNGO(pid) || OTOptions.isStaff(pid)) &&
    model.currentSelection.exists(!_.isEmpty)

  override def apply() {
    exec(model.currentSelection) { (client, pid, selection) =>
      client.setChecked(pid, selection.map(_.getName).asJavaCollection, !allChecked(selection))
    }
  }
}
