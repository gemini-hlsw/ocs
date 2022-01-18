package edu.gemini.pit.ui.util

import java.awt
import javax.swing
import scala.swing._
import swing.text.JTextComponent
import swing.{JCheckBox, JTextField, JComponent}

/**
 * Modal editor for a value of type A, with standard footer buttons.
 */
abstract class StdModalEditor[A](theTitle: String) extends ModalEditor[A] { dialog =>

  // Top-level config
  title = theTitle
  resizable = false
  // Call this before setting contents
  setDropTarget(Contents.peer)
  contents = Contents
  pack()

  def header: Component = null
  def editor: Component
  def value: A

  /** Override this, then call validate() to update Ok button. */
  def editorValid:Boolean = true

  final def validateEditor(): Unit = {
    Contents.Footer.OkButton.enabled = editorValid
  }

  // REL-1131 This is necessary to avoid a bug that shows in certain Linux systems on Sun's JDK
  def setDropTarget(c:JComponent): Unit = {
    c match {
      case _:JTextComponent => c.setDropTarget(null)
      case _                =>
    }
    c.getComponents.collect {
      case j:JComponent => j
    }.foreach(setDropTarget)
  }


  // Our main content object
  object Contents extends BorderPanel {
    // Space things out a little more
    peer.setLayout(new awt.BorderLayout(8, 8))
    border = swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)

    // Add our content, defined below
    Option(header).foreach { add(_, BorderPanel.Position.North) }
    add(editor, BorderPanel.Position.Center)
    add(Footer, BorderPanel.Position.South)

    // Footer is a standard widget
    lazy val Footer: OkCancelFooter = OkCancelFooter(dialog) {
      close(value)
    }

  }

}