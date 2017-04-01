package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable.{NonSiderealTarget, SiderealTarget, Target}
import edu.gemini.model.p1.targetio.impl.{NonSiderealWriter, SiderealWriter}
import edu.gemini.model.p1.targetio.api.FileType
import edu.gemini.pit.ui.util.{Rows, StdModalEditor, ValueRenderer}
import java.io.File
import java.util.prefs.Preferences._

import swing._
import javax.swing.JOptionPane

import edu.gemini.shared.gui.Chooser


object TargetExporter {
  def isExportable(target: Target) = ExportType(target).isDefined

  def open(parent: UIElement, targets: List[Target]) {
    new TargetExporter(parent, targets).open()
  }

  sealed trait ExportType {
    def value(): String
  }

  object ExportType {
    case object NonSidereal extends ExportType {
      def value() = "Non-sidereal"
    }
    case object Sidereal extends ExportType {
      def value() = "Sidereal"
    }

    val values: List[ExportType] = List(NonSidereal, Sidereal)

    def fromValue(v: String) = values.find(_.value() == v)

    def apply(t: Target): Option[ExportType] = t match {
      case _: NonSiderealTarget if !t.isEmpty => Some(NonSidereal)
      case _: SiderealTarget    if !t.isEmpty => Some(Sidereal)
      case _                                  => None
    }

    def options(ts: List[Target]): Set[ExportType] =
      (for { Some(et) <- ts.map(apply) } yield et).toSet
  }

  case class ExportPreferences(exportType: ExportType, fileType: FileType)

  class PreferenceEditor(init: ExportPreferences) extends StdModalEditor[ExportPreferences]("Export Preference") {

    object Editor extends GridBagPanel with Rows {
      addRow(ExportTypeLabel, ExportTypeCombo)
      addRow(new Label("Export As"), ExportAsCombo)
    }

    object ExportTypeLabel extends Label("Export")

    object ExportTypeCombo extends ComboBox(ExportType.values) with ValueRenderer[ExportType] {
      selection.item = init.exportType
    }
    object ExportAsCombo extends ComboBox(FileType.values)  with ValueRenderer[FileType] {
      selection.item = init.fileType
    }

    def exportTypeVisible: Boolean = ExportTypeLabel.visible
    def exportTypeVisible_=(b: Boolean) {
      ExportTypeLabel.visible = b
      ExportTypeCombo.visible = b
    }

    def editor = Editor

    def value = ExportPreferences(
      ExportTypeCombo.selection.item,
      ExportAsCombo.selection.item)
  }
}

import TargetExporter._
import ExportType._

class TargetExporter(parent: UIElement, targets: List[Target]) {
  private lazy val node = userNodeForPackage(getClass)
  private lazy val exportTypePref = getClass.getName + ".exportType"
  private lazy val exportTypeDef = NonSidereal
  private lazy val fileTypePref = getClass.getName + ".exportType"
  private lazy val fileTypeDef = FileType.Csv

  private def lookupPref[T](n: String, d: T, read: String=>Option[T]): T =
    Option(node.get(n, null)) flatMap { v => read(v) } getOrElse d

  def exportType: ExportType = lookupPref(exportTypePref, exportTypeDef, ExportType.fromValue)
  def exportType_=(et: ExportType) { node.put(exportTypePref, et.value()) }

  def fileType: FileType = lookupPref(fileTypePref, fileTypeDef, FileType.fromExtension)
  def fileType_=(ft: FileType) { node.put(fileTypePref, ft.extension) }

  def open() {
    askPrefs foreach { prefs =>
      try {
        askFile(prefs.fileType) foreach { file => export(prefs, file) }
      } finally {
        exportType = prefs.exportType
        fileType   = prefs.fileType
      }
    }
  }

  private def askPrefs: Option[ExportPreferences] = {
    val exportOptions = ExportType.options(targets)
    val config = exportOptions.size match {
      case 1 => Some((exportOptions.iterator.next(), false))
      case 2 => Some((exportType, true))
      case _ => None
    }

    config flatMap {
      case (etype, vis) =>
        val ed = new PreferenceEditor(ExportPreferences(etype, fileType))
        ed.exportTypeVisible = vis
        ed.open(parent)
    }
  }

  private def askFile(ftype: FileType): Option[File] = {
    val chooser = new Chooser[TargetExporter]("file", parent.peer)
    val choice  = chooser.chooseSave(ftype.value(), ".%s".format(ftype.extension))
    choice flatMap { file =>
      if (hasWrongExtension(file, ftype)) askExtension(file, ftype) else Some(file)
    }
  }

  private def hasWrongExtension(f: File, ftype: FileType) = extension(f) exists { _ != ftype.extension }

  // Handles grim case of, for example, choosing to write a .csv file but
  // picking a file with an extension like ".fits".
  private def askExtension(f: File, ftype: FileType): Option[File] = {
    val cur = extension(f).get
    val req = ftype.extension
    JOptionPane.showOptionDialog(
      parent.peer,
      "You chose a file with extension '%s', not '%s'.".format(cur, req),
      "Choose File Extension",
      JOptionPane.YES_NO_CANCEL_OPTION,
      JOptionPane.QUESTION_MESSAGE,
      null,
      Array("Switch to .%s".format(req), "Use both %s.%s".format(cur, req), "Cancel"),
      "Switch to .%s".format(req)) match {
      case 0 => Some(new File("%s%s".format(f.getPath.stripSuffix(cur), req)))
      case 1 => Some(new File("%s.%s".format(f.getPath, req)))
      case _ => None
    }
  }

  private def extension(f: File): Option[String] =
    f.getName.reverse.takeWhile(_ != '.').reverse match {
      case ext if ext != f.getName => Some(ext)
      case _ => None
    }

  private def export(prefs: ExportPreferences, file: File) {
    val res = prefs.exportType match {
      case NonSidereal =>
        def collect(ts: List[Target]): List[NonSiderealTarget] = ts collect {
          case ns: NonSiderealTarget if !ns.isEmpty => ns
        }
        NonSiderealWriter.write(collect(targets), file, prefs.fileType)
      case Sidereal    =>
        def collect(ts: List[Target]): List[SiderealTarget] = ts collect {
          case s: SiderealTarget if !s.isEmpty => s
        }
        SiderealWriter.write(collect(targets), file, prefs.fileType)
    }

    res.left foreach { err =>
      Dialog.showMessage(
        message = err.msg,
        title   = "Problem Exporting Targets",
        messageType = Dialog.Message.Error
      )
    }
  }
}
