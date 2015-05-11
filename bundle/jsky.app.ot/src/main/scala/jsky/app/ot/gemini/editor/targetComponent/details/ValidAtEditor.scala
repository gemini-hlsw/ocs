package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt._
import java.awt.event.{ActionEvent, ActionListener}
import java.text.SimpleDateFormat
import java.util.{Calendar, Date, TimeZone}
import javax.swing._

import language.implicitConversions

import edu.gemini.horizons.api.HorizonsQuery.ObjectType
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.gui.calendar.JCalendarPopup
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.{NonSiderealTarget, NamedTarget, ITarget, ConicTarget}
import jsky.app.ot.gemini.editor.horizons.HorizonsPlotter
import jsky.app.ot.gemini.editor.targetComponent.{TimeConfig, TelescopePosEditor}
import jsky.app.ot.ui.util.TimeDocument

import scalaz._, Scalaz._
import Horizons._

// [DATE] at [TIME] UTC [Go] [Plot]
abstract class ValidAtEditor[A <: ITarget](empty: A) extends JPanel with TelescopePosEditor with ReentrancyHack {

  private[this] var spt: SPTarget = new SPTarget(empty)
  private[this] var node: ISPNode = null

  val UTC = TimeZone.getTimeZone("UTC")

  val calendar = new JCalendarPopup { c =>
    c.setTimeZone(UTC)
    c.setMaximumWidth(150)
    c.setPreferredSize(c.getPreferredSize <| (_.width = 150))
    c.setMinimumSize(c.getPreferredSize)
  }

  val timeConfig = new JComboBox[TimeConfig] {

    val timeFormatter = new SimpleDateFormat("HH:mm:ss") <| (_.setTimeZone(UTC))
    val textField     = getEditor.getEditorComponent.asInstanceOf[JTextField] // ugh
    val textDoc       = new TimeDocument(textField) <| (_.setTime(timeFormatter.format(new Date)))

    textField.setDocument(textDoc) // tie the knot

    setEditable(true)
    setModel(new DefaultComboBoxModel(TimeConfig.values))
    setRenderer(new ListCellRenderer[TimeConfig] {
      val delegate = getRenderer // N.B. we can't implement this in Scala, but we delegate to the old one
      override def getListCellRendererComponent(list: JList[_ <: TimeConfig], value: TimeConfig, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component =
        delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) match {
          case label: JLabel => label <| (_.setText(value.displayValue))
        }
    })

    addActionListener(new ActionListener {
      override def actionPerformed(e: ActionEvent): Unit =
        nonreentrant {
          getSelectedItem match {
            case tc: TimeConfig => setTime(tc.getDate)
            case ts: String     => textDoc.setTime(ts) // I don't think this ever happens
          }
        }
    })

    def setTime(d: Date): Unit = {
      calendar.setDate(d)
      textDoc.setTime(timeFormatter.format(d))
    }

  }

  val go   = new JButton("Go")   <| { _.addActionListener(LookupListener(plot = false)) }
  val plot = new JButton("Plot") <| { _.addActionListener(LookupListener(plot = true))  }

  /**
   * An action listener that performs a catalog lookup, replacing the current target on success, and
   * plotting the ephemeris if desired.
   */
  case class LookupListener(plot: Boolean) extends ActionListener {
    override def actionPerformed(e: ActionEvent): Unit =
      lookupAndSet(plot, useCache = true).invokeAndWait
  }

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

  ///
  /// METHODS
  ///

  override def edit(context: GOption[ObsContext], target: SPTarget, node0: ISPNode): Unit = {
    spt = target
    node = node0
    nonreentrant {
      val nst = spt.getTarget.asInstanceOf[NonSiderealTarget]
      Option(nst.getDateForPosition).foreach { d =>
        calendar.setDate(d)
        timeConfig.setTime(d)
      }
    }
  }

  def objectTypeForTag(tag: ITarget.Tag): ObjectType =
    tag match {
      case ITarget.Tag.JPL_MINOR_BODY   => ObjectType.COMET
      case ITarget.Tag.MPC_MINOR_PLANET => ObjectType.MINOR_BODY
    }


  ///
  /// HORIZONS
  ///

  /** A program that returns the editor's current date and time. */
  def dateTime: HorizonsIO[Date] = {
    val cal = Calendar.getInstance(UTC)
    cal.setTimeZone(calendar.getTimeZone)
    cal.setTime(calendar.getDate)
    cal.set(Calendar.HOUR_OF_DAY, timeConfig.textDoc.getHoursField)
    cal.set(Calendar.MINUTE,      timeConfig.textDoc.getMinutesField)
    cal.set(Calendar.SECOND,      timeConfig.textDoc.getSecondsField)
    HorizonsIO.delay(cal.getTime)
  }

  /** A program that returns the editor's current target. */
  val target: HorizonsIO[A] =
    HorizonsIO.delay(spt.getTarget.asInstanceOf[A])

  /**
   * A program that looks up a new target with the same horizons information as the editor's
   * current target, if available, else the current target's name and type; and the date and time
   * indicated by the controls in the editor.
   */
  def lookupAction(useCache: Boolean): HorizonsIO[(A, Ephemeris)]

  /**
   * Constructs a program that replaces the current conic target. Note that this may result in this
   * editor being replaced entirely. This is ok!
   */
  def updateSPTarget(t: ITarget): HorizonsIO[Unit] =
    HorizonsIO.delay(spt.setTarget(t))

  /**
   * Program that implements the "go" button behavior: it looks up a conic target in Horizons based
   * on the current target's properties and the values in the time/date controls in the editor, and
   * replaces the current target if successful. It also plots the ephemeris if requested.
   */
  def lookupAndSet(plot: Boolean, useCache: Boolean): HorizonsIO[Unit] =
    lookupAction(useCache) >>= { case (target, ephemeris) =>
      updateSPTarget(target) *>
      plot.whenM(plotEphemeris(ephemeris))
    }

  def plotEphemeris(ephemeris: Ephemeris): HorizonsIO[Unit] =
    HorizonsIO.either {
      ephemeris.isEmpty either NoOrbitalElements or HorizonsPlotter.plot(node, ephemeris)
    }

}

class ConicValidAtEditor extends ValidAtEditor[ConicTarget](new ConicTarget) {

  def lookupAction(useCache: Boolean): HorizonsIO[(ConicTarget, Ephemeris)] =
    (target |@| dateTime).tupled >>= { case (ct, date) =>
      if (ct.isHorizonsDataPopulated) {
        lookupConicTargetById(
          ct.getName,
          ct.getHorizonsObjectId.toString,
          ct.getHorizonsObjectType,
          date,
          useCache)
      } else {
        lookupConicTargetByName(
          ct.getName,
          objectTypeForTag(ct.getTag),
          date)
      }
    }

}

class NamedValidAtEditor extends ValidAtEditor[NamedTarget](new NamedTarget) {

  def lookupAction(useCache: Boolean): HorizonsIO[(NamedTarget, Ephemeris)] =
    (target |@| dateTime).tupled >>= { case (nt, date) =>
      lookupSolarObject(nt.getName, nt.getSolarObject, date)
    }
}