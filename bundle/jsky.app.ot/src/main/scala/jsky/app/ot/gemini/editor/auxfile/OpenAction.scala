package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.auxfile.api.{AuxFile, AuxFileException}
import edu.gemini.auxfile.client.AuxFileClient
import edu.gemini.spModel.core.SPProgramID
import jsky.app.ot.ui.util.{ProgressDialog, ProgressModel}
import java.awt.Desktop
import java.io.File

import jsky.util.gui.Resources

import scala.swing.Component
import scala.swing.Swing._

object OpenAction {
  val SupportedExtensions = Set(
    "bpm", "doc", "eps", "gif", "html", "jpeg", "jpg", "pdf", "png", "pgm",
    "ppm", "ppt", "png", "ps",  "rtf",  "txt",  "tif", "tiff")

  def isSupported(f: AuxFile): Boolean =
    SupportedExtensions.contains(f.getName.reverse.takeWhile(_ != '.').reverse)
}

import AuxFileAction.transferListener
import OpenAction.isSupported

class OpenAction(c: Component, model: AuxFileModel) extends AuxFileAction("Open", c, model) {
  icon    = Resources.getIcon("eclipse/openbrwsr.gif")
  toolTip = "Open the selected file for viewing."

  override def interpret(ex: AuxFileException) =
     s"Sorry, there was an error while opening files: '${ex.getMessage}'"

  override def currentEnabledState: Boolean = super.currentEnabledState &&
    Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.OPEN) &&
    model.currentSelection.exists(lst => !lst.isEmpty && lst.forall(isSupported))

  private def open(client: AuxFileClient, pid: SPProgramID, sel: AuxFile): Boolean = {
    val tmpDir    = new File(System.getProperties.getProperty("java.io.tmpdir"))
    val localFile = new File(tmpDir, sel.getName)
    if (localFile.exists()) localFile.delete()
    localFile.deleteOnExit()

    val pm = new ProgressModel(s"Fetching file ${sel.getName}", 100)
    val pd = new ProgressDialog(jFrame.orNull, s"Open ${sel.getName}", false, pm)
    onEDT(pd.setVisible(true))

    val fetchResult = try {
      client.fetch(pid, sel.getName, localFile, transferListener(pm))
    } finally {
      onEDT {
        pd.setVisible(false)
        pd.dispose()
      }
    }

    if (fetchResult) Desktop.getDesktop.open(localFile)
    fetchResult
  }

  override def apply() {
    exec(model.currentSelection) { (client, pid, selection) =>
      (true/:selection) { (t,f) => t && open(client, pid, f) }
    }
  }
}
