package edu.gemini.pit.ui.util

import scala.swing._
import scala.swing.event._
import javax.swing

/**
 * A modal editor that implements a wizard UI.
 */
abstract class StdModalWizard[S, A](s: String, initial: S) extends ModalEditor[A] with (S => Unit) { dialog =>

  // Configure with our content, defined below.
  title = s
  resizable = false
  contents = Contents
  pack()

  // Our state is mutable and initially null, but is set immediately below
  private[this] var _state: S = initial

  // Our state has structured accessors. On update we update the UI
  def state = _state
  def state_=(newState: S) {
    _state = newState
    apply(_state)
  }

  def back()
  def next()
  def finish: A

  def enable(back: Boolean, next: Boolean, done: Boolean) {
    Footer.Back.enabled = back
    Footer.Next.enabled = next
    Footer.Finish.enabled = done
  }

  // Initialize
  enable(false, false, false)
  apply(state)

  def editor:Component
  
    // Our content object
  object Contents extends BorderPanel {
    import BorderPanel.Position._

    border = swing.BorderFactory.createEmptyBorder(8, 8, 8, 8);

    // Main components, defined below
    add(editor, Center)
    add(Footer, South)

    }

  // Footer
  object Footer extends BorderPanel {

    add(new FlowPanel {
      contents += Cancel
      contents += Back
      contents += Next
      contents += Finish
    }, BorderPanel.Position.East)

    object Cancel extends Button("Cancel") {
      reactions += {
        case ButtonClicked(_) => dialog.close()
      }
      focusable = false
    }
    object Back extends Button("< Back") {
      reactions += {
        case ButtonClicked(_) => back()
      }
      focusable = false
    }
    object Next extends Button("Next >") {
      reactions += {
        case ButtonClicked(_) => next()
      }
      focusable = false
    }

    object Finish extends Button("Finish") {
      reactions += {
        case ButtonClicked(_) => close(finish)
      }
      focusable = false
      dialog.peer.getRootPane.setDefaultButton(peer)
    }

  }

}
