package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.auxfile.client.AuxFileClient
import edu.gemini.auxfile.api.AuxFileException
import jsky.app.ot.ui.util.{ProgressDialog, ProgressModel}
import java.io.File

import scala.swing._
import scala.swing.Swing.onEDT
import AuxFileAction.transferListener
import jsky.util.gui.Resources

class AddAction(c: Component, model: AuxFileModel) extends AuxFileAction("Add", c, model) {
  icon    = Resources.getIcon("eclipse/add.gif")
  toolTip = "Add a new file attachment."

  override def interpret(ex: AuxFileException) =
    s"Sorry, the was a problem attaching files: '${ex.getMessage}'"

  private def prompt: Option[List[File]] = {
    val fc = new FileChooser(dirPreference.orNull)
    fc.title = "Choose File to Attach (Upload)"
    fc.fileSelectionMode = FileChooser.SelectionMode.FilesOnly
    fc.multiSelectionEnabled = true
    fc.peer.setApproveButtonMnemonic('a')
    fc.peer.setApproveButtonToolTipText("Upload selected file.")
    fc.showDialog(c, "Attach") match {
      case FileChooser.Result.Approve =>
        fc.selectedFiles.toList match {
          case Nil   => None
          case files =>
            dirPreference = Some(files.head.getParentFile)
            Some(files)
        }
      case _ => None
    }
  }

  private def validate(files: List[File]): Option[List[File]] =
    files.filter(_.length > MaxFileSize) match {
      case Nil     => Some(files)
      case List(f) =>
        Dialog.showMessage(c, s"The file '${f.getName}' is larger than the limit ($MaxFileSize MB).")
        None
      case fs      =>
        Dialog.showMessage(c, s"The files ${fs.map(_.getName).mkString("'","', '", "'")} are larger than the limit ($MaxFileSize MB)" )
        None
    }


  private def store(client: AuxFileClient, pid: SPProgramID, file: File) {
    val pm = new ProgressModel(s"Attaching file ${file.getName}", 100)
    val pd = new ProgressDialog(jFrame.orNull, s"Attach ${file.getName}", false, pm)
    onEDT(pd.setVisible(true))

    try {
      client.store(pid, file.getName, file, transferListener(pm))
    } finally {
      onEDT {
        pd.setVisible(false)
        pd.dispose()
      }
    }
  }

  override def apply() {
    exec(prompt.flatMap(validate)) { (client, pid, files) =>
      files foreach { store(client, pid, _) }
    }
  }
}
