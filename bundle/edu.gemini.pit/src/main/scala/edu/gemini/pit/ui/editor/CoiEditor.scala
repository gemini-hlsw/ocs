package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable.{CoInvestigator, InvestigatorGender, InvestigatorStatus}
import edu.gemini.shared.gui.textComponent.SelectOnFocus

import scala.swing.ComboBox
import scala.swing.GridBagPanel
import scala.swing.Label
import scala.swing.TextField
import scala.swing.UIElement
import edu.gemini.pit.ui.util._

import swing.event.ValueChanged

object CoiEditor {
  def open(coi: CoInvestigator, editable: Boolean, parent: UIElement, setup: CoiEditor => Unit = _ => ()): Option[CoInvestigator] = {
    val editor = new CoiEditor(coi, editable)
    setup(editor)
    editor.open(parent)
  }
}

class CoiEditor(coi: CoInvestigator, editable:Boolean) extends StdModalEditor[CoInvestigator]("Edit Co-Investigator") {

  // Our main editor component
  object Editor extends GridBagPanel with Rows {
    addRow(new Label("First Name:") { icon = SharedIcons.ICON_USER_DIS }, FirstName)
    addRow(new Label("Last Name:"), LastName)
    addRow(new Label("Institution:"), InstitutionEditor)
    addRow(new Label("Degree Status:"), Status)
    addRow(new Label("Gender"), Gender)
    addRow(new Label("Gender identity is for statistics only and will not be provided to the TACs."))
    addRow(new Label("Email Address:"), Email)
    addRow(new Label("Phone Number(s):"), Phone)
  }

  // Editability
  FirstName.enabled = editable
  LastName.enabled = editable
  Institution.enabled = editable
  Institution.enabled = editable
  Status.enabled = editable
  Gender.enabled = editable
  Email.enabled = editable
  Phone.enabled = editable
  Contents.Footer.OkButton.enabled = editable

  // Validation (optional for noe)
  private val validatingControls = Seq(FirstName, LastName, Email, Institution)
//  override def editorValid = validatingControls.forall(_.valid)
  validatingControls foreach {
    _.reactions += {
      case ValueChanged(_) => validateEditor()
    }
  }
  validateEditor()

  // Our data controls
  object FirstName extends TextField(coi.firstName, 30) with SelectOnFocus with NonEmptyText
  object LastName extends TextField(coi.lastName) with SelectOnFocus with NonEmptyText
  object Institution extends TextField(coi.institution) with SelectOnFocus with NonEmptyText
  object Pick extends InstitutionChooser {
    def currentInstitutionName: String = Institution.text
    def institutionSelected(inst: Institution) {
      Institution.text = inst.name
    }
  }
  object InstitutionEditor extends GridBagPanel {
    add(Institution, new Constraints() { gridx = 0; fill = GridBagPanel.Fill.Horizontal; weightx = 1.0 })
    add(Pick, new Constraints() { gridx = 1 })
  }

  object Email extends TextField(coi.email) with SelectOnFocus with EmailText
  object Phone extends TextField(coi.phone.mkString(", ")) with SelectOnFocus
  object Status extends ComboBox(InvestigatorStatus.values.toSeq) with ValueRenderer[InvestigatorStatus] {
    selection.item = coi.status
  }
  object Gender extends ComboBox(InvestigatorGender.values.toSeq) with ValueRenderer[InvestigatorGender] {
    selection.item = coi.gender

    override def text(a: InvestigatorGender): String = a match {
      case InvestigatorGender.NONE_SELECTED => ""
      case x => x.value()
    }
  }


  // Construct the editor
  def editor = Editor

  // Construct a new value
  def value: CoInvestigator = coi.copy(
    firstName = FirstName.text,
    lastName = LastName.text,
    institution = Institution.text,
    status = Status.selection.item,
    gender = Gender.selection.item,
    phone = Phone.text.split(",").map(_.trim).toList,
    email = Email.text)

}



