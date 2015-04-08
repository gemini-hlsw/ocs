package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt._
import java.awt.event.{ItemEvent, ItemListener}
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import javax.swing._
import javax.swing.event.DocumentEvent

import edu.gemini.horizons.api.{HorizonsQuery, HorizonsReply}
import edu.gemini.shared.gui.calendar.JCalendarPopup
import edu.gemini.shared.gui.text.AbstractDocumentListener
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ITarget.Tag
import edu.gemini.spModel.target.system.{ITarget, NamedTarget, ConicTarget}
import jsky.app.ot.OTOptions
import jsky.app.ot.gemini.editor.horizons.HorizonsService
import jsky.app.ot.gemini.editor.targetComponent.{TimeConfig, TelescopePosEditor}
import jsky.app.ot.ui.util.TimeDocument
import jsky.util.gui.DialogUtil

import scalaz._, Scalaz._
import scalaz.concurrent.Task

// [DATE] at [TIME] UTC [Go] [Plot]
class ValidAtEditor extends JPanel with TelescopePosEditor with ReentrancyHack {

  private[this] var ct = new ConicTarget

  val UTC = TimeZone.getTimeZone("UTC")

  val calendar = new JCalendarPopup { c =>
    c.setTimeZone(UTC)
    c.setMaximumWidth(150)
    c.setPreferredSize(c.getPreferredSize <| (_.width = 150))
    c.setMinimumSize(c.getPreferredSize)
  }

  val timeConfig = new JComboBox[TimeConfig]() <| { w =>

    val timeFormatter = new SimpleDateFormat("HH:mm:ss") <| (_.setTimeZone(UTC))
    val textField = w.getEditor.getEditorComponent.asInstanceOf[JTextField] // ugh
    val textDoc = new TimeDocument(textField) <| (_.setTime(timeFormatter.format(new Date)))
    textField.setDocument(textDoc) // tie the knot

    w.setEditable(true)
    w.setModel(new DefaultComboBoxModel(TimeConfig.values))
    w.setRenderer(new ListCellRenderer[TimeConfig] {
      val delegate = w.getRenderer // N.B. we can't implement this in Scala, but we delegate to the old one
      override def getListCellRendererComponent(list: JList[_ <: TimeConfig], value: TimeConfig, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component =
        delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) match {
          case label: JLabel => label <| (_.setText(value.displayValue))
        }
    })

    w.addItemListener(new ItemListener {
      override def itemStateChanged(e: ItemEvent): Unit =
        if (e.getStateChange == ItemEvent.SELECTED) {
          e.getItem match {
            case tc: TimeConfig => textDoc.setTime(timeFormatter.format(tc.getDate))
            case ts: String     => // String is selected; what to do?
          }
        }
    })

    textDoc.addDocumentListener(new AbstractDocumentListener {
      override def textChanged(docEvent: DocumentEvent, newText: String): Unit =
        println("Change! " + newText)
    })

  }

  val go = new JButton("Go")

  val plot = new JButton("Plot")

  setLayout(new GridBagLayout)

  add(calendar, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.fill = GridBagConstraints.HORIZONTAL
    c.weightx = 2
    c.insets = new Insets(2, 2, 0, 0)
  })

  add(new JLabel("at"), new GridBagConstraints <| { c =>
    c.gridx = 1
    c.insets = new Insets(0, 5, 0, 0)
  })

  add(timeConfig, new GridBagConstraints <| { c =>
    c.gridx = 2
    c.insets = new Insets(2, 5, 0, 0)
  })

  add(new JLabel("UTC"), new GridBagConstraints <| { c =>
    c.gridx = 3
    c.insets = new Insets(2, 5, 0, 0)
  })

  add(go, new GridBagConstraints <| { c =>
    c.gridx = 4
    c.insets = new Insets(2, 5, 0, 0)
  })

  add(plot, new GridBagConstraints <| { c =>
    c.gridx = 5
    c.insets = new Insets(2, 2, 0, 0)
  })

  override def edit(ctx: GOption[ObsContext], target: SPTarget): Unit = {
    ct = target.getTarget.asInstanceOf[ConicTarget]
  }

}

