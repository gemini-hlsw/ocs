package edu.gemini.pit.ui.editor

import edu.gemini.model.p1.immutable._
import swing._
import edu.gemini.pit.ui.robot.CatalogRobot
import edu.gemini.pit.model.Model
import edu.gemini.pit.catalog._
import java.awt
import awt.Color
import edu.gemini.pit.ui.util._
import javax.swing.BorderFactory
import Swing._

object SynchronousLookup2 {
  def open(name:String, parent:UIElement) = new SynchronousLookup2(name).open(parent)
}

class SynchronousLookup2 private (targetName:String) extends ModalEditor[Target] {dialog =>

  // TODO: this probably leaks an actor
  private lazy val handler = new CatalogRobot(peer)
  handler.addListener(listener)

  // Top-level config
  title = "Lookup"
  resizable = false
  contents = Contents
  pack()

  // We're creating a new model with a single obs with an empty target
  val t = Target.empty.copy(name = targetName)
  val m = (Model.proposal andThen Proposal.targets).set(Model.empty, List(t))

  // Order here is significant
  handler.reset()
  handler.bind(Some(m), done)
  handler.lookup(t)

  // Our main content object
  object Contents extends BorderPanel {

    // Space things out a little more
    peer.setLayout(new awt.BorderLayout(8, 8))
    border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

    // Our content, defined below
    add(msg, BorderPanel.Position.Center)
    add(spinner, BorderPanel.Position.West )
    add(cancel, BorderPanel.Position.East)

    // Error line
    object msg extends Label("Searching...") {
      horizontalAlignment = Alignment.Left
      preferredSize = (150, 15)
    }

    // Spinner icon
    object spinner extends Label {
      icon = SharedIcons.ICON_SPINNER_BLUE
      visible = false
    }

    // Cancel button
    lazy val cancel = Button("Cancel") {
      dialog.close()
    }

  }

  def listener(state:CatalogRobot#State) {
    state.headOption.foreach {
      case (_, s) => s match {
        case Some(f) =>
          Contents.msg.text = f match {
            case Offline     => "Server(s) offline."
            case Error(e)    => "Server error."
            case NotFound(_) => "Not found."
          }
          Contents.msg.foreground = Color.RED
          Contents.spinner.visible = false
        case None =>
          Contents.msg.foreground = Color.BLACK
          Contents.msg.text = "Searching..."
          Contents.spinner.visible = true
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