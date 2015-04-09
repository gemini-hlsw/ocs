package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt._
import java.awt.event.{ActionEvent, ActionListener, ItemEvent, ItemListener}
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import javax.swing._
import javax.swing.event.DocumentEvent

import edu.gemini.horizons.api.HorizonsQuery.ObjectType
import edu.gemini.shared.gui.calendar.JCalendarPopup
import edu.gemini.shared.gui.text.AbstractDocumentListener
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.{ITarget, ConicTarget}
import jsky.app.ot.gemini.editor.targetComponent.{TimeConfig, TelescopePosEditor}
import jsky.app.ot.ui.util.TimeDocument

import scalaz._, Scalaz._

// [DATE] at [TIME] UTC [Go] [Plot]
class ValidAtEditor extends JPanel with TelescopePosEditor with ReentrancyHack {

  private[this] var spt: SPTarget = new SPTarget(new ConicTarget)

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

  val go = new JButton("Go") <| { b =>
    b.addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit =
        goAction.run.runAsync { e =>
          e.leftMap[Horizons.HorizonsFailure](Horizons.UnknownError).join match {
            case -\/(e)      => println(e)
            case _ => // done
          }
        }
    })
  }

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
    spt = target
  }

  def objectTypeForTag(tag: ITarget.Tag): ObjectType =
    tag match {
      case ITarget.Tag.JPL_MINOR_BODY   => ObjectType.COMET
      case ITarget.Tag.MPC_MINOR_PLANET => ObjectType.MINOR_BODY
    }

  /** A program that returns the editor's current date and time. */
  def dateTime: Horizons.HorizonsIO[Date] =
    Horizons.HorizonsIO.delay(new Date) // TODO: get from controls

  /** A program that returns the editor's current conic target. */
  val conicTarget: Horizons.HorizonsIO[ConicTarget] =
    Horizons.HorizonsIO.delay(spt.getTarget.asInstanceOf[ConicTarget])

  /**
   * Construct a program that looks up a new conic target with the same horizons information as the
   * editor's current target, if available, else the current target's name and type; and the date
   * and time indicated by the controls in the editor.
   */
  def lookupAction: Horizons.HorizonsIO[(ConicTarget, Horizons.Ephemeris)] =
    (conicTarget |@| dateTime).tupled >>= { case (ct, date) =>
      if (ct.isHorizonsDataPopulated) {
        Horizons.lookupConicTargetById(
          ct.getHorizonsObjectId.toString,
          ct.getHorizonsObjectTypeOrdinal,
          date)
      } else {
        Horizons.lookupConicTargetByName(
          ct.getName,
          objectTypeForTag(ct.getTag),
          date)
      }
    }

  /**
   * Constructs a program that replaces the current conic target. Note that this may result in this
   * editor being replaced entirely. This is ok!
   */
  def updateSPTarget(t: ConicTarget): Horizons.HorizonsIO[Unit] =
    Horizons.HorizonsIO.delay(spt.setTarget(t))

  /**
   * Program that implements the "go" button behavior: it looks up a conic target in Horizons based
   * on the current target's properties and the values in the time/date controls in the editor, and
   * replaces the current target if successful.
   */
  val goAction: Horizons.HorizonsIO[Unit] =
    lookupAction >>= { case (target, ephemeris) => updateSPTarget(target) }

}

