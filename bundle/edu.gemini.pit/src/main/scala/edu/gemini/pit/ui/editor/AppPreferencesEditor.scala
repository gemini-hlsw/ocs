package edu.gemini.pit.ui.editor

import edu.gemini.pit.model.AppPreferences
import edu.gemini.model.p1.pdf.P1PDF

import edu.gemini.pit.ui.util.{ValueRenderer, Rows, StdModalEditor}
import swing._

object AppPreferencesEditor {
  def open(parent: UIElement, ps:AppPreferences):Option[AppPreferences] = new AppPreferencesEditor(ps).open(parent)
}

class AppPreferencesEditor private (ps:AppPreferences) extends StdModalEditor[AppPreferences]("Preferences ...") {

  object editor extends GridBagPanel with Rows {
    addRow(new Label("PDF Format"), PdfTemplateCombo)
    addRow(new Label("PIT Mode"), PitModeCombo)
  }

  object PdfTemplateCombo extends ComboBox(P1PDF.templates) with ValueRenderer[P1PDF.Template] {
    selection.item = ps.pdf.getOrElse(P1PDF.GeminiDefault)
  }

  object PitModeCombo extends ComboBox(AppPreferences.PITMode.values.toSeq) {
    selection.item = ps.mode
  }

  def value = AppPreferences(Some(PdfTemplateCombo.selection.item), PitModeCombo.selection.item)

}