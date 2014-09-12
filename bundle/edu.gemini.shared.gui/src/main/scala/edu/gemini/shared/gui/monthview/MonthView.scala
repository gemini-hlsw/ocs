package edu.gemini.shared.gui.monthview

import org.jdesktop.swingx.JXMonthView

import java.awt.{Insets, Color}
import java.util.{TimeZone, Calendar, Date}
import java.util.Calendar.{YEAR, MONTH, DAY_OF_MONTH, MILLISECOND}
import javax.swing.BorderFactory
import scala.swing.{BorderPanel, Button, Component}
import scala.swing.event.ButtonClicked

object MonthView {
  private val StringBgColor = new Color(175, 214, 176)
  val DefaultNumberOfMonths = 2

  case class Ymd(year: Int, month: Int, day: Int) {
    require(month >= 1 && month <= 12)
    require(day >= 1 && day <= 31)

    def timeAtMidnight: Long = timeAtMidnight(TimeZone.getDefault)

    def timeAtMidnight(tz: TimeZone): Long = {
      val c = Calendar.getInstance(tz)
      c.set(year, month-1, day, 0, 0, 0)
      c.set(MILLISECOND, 0)
      c.getTimeInMillis
    }
  }

  object Ymd {
    def apply(time: Long): Ymd = apply(time, TimeZone.getDefault)

    def apply(time: Long, tz: TimeZone): Ymd = {
      val c = Calendar.getInstance(tz)
      c.setTimeInMillis(time)
      Ymd(c.get(YEAR), c.get(MONTH) + 1, c.get(DAY_OF_MONTH))
    }
  }
}

import MonthView._

class MonthView(val selectionMode: DateSelectionMode = DateSelectionMode.Single,
                val numMonthsToShow: Int = 2, val rows: Int = 1) extends BorderPanel {

  private val monthView = new JXMonthView() {
    setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 0, 1, 1),
      BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY),
      BorderFactory.createEmptyBorder(11, 4, 0, 4))))
    setMonthStringInsets(new Insets(-3, 0,-3, 0))
    setPreferredColumnCount(numMonthsToShow / rows)
    setPreferredRowCount(rows)
    setMonthStringBackground(StringBgColor)

    // TODO: We may want to change this later to support multiple selection intervals. Spiffy!
    setSelectionMode(selectionMode.correspondingType)

    val today = new Date()
    setFirstDisplayedDay(today)
    setSelectionInterval(today, today)
  }

  // Set up the layout of the panel.
  private val leftButton  = new NavButton("<", -1)
  private val rightButton = new NavButton(">",  1)

  layout(Component.wrap(monthView)) = BorderPanel.Position.Center
  layout(leftButton)                = BorderPanel.Position.West
  layout(rightButton)               = BorderPanel.Position.East

  private class NavButton(label: String, delta: Int) extends Button {
    text = label
    focusable = false
    listenTo(this)
    reactions += {
      case ButtonClicked(_) =>
        val cal = Calendar.getInstance()
        cal.setTimeInMillis(monthView.getFirstDisplayedDay.getTime)
        cal.add(MONTH, delta)
        monthView.setFirstDisplayedDay(new Date(cal.getTimeInMillis))
    }
  }

  def selectionInterval: (Ymd, Ymd) = {
    val f = monthView.getFirstSelectionDate.getTime
    val fYmd = Ymd(f)

    val l = monthView.getLastSelectionDate.getTime
    if (f == l) (fYmd, fYmd)
    else (fYmd, Ymd(l))
  }

  def selection: Ymd = selectionInterval._1

  def selectionInterval_=(interval: (Ymd, Ymd)): Unit = {
    val (fYmd, lYmd) = interval
    val f = new Date(fYmd.timeAtMidnight)
    val l = if (fYmd == lYmd) f else new Date(lYmd.timeAtMidnight)
    monthView.setFirstDisplayedDay(f)
    monthView.setSelectionInterval(f, l)
  }

  def selection_=(sel: Ymd): Unit = {
    selectionInterval = (sel, sel)
  }
}