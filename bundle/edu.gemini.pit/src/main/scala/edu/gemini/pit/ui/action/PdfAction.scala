package edu.gemini.pit.ui.action

import edu.gemini.model.p1.immutable.ProposalIo
import edu.gemini.model.p1.pdf.P1PDF
import edu.gemini.pit.model.{AppPreferences, Model}
import edu.gemini.pit.ui.util.{Rows, StdModalEditor, ValueRenderer}
import edu.gemini.ui.workspace.scala.RichShell
import java.util.prefs.Preferences.userNodeForPackage

import edu.gemini.shared.gui.Chooser

import scala.swing._

object PdfAction {

  private val ASK_PREF = getClass.getName + ".askPdfTemplate"

  // Whether to bug the user to select a PDF template.
  def ask: Boolean = try {
    userNodeForPackage(getClass).getBoolean(ASK_PREF, true)
  } catch {
    case _: Exception => false
  }

  // Set to true to prompt the user for a PDF template.
  def ask_=(b: Boolean): Unit = {
    userNodeForPackage(getClass).putBoolean(ASK_PREF, b)
  }

}

import PdfAction._

class PdfAction(shell: RichShell[Model]) extends ShellAction(shell, "Export as PDF...", None, 0) {

  enabledWhen {
    shell.model.isDefined
  }

  case class TemplatePref(template: P1PDF.Template, ask: Boolean)

  object TemplatePref {
    def current: TemplatePref = {
      val ap = AppPreferences.current
      val tmpl = ap.pdf.getOrElse(P1PDF.GeminiDARP)
      TemplatePref(tmpl, ask)
    }

    def current_=(tp: TemplatePref): Unit = {
      ask = tp.ask
      AppPreferences.current = AppPreferences.current.copy(pdf = Some(tp.template))
    }
  }

  class TemplateEditor(tp: TemplatePref) extends StdModalEditor[TemplatePref]("Select PDF Format") {

    object Editor extends GridBagPanel with Rows {
      addRow(new Label("Format"), TemplateCombo)
      addRow(new Label(" "))
      addRow(DontAskCheck)
    }

    object TemplateCombo extends ComboBox(P1PDF.templates) with ValueRenderer[P1PDF.Template] {
      selection.item = tp.template
    }

    object DontAskCheck extends CheckBox("Don't ask me again.") {
      selected = false
      font = font.deriveFont(font.getSize2D - 2.0f)
    }

    def editor = Editor

    def value = TemplatePref(TemplateCombo.selection.item, !DontAskCheck.selected)
  }

  object TemplateEditor {
    def show(parent: UIElement): Option[P1PDF.Template] = {
      val ed = new TemplateEditor(TemplatePref.current)
      val res = ed.open(parent)
      res foreach {
        p => TemplatePref.current = p
      }
      res.map(_.template)
    }
  }

  def templatePreference(p: UIElement): Option[P1PDF.Template] =
    if (AppPreferences.current.pdf.isEmpty || ask)
      TemplateEditor.show(p)
    else
      Some(TemplatePref.current.template)

  override def apply(): Unit = {
    for {
      m <- shell.model
      t <- templatePreference(UIElement.wrap(shell.peer))
      f <- new Chooser[PdfAction]("defaultDir", shell.peer).chooseSave("PDF Documents", ".pdf")
    } {
      val xml = ProposalIo.writeToXml(m.proposal)
      P1PDF.createFromNode(xml, t, f, shell.file.map(_.getParentFile).filterNot(_ == null))
    }
  }
}
