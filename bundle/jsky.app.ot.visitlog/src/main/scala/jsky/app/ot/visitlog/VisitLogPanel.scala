package jsky.app.ot.visitlog

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.obsrecord.ObsVisit

import jsky.app.ot.plugin.OtViewerService

import java.util.TimeZone
import java.awt.Color
import javax.swing.{AbstractAction, KeyStroke, JComponent, ImageIcon}

import scala.swing._
import scala.swing.event.{KeyTyped, ButtonClicked, MouseClicked}
import scala.swing.Swing._
import scala.swing.GridBagPanel
import scala.swing.GridBagPanel.Anchor._
import scala.swing.GridBagPanel.Fill.{Both, Horizontal}
import java.awt.event.{ActionEvent, KeyEvent}
import edu.gemini.skycalc.ObservingNight


object VisitLogPanel {
  var viewerService: Option[OtViewerService] = None

  def open(oid: SPObservationID): Unit =
    viewerService.foreach {
      vs => vs.loadAndView(oid)
    }

  private val LoadingColor = new Color(225, 225, 225)
  private val LoadedColor = new Color(238, 255, 204)
  private val NoDataColor = new Color(255, 255, 204)
  private val ErrorColor = new Color(253, 177, 177)

  private def load(name: String): ImageIcon =
    new ImageIcon(this.getClass.getResource(name))

  private val SpinnerIcon = load("spinner16.gif")
  private val ErrorIcon = load("error.gif")
  private val CalendarIcon = load("dates.gif")

  private def nightString(n: ObservingNight): String =
    s"${n.getSite.abbreviation} ${n.getNightString}"
}

import VisitLogPanel._
import VisitLogClient._

class VisitLogPanel(client: VisitLogClient, owner: Window) extends GridBagPanel {
  private var night: Option[ObservingNight] = None
  val site = client.peer.site

  border = EmptyBorder(10, 10, 10, 10)

  object titleLabel extends Label("") {
    font = font.deriveFont(font.getSize2D + 4)
  }

  object calendarButton extends Button {
    icon = CalendarIcon
    focusable = false
    tooltip = "Select an observing night to view."
  }

  class ButtonNoFocus(s: String) extends Button(s) {
    focusable = false
  }

  object backButton extends ButtonNoFocus("<") {
    tooltip = "View observation visits for the previous observing night."
  }

  object todayButton extends ButtonNoFocus("Today") {
    tooltip = "View observation visits for the current observing night."
  }

  object forwardButton extends ButtonNoFocus(">") {
    tooltip = "View observation visits for the next observing night."
  }

  listenTo(calendarButton, backButton, todayButton, forwardButton)
  reactions += {
    case ButtonClicked(`backButton`) =>
      night.foreach {
        n => client.load(n.previous())
      }
    case ButtonClicked(`todayButton`) =>
      client.load(new ObservingNight(site))
    case ButtonClicked(`forwardButton`) =>
      night.foreach {
        n => client.load(n.next())
      }
    case ButtonClicked(`calendarButton`) =>
      val cd = new CalendarDialog(owner)
      cd.pack()
      cd.setLocationRelativeTo(calendarButton)
      cd.visible = true
      cd.selectedDate.foreach {
        time => client.load(new ObservingNight(site, time))
      }
  }

  object navigatePanel extends GridBagPanel {
    layout(calendarButton) = new Constraints() {
      gridx = 0
      insets = new Insets(0, 0, 0, 10)
    }
    layout(backButton) = new Constraints() {
      gridx = 1
    }
    layout(todayButton) = new Constraints() {
      gridx = 2
    }
    layout(forwardButton) = new Constraints() {
      gridx = 3
    }
  }

  object headerPanel extends GridBagPanel {
    layout(titleLabel) = new Constraints() {
      anchor = West
    }

    layout(Swing.HGlue) = new Constraints() {
      gridx = 2
      fill = Horizontal
      weightx = 1.0
    }

    layout(navigatePanel) = new Constraints() {
      gridx = 3
    }
  }

  layout(headerPanel) = new Constraints() {
    insets = new Insets(0, 0, 10, 0)
    fill = Horizontal
    weightx = 1.0
  }

  val visitTable = new VisitTable

  def openSelectedObs(): Unit = {
    val index = visitTable.selection.rows.leadIndex
    if ((index >= 0) && (index < visitTable.visits.size)) {
      open(visitTable.visits(index).getObsId)
    }
  }

  // Instead of moving down a row, make the return key open the observation.
  val enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
  visitTable.peer.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enterKey, "OpenObs")
  visitTable.peer.getActionMap.put("OpenObs", new AbstractAction() {
    override def actionPerformed(evt: ActionEvent): Unit = openSelectedObs()
  })

  // Also open obs when the row is double clicked
  listenTo(visitTable.mouse.clicks)
  reactions += {
    case MouseClicked(_, _, _, 2, false) => openSelectedObs()
  }

  layout(new ScrollPane(visitTable)) = new Constraints() {
    gridy = 1
    fill = Both
    weightx = 1.0
    weighty = 1.0
  }

  object statusLabel extends Label("") {
    border = EmptyBorder(2, 2, 2, 2)
    foreground = Color.darkGray
    background = new Color(255, 255, 204)
    opaque = true
    horizontalAlignment = Alignment.Left
  }

  layout(statusLabel) = new Constraints() {
    gridy = 2
    fill = Horizontal
    anchor = West
    weightx = 1.0
    insets = new Insets(2, 0, 5, 0)
  }

  object localButton extends RadioButton(s"Local (${site.abbreviation})")

  listenTo(localButton)

  object utcButton extends RadioButton("UTC")

  listenTo(utcButton)

  new ButtonGroup(localButton, utcButton) {
    select(utcButton)
  }

  reactions += {
    case ButtonClicked(`localButton`) => visitTable.timeZone = site.timezone
    case ButtonClicked(`utcButton`) => visitTable.timeZone = TimeZone.getTimeZone("UTC")
  }

  val tzPanel = new GridBagPanel {
    layout(new Label("Time Zone")) = new Constraints() {
      gridx = 0
    }

    layout(localButton) = new Constraints() {
      gridx = 1
      insets = new Insets(0, 10, 0, 0)
    }

    layout(utcButton) = new Constraints() {
      gridx = 2
      insets = new Insets(0, 10, 0, 0)
    }

    layout(HGlue) = new Constraints() {
      gridx = 3
      fill = Horizontal
      weightx = 1.0
    }
  }

  layout(tzPanel) = new Constraints() {
    gridy = 3
    fill = Horizontal
    weightx = 1.0
  }

  private def update(n: ObservingNight, v: List[ObsVisit] = Nil): Unit = {
    night = Some(n)
    titleLabel.text = nightString(n)
    visitTable.visits = v
  }

  listenTo(client)
  reactions += {
    case Loading(n) =>
      update(n)
      statusLabel.text = s"Loading ${nightString(n)} ..."
      statusLabel.icon = SpinnerIcon
      statusLabel.background = LoadingColor
    case Loaded(n, v) =>
      update(n, v)
      statusLabel.icon = null
      if (v.size > 0) {
        statusLabel.text = "Double click table row to open observation in a viewer."
        statusLabel.background = LoadedColor
      } else {
        statusLabel.text = "There is no observation visit data for this night."
        statusLabel.background = NoDataColor
      }
    case IoProblem(n, e) =>
      update(n)
      statusLabel.text = "There was a network connection issue, try again later."
      statusLabel.icon = ErrorIcon
      statusLabel.background = ErrorColor
    case Error(n, t) =>
      update(n)
      statusLabel.text = "There was an unexpected error: " + Option(t.getMessage).getOrElse("")
      statusLabel.icon = ErrorIcon
      statusLabel.background = ErrorColor
  }
}
