package edu.gemini.shared.gui

import java.io.File
import java.util.prefs.Preferences.userNodeForPackage
import javax.swing.JFileChooser
import javax.swing.JFileChooser.APPROVE_OPTION
import javax.swing.JOptionPane.{WARNING_MESSAGE, YES_NO_OPTION, YES_OPTION, showConfirmDialog}
import javax.swing.filechooser.FileFilter

/**
 * A file chooser that remembers (persistently) the last location used. This value is remembered on a per-class
 * basis (the type parameter A is the owning class) and a subkey. For example, an import/export class might use
 * a Chooser[ImportExport] with keys "importDir" and "exportDir".
 */
class Chooser[A:Manifest](key: String, parent: java.awt.Component) {

  private lazy val pref = implicitly[Manifest[A]].runtimeClass.getName + "." + key
  private lazy val node = userNodeForPackage(getClass)
  private lazy val home = new File(System.getProperty("user.home"))
  private lazy val peer = new JFileChooser()

  private def filter(title:String, ext:String) = new FileFilter() {
    def getDescription = title
    def accept(f: File) = f.getName.endsWith(ext) || f.isDirectory
  }

  def defaultDir = Option(node.get(pref, null)).map(new File(_)).filter(_.isDirectory).getOrElse(home)
  def defaultDir_=(f: File) {
    node.put(pref, f.getPath)
  }

  def chooseSave(title:String, ext:String): Option[File] = try {
    peer.setFileFilter(filter(title, ext))
    peer.setCurrentDirectory(defaultDir)
    peer.showSaveDialog(parent) match {
      case APPROVE_OPTION => confirmSave(confirmExt(peer.getSelectedFile, ext))
      case _              => None
    }
  } finally {
    defaultDir = peer.getCurrentDirectory
  }

  def chooseOpen(title:String, ext:String): Option[File] = try {
    peer.setFileFilter(filter(title, ext))
    peer.setCurrentDirectory(defaultDir)
    peer.showOpenDialog(parent) match {
      case APPROVE_OPTION => Some(peer.getSelectedFile)
      case _              => None
    }
  } finally {
    defaultDir = peer.getCurrentDirectory
  }

  private def confirmExt(file:File, ext:String) = 
    if (file.getName.endsWith(ext)) file else new File(file.getParentFile, file.getName + ext)
  
  private def confirmSave(file: File) = if ((!file.exists) || YES_OPTION == showConfirmDialog(parent,
    "The file " + file.getName + " already exists.\nAre you sure you want to overwrite it?",
    "Confirm Save", WARNING_MESSAGE, YES_NO_OPTION)) Some(file) else None

}

