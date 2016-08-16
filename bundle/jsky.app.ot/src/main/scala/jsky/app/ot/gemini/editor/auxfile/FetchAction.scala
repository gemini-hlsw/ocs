package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.auxfile.api.{AuxFile, AuxFileException}
import edu.gemini.auxfile.client.AuxFileClient
import edu.gemini.spModel.core.SPProgramID
import jsky.app.ot.ui.util.{ProgressDialog, ProgressModel}
import java.io.File

import scala.swing.{Component, Dialog, FileChooser}
import scala.swing.Swing._
import AuxFileAction.transferListener
import jsky.util.gui.Resources

class FetchAction(c: Component, model: AuxFileModel) extends AuxFileAction("Fetch", c, model) {
  icon    = Resources.getIcon("eclipse/download.gif")
  toolTip = "Download the selected file."

  override def interpret(ex: AuxFileException) =
    s"Sorry, there was a problem fetching files: '${ex.getMessage}'"

  override def currentEnabledState: Boolean = super.currentEnabledState &&
    model.currentSelection.exists(!_.isEmpty)

  def promptForDirectory: Option[File] = {
    val fc = new FileChooser(dirPreference.orNull)
    fc.title = "Choose Store Directory"
    fc.fileSelectionMode = FileChooser.SelectionMode.DirectoriesOnly
    fc.multiSelectionEnabled = false
    fc.peer.setApproveButtonMnemonic('s')
    fc.peer.setApproveButtonToolTipText("Store to selected directory.")
    fc.showDialog(c, "Store") match {
      case FileChooser.Result.Approve =>
        val dir = fc.selectedFile
        if (!dir.exists) {
          Dialog.showMessage(c, s"The directory '${dir.getName}' does not exist.", "Missing Directory", Dialog.Message.Error)
          None
        } else if (!dir.canWrite) {
          Dialog.showMessage(c, s"The directory '${dir.getName}' is not writable", "Not Writable", Dialog.Message.Error)
          None
        } else {
          dirPreference = Some(dir)
          Some(dir)
        }
      case _ => None
    }
  }

  private def input: Option[(List[AuxFile], File)] =
    for {
      sel <- model.currentSelection
      dir <- promptForDirectory
    } yield (sel, dir)

  private def fetch(client: AuxFileClient, pid: SPProgramID, sel: AuxFile, dir: File): Boolean = {
    val pm = new ProgressModel(s"Fetching file ${sel.getName}", 100)
    val pd = new ProgressDialog(jFrame.orNull, s"Fetch ${sel.getName}", false, pm)
    onEDT(pd.setVisible(true))

    val localFile = new File(dir, sel.getName)
    try {
      client.fetch(pid, sel.getName, localFile, transferListener(pm))
    } finally {
      onEDT {
        pd.setVisible(false)
        pd.dispose()
      }
    }
  }

  override def apply() {
    exec(input) { (client, pid, input) =>
      val (selection, dir) = input
      (true/:selection) { (t,f) => t && fetch(client, pid, f, dir) }
    }
  }
}
