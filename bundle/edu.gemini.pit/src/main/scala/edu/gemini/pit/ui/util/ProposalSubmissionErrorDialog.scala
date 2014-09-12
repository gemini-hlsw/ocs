package edu.gemini.pit.ui.util

import swing._
import java.awt
import javax.swing.BorderFactory

import scalaz._
import Scalaz._

import java.awt.Color

class ProposalSubmissionErrorDialog(msg: Seq[String]) extends StdModalEditor[String]("Proposal submission errors") { dialog =>

  contents = Content

  // Our main content object
  object Content extends BorderPanel {

    // Space things out a little more
    peer.setLayout(new awt.BorderLayout(8, 8))
    border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

    // Add our content, defined below
    Option(header).foreach { add(_, BorderPanel.Position.North) }
    add(editor, BorderPanel.Position.Center)
    add(Footer, BorderPanel.Position.South)

    // Footer is a standard widget
    lazy val Footer = OkFooter(dialog) {
      close(value)
    }

  }

  object DetailsArea extends BorderPanel {
    border = BorderFactory.createLineBorder(Color.LIGHT_GRAY)
    val text = new TextArea
    text.wordWrap = true
    text.lineWrap = true
    text.editable = false
    text.background = Color.white

    text.text = msg.map(l => s"\u2022 $l").mkString("\n")
    val sp = new ScrollPane(text)
    sp.horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
    sp.preferredSize = new Dimension(550, 150)
    add(sp, BorderPanel.Position.Center)
  }

  object Log extends GridBagPanel with Rows {
    addRow(new Label("Summary of errors:"))
    add(DetailsArea, new Constraints { gridx = 1; gridy = 3; fill = GridBagPanel.Fill.Horizontal; weightx = 2 })

    revalidate()
    repaint()
    dialog.pack()
  }

  def editor = Log

  def value = ""
}
