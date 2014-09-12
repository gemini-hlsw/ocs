package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable._
import swing._
import edu.gemini.pit.ui.util.BooleanToolPreference._
import edu.gemini.pit.ui.robot.CatalogRobot
import edu.gemini.pit.model.Model
import edu.gemini.pit.catalog._
import java.awt
import awt.Color
import edu.gemini.pit.ui.util._
import javax.swing.BorderFactory
import Swing._

object SynchronousLookup {
  def open(name:String, parent:UIElement) = new SynchronousLookup(name).open(parent)
}

class SynchronousLookup private (targetName:String) extends ModalEditor[Target] {dialog =>

  // TODO: this probably leaks an actor
  private lazy val handler = new CatalogRobot(peer)
  handler.addListener(listener)

  // Top-level config
  title = "Catalog Lookup"
  resizable = false
  contents = Contents
  updateEnabledState()
  pack()

  // Our main content object
  object Contents extends BorderPanel {

    // Space things out a little more
    peer.setLayout(new awt.BorderLayout(8, 8))
    border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

    // Add our content, defined below
    add(new Label("Select one or more catalogs:"), BorderPanel.Position.North)
    add(choices, BorderPanel.Position.Center)
    add(footer, BorderPanel.Position.South)

    // Footer is just a flow panel, more or less
    object footer extends BorderPanel {

      // Our content, defined below
      add(msg, BorderPanel.Position.North)
      add(new FlowPanel {
        contents += spinner
        contents += lookup
        contents += cancel
      }, BorderPanel.Position.East)

      // Error line
      object msg extends Label("") {
        foreground = Color.RED
        horizontalAlignment = Alignment.Left
        preferredSize = (preferredSize.width, 15)
      }

      // Spinner icon
      object spinner extends Label {
        icon = SharedIcons.ICON_SPINNER_BLUE
        visible = false
      }

      // Lookup is our default button
      dialog.peer.getRootPane.setDefaultButton(lookup.peer)

      // Lookup and cancel buttons
      lazy val lookup:Button = Button("Lookup") {

        // We're creating a new model with a single obs with an empty target
        val t = Target.empty.copy(name = targetName)
        val m = (Model.proposal andThen Proposal.targets).set(Model.empty, List(t))
        handler.reset
        handler.bind(Some(m), done) // This will set things going
        handler.lookup(t)
      }

      // Cancel button
      lazy val cancel = Button("Cancel") {
        dialog.close()
      }

    }

    // Our choice area is just a list of checkboxes
    object choices extends GridBagPanel with Rows {
      addRow(CatalogButton(SIMBAD))
      addRow(CatalogButton(NED))
      addRow(CatalogButton(HORIZONS))
    }

    // Our custom checkbox type
    private case class CatalogButton(pref:BooleanToolPreference) extends CheckBox(pref.name) {
      selected = pref.get
      action = Action(pref.name) {
        pref.set(selected)
        updateEnabledState()
      }
    }

  }

  // Fix the the lookup button's enabled state
  private def updateEnabledState() {
    Contents.footer.lookup.enabled = SIMBAD.get || NED.get || HORIZONS.get
  }

  def listener(state:CatalogRobot#State) {
    state.headOption.map {
      case (t, s) => s match {

        case Some(f) =>
          Contents.footer.msg.text = f match {
            case Offline     => "Server(s) offline."
            case Error(_)    => "Server error."
            case NotFound(_) => "Not found."
          }
          Contents.footer.lookup.enabled = true
          Contents.footer.spinner.visible = false

        case None =>
          Contents.footer.msg.text = ""
          Contents.footer.lookup.enabled = false
          Contents.footer.spinner.visible = true

      }
    }
  }

  private def done(model:Option[Model]) {
    for {
      m <- model
      t <- m.proposal.targets.headOption
    } close(t)
  }

}