package edu.gemini.qv.plugin.selector

import edu.gemini.qv.plugin.{QvContext, TimeRangeChanged, TimeValueChanged, TimeZoneChanged}
import edu.gemini.qv.plugin.selector.TimeRangeSelector._
import edu.gemini.qv.plugin.ui.{CalendarDialog, QvGui}
import edu.gemini.shared.gui.monthview.DateSelectionMode
import edu.gemini.spModel.core.Semester
import java.awt.event.{AdjustmentEvent, AdjustmentListener}
import java.util.TimeZone

import edu.gemini.shared.util.DateTimeUtils

import scala.concurrent.duration._
import scala.swing._
import scala.swing.event.ButtonClicked

object TimeRangeSelector {
  abstract class RangeType(val duration: Long, val increment: Long)
  object Day      extends RangeType(1.day.toMillis,     1.day.toMillis)
  object Week     extends RangeType(7.days.toMillis,    1.day.toMillis)
  object Month    extends RangeType(30.days.toMillis,   1.day.toMillis)
  object Quarter  extends RangeType(92.days.toMillis,  30.days.toMillis)
  object HalfYear extends RangeType(183.days.toMillis, 30.days.toMillis)
  object Year     extends RangeType(365.days.toMillis, 30.days.toMillis)
  object Custom   extends RangeType(0,                  0)
}

class TimeRangeSelector(ctx: QvContext, semesters: Seq[Semester]) extends GridBagPanel {

  private val rangeButtonGroup = new ButtonGroup()
  private val timeRange = new TimeRangePanel
  private val userRange = new CustomRangePanel
  private val scrollBar = new TimeScrollBar
  private val timeSelector = new TimeSelectorPanel

  layout(timeRange) = new Constraints() {
    gridx = 0
  }
  layout(Swing.HStrut(10)) = new Constraints() {
    gridx = 1
  }
  layout(userRange) = new Constraints() {
    gridx = 2
  }
  layout(Swing.HStrut(10)) = new Constraints() {
    gridx = 3
  }
  layout(scrollBar) = new Constraints() {
    gridx = 4
    weightx = 1.0
    fill = GridBagPanel.Fill.Horizontal
  }
  layout(Swing.HStrut(10)) = new Constraints() {
    gridx = 5
  }
  layout(timeSelector)  = new Constraints() {
    gridx = 6
  }

  listenTo(ctx)
  reactions += {
    case TimeValueChanged => scrollBar.updateScrollBar()
    case TimeRangeChanged => scrollBar.updateScrollBar()
  }

  def start = {
      if (scrollBar.enabled) ctx.range.start + scrollBar.value.hours.toMillis
      else if (ctx.rangeType == Custom) ctx.customStart
      else ctx.range.start
  }

  def end = {
      if (scrollBar.enabled) start + scrollBar.visibleAmount.hours.toMillis
      else if (ctx.rangeType == Custom) ctx.customEnd
      else ctx.range.end
  }

  def selectedZone = ctx.selectedTimeZone
  def showSingleDay = ctx.rangeType == Day
  def daysShowing = (ctx.rangeType.duration / DateTimeUtils.MillisecondsPerDay).toInt

  class TimeScrollBar extends ScrollBar with AdjustmentListener {
    orientation = Orientation.Horizontal
    updateScrollBar()

    // seems Scala event handling is broken/not implemented..
    // use plain vanilla Java listener instead
    peer.addAdjustmentListener(this)

    // update current range of scrollbar
    def updateScrollBar(): Unit = {

      // all units in hours!
      val visibleRange = Math.ceil(ctx.rangeType.duration.milliseconds.toHours).toInt
      val fullRange    = Math.ceil(ctx.range.duration.milliseconds.toHours).toInt
      val increment    = Math.ceil(ctx.rangeType.increment.milliseconds.toHours).toInt

      minimum = 0
      maximum = fullRange
      unitIncrement = increment
      blockIncrement = visibleRange
      if (visibleRange >= fullRange) {
        enabled = false
        visibleAmount = fullRange
      } else if (ctx.rangeType == Custom) {
        val v = Math.max((ctx.customStart - ctx.range.start).milliseconds.toHours, 0).toInt
        enabled = false
        visibleAmount = (ctx.customEnd - ctx.customStart).milliseconds.toHours.toInt
        value = v - v % 24
      } else {
        enabled = true
        visibleAmount = visibleRange - visibleRange % 24    // only allow multiples of 24hrs
        value = ctx.rangeValue - ctx.rangeValue % 24        // only allow multiples of 24hrs
      }

    }

    def adjustmentValueChanged(e: AdjustmentEvent): Unit = {
      ctx.rangeValue = scrollBar.value
    }
  }

  class TimeRangePanel extends FlowPanel() {

    private val buttons = Seq(
      TimeRangeBtn("D", "1 Day", Day),
      TimeRangeBtn("W", "1 Week (7 Days)", Week),
      TimeRangeBtn("M", "1 Month (30 Days)", Month),
      TimeRangeBtn("3M", "3 Months (90 Days)", Quarter),
      TimeRangeBtn("6M", "6 Months (180 Days)", HalfYear),
      TimeRangeBtn("Y", "1 Year (365 Days)", Year)
    )

    updateTimeRange()

    listenTo(ctx)
    reactions += {
      case TimeRangeChanged => updateTimeRange()
    }

    hGap = 0
    rangeButtonGroup.buttons ++= buttons
    contents ++= buttons

    private def updateTimeRange() =
      buttons.filter(_.range == ctx.rangeType).foreach(_.selected = true)

  }

  class CustomRangePanel extends FlowPanel() {
    private val customRange = TimeRangeBtn("R", "User defined range.", Custom)

    private object calendarButton extends Button {
      icon = QvGui.CalendarIcon
      focusable = false
      tooltip = "Select start and end dates for user defined range (R)."
    }

    hGap = 0
    rangeButtonGroup.buttons += customRange
    contents += customRange
    contents += calendarButton

    updateButton()

    listenTo(ctx, calendarButton)
    reactions += {
      case ButtonClicked(`calendarButton`) =>
        val cd = new CalendarDialog("Select Start and End Date for Custom Range", ctx.customStart, ctx.customEnd - 24.hours.toMillis, DateSelectionMode.Interval)
        cd.pack()
        cd.setLocationRelativeTo(calendarButton)
        cd.visible = true
        cd.startDate.foreach {
          time => ctx.customStart = DateTimeUtils.startOfDayInMs(time + 14.hours.toMillis, ctx.timezone.toZoneId)
        }
        cd.endDate.foreach {
          time => ctx.customEnd = DateTimeUtils.endOfDayInMs(time + 14.hours.toMillis, ctx.timezone.toZoneId)
        }
        // force update
        cd.startDate.foreach {
          t => ctx.rangeType = Custom
        }
      case TimeRangeChanged => updateButton()
    }

    private def updateButton() =
      if (ctx.rangeType == Custom) customRange.selected = true

  }

  class TimeSelectorPanel extends FlowPanel() {

    private val buttons = Seq(
      TimeTypeBtn("UTC", "Universal Time", TimeZone.getTimeZone("UTC")),
      TimeTypeBtn("Local", "Local Time at Site", ctx.timezone),
      TimeTypeBtn("LST", "Local Sidereal Time", ctx.lstTimezone)
    )

    new ButtonGroup(buttons:_*)
    contents ++= buttons

    listenTo(ctx)
    reactions += {
      case TimeZoneChanged => updateTimeZone()
    }

    updateTimeZone()

    private def updateTimeZone(): Unit =
      buttons.foreach(b => b.selected = b.timeZone == ctx.selectedTimeZone)

    case class TimeTypeBtn(label: String, tip: String, timeZone: TimeZone) extends RadioButton {
      focusable = false
      action = new Action(label) {
        toolTip = tip
        def apply() = ctx.selectedTimeZone = timeZone
      }
    }

  }

  case class TimeRangeBtn(label: String, tip: String, range: RangeType, select: Boolean = false) extends ToggleButton {
    focusable = false
    selected = select
    action = new Action(label) {
      toolTip = tip
      def apply() = ctx.rangeType = range
    }
  }


}
