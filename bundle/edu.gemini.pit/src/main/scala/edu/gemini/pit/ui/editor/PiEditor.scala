package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable.PrincipalInvestigator
import edu.gemini.model.p1.immutable.InstitutionAddress
import edu.gemini.model.p1.immutable.InvestigatorStatus


import edu.gemini.pit.ui.util._
import java.awt.event.{KeyAdapter, KeyEvent}
import edu.gemini.shared.gui.textComponent.SelectOnFocus

import swing._
import event.ValueChanged

object PiEditor {
  def open(coi: PrincipalInvestigator, editable: Boolean, parent: UIElement, setup: PiEditor => Unit = _ => ()): Option[PrincipalInvestigator] = {
    val editor = new PiEditor(coi, editable)
    setup(editor)
    editor.open(parent)
  }
}

class PiEditor(pi: PrincipalInvestigator, editable:Boolean) extends StdModalEditor[PrincipalInvestigator]("Edit Principal Investigator") {

  // Our editor
  object Editor extends GridBagPanel with Rows {
    addRow(new Label("First Name:") { icon = SharedIcons.ICON_USER }, FirstName)
    addRow(new Label("Last Name:"), LastName)
    addRow(new Label("Degree Status:"), Status)
    addRow(new Label("Email Address:"), Email)
    addRow(new Label("Phone Number(s):"), Phone)
    addSpacer()

    addRow(new Label("Institution:"), Institution.NameEditor)
    addRow(new Label("Address:"), Institution.AddressScroll, GridBagPanel.Fill.Both, wy=1)
    addRow(new Label("Country:"), Institution.Country)
  }

  // Editability
  FirstName.enabled = editable
  LastName.enabled = editable
  Institution.Name.enabled = editable
  Institution.Address.enabled = editable
  Institution.Country.enabled = editable
  Status.enabled = editable
  Email.enabled = editable
  Phone.enabled = editable
  Contents.Footer.OkButton.enabled = editable

  // Validation (optional for now)
  private val validatingControls = Seq(FirstName, LastName, Email, Institution.Name)
  validatingControls foreach {
    _.reactions += {
      case ValueChanged(_) => validateEditor()
    }
  }
  validateEditor()

  // Main PI Fields
  object FirstName extends TextField(pi.firstName, 20) with SelectOnFocus with NonEmptyText
  object LastName extends TextField(pi.lastName) with SelectOnFocus with NonEmptyText
  object Email extends TextField(pi.email) with SelectOnFocus with EmailText
  object Phone extends TextField(pi.phone.mkString(", ")) with SelectOnFocus
  object Status extends ComboBox(InvestigatorStatus.values.toSeq) with ValueRenderer[InvestigatorStatus] {
    selection.item = pi.status
  }

  // Institution Fields
  object Institution {
    val ia: InstitutionAddress = pi.address
    object Name extends TextField(ia.institution, 20) with SelectOnFocus with NonEmptyText

    object Pick extends InstitutionChooser {
      def currentInstitutionName: String = Name.text
      def institutionSelected(inst: Institution) {
        Name.text    = inst.name
        Address.text = inst.addr.mkString("\n")
        Country.text = inst.country
      }
    }

    object NameEditor extends GridBagPanel {
      add(Name, new Constraints() { gridx = 0; fill = GridBagPanel.Fill.Horizontal; weightx = 1.0 })
      add(Pick, new Constraints() { gridx = 1 })
    }

    object Address extends TextArea(ia.address, 6, 30) with SelectOnFocus {
      peer.addKeyListener(new KeyAdapter {
        override def keyPressed(e:KeyEvent) {
          if (e.getKeyCode == KeyEvent.VK_TAB) {
            if (e.getModifiers > 0)
              peer.transferFocusBackward()
            else
              peer.transferFocus()
            e.consume()
          }
        }
      })
    }

    object AddressScroll extends ScrollPane(Address)
    object Country extends TextField(ia.country) with SelectOnFocus with NonEmptyText
  }

  // Construct our editor
  def editor = Editor

  // Construct a new value
  def value = pi.copy(
    firstName = FirstName.text,
    lastName = LastName.text,
    address = pi.address.copy(
      institution = Institution.Name.text,
      address = Institution.Address.text,
      country = Institution.Country.text),
    status = Status.selection.item.asInstanceOf[InvestigatorStatus], // :-/
    phone = Phone.text.split(",").map(_.trim).filter(_.nonEmpty).toList,
    email = Email.text)

}



