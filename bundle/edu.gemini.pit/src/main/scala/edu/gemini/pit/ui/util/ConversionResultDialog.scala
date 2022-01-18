package edu.gemini.pit.ui.util

import swing._
import java.awt
import javax.swing.BorderFactory
import edu.gemini.model.p1.immutable.Semester
import edu.gemini.pit.model.{Model, ModelConversion}

import scalaz._
import Scalaz._

import edu.gemini.pit.ui.binding.{Bound, BoundView}
import java.awt.Color

class ConversionResultDialog(m: Model) extends StdModalEditor[ModelConversion]("Proposal converted") with BoundView[Model] { dialog =>
  protected def lens = Lens.lensId[Model]

  contents = Content

  override def value = m.conversion

  override def editor = Log

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

  override def refresh(m: Option[Model]): Unit = {
    DetailsArea.bind(m, _ => ())
  }

  object DetailsArea extends BorderPanel with Bound[Model, ModelConversion] {
    val text = new TextArea
    text.wordWrap = true
    text.lineWrap = true
    text.editable = false
    text.background = Color.white

    val sp = new ScrollPane(text)
    sp.horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
    sp.preferredSize = new Dimension(200, 150)
    add(sp, BorderPanel.Position.Center)

    override def refresh(m: Option[ModelConversion]): Unit = {
      text.text = ~m.map(_.changes.map(l => s"\u2022 $l").mkString("\n"))
    }

    protected def lens = Model.conversion
  }

  object Log extends GridBagPanel with Rows {
    val conversionText = if (value.from.display =/= Semester.current.display) {
        s"The opened proposal from semester ${value.from.display} was converted to the ${Semester.current.display} format."
      } else {
        s"The opened proposal from semester ${value.from.display} was converted to the current format."
      }

    addRow(new Label(conversionText))
    addRow(new Label("Summary of changes:"))
    add(DetailsArea, new Constraints { gridx = 1; gridy = 3; fill = GridBagPanel.Fill.Horizontal; weightx = 2 })

    revalidate()
    repaint()
    dialog.pack()
  }

}
