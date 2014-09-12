package edu.gemini.pit.ui.editor

import swing.event.ButtonClicked
import swing.{Dialog, Button}

object InstitutionChooser

abstract class InstitutionChooser extends Button("Choose") {
  tooltip = "Choose the institution from a list of options."
  reactions += { case ButtonClicked(_) => apply() }

  def apply() {
    val instName = Dialog.showInput(this, "Select the institution from these options",
      title = "Select Institution",
      entries = Institutions.all.map(_.name),
      initial = Institutions.bestMatch(currentInstitutionName).map(_.name).getOrElse(Institutions.all.head.name)
    )

    (for {n <- instName; i <- Institutions.bestMatch(n)} yield i) foreach {
      institutionSelected
    }
  }

  def currentInstitutionName: String
  def institutionSelected(inst: Institution)
}