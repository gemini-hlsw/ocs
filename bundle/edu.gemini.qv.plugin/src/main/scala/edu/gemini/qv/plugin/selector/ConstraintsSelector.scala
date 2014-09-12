package edu.gemini.qv.plugin.selector

import edu.gemini.qv.plugin.{ConstraintsChanged, TimeRangeChanged, QvContext, QvTool}
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.qv.plugin.ui.QvGui.ActionButton
import edu.gemini.qv.plugin.util.ConstraintsCache._
import edu.gemini.qv.plugin.util.ScheduleCache._
import edu.gemini.qv.plugin.util.SolutionProvider.ConstraintType
import edu.gemini.qv.plugin.util._
import edu.gemini.spModel.core.Site
import java.awt.Desktop
import java.net.{URL, URI}
import scala.swing._
import scala.swing.event.ButtonClicked

/**
 * UI element that allows to deselect constraints in order to look at different scenarios for observations.
 * The buttons are organised on two separate panels which gives more flexibility to place them in the UI.
 */
class ConstraintsSelector(ctx: QvContext) extends Publisher {

  private val mainConstraintsBtns = Seq(
    ConstraintBtn(
      "Elevation",
      "Restrict observations by elevation, hour angle and airmass constraints.",
      Set(Elevation)),
    ConstraintBtn(
      "Sky Brightness",
      "Restrict observations by sky brightness constraints.",
      Set(SkyBrightness)),
    ConstraintBtn(
      "Timing Windows",
      "Restrict observations by timing windows.",
      Set(TimingWindows)),
    ConstraintBtn(
      "Minimum Time",
      "Restrict by the minimum time needed to successfully observe an observation.",
      Set(MinimumTime))
  )

  private val scheduleConstraintsBtns = Seq(
    ConstraintBtn(
      "Instruments",
      "Restrict observations by instrument availability.",
      Set(InstrumentConstraint)),
    ConstraintBtn(
      "Telescope",
      "Take planned engineering and shutdown time and long duration weather events into account.",
      Set(EngineeringConstraint, ShutdownConstraint, WeatherConstraint)),
    ConstraintBtn(
      "Programs",
      "Take fast turnaround, laser availability and classical observing restrictions into account.",
      Set(FastTurnaroundConstraint, LaserConstraint, ProgramConstraint))
  )

  private val buttons = mainConstraintsBtns ++ scheduleConstraintsBtns


  object mainConstraints extends FlowPanel() {
    hGap = 0
    contents += new Label("Constraints:")
    contents += Swing.HStrut(5)
    contents ++= mainConstraintsBtns
  }

  object scheduleConstraints extends FlowPanel() {
    hGap = 0
    contents += new Label("Schedule:")
    contents += Swing.HStrut(5)
    contents ++= scheduleConstraintsBtns
    contents += Swing.HStrut(5)
    contents += ActionButton(
      "",
      "Show schedule constraints calendar in browser.",
      () => {
        Desktop.getDesktop.browse(SolutionProvider(ctx).telescopeScheduleUrl)
      },
      QvGui.CalendarIcon
    )
    contents += ActionButton(
      "",
      "Edit schedule constraints.",
      (button: Button) => {
        val editor = new ScheduleEditor(ctx, SolutionProvider(ctx).scheduleCache)
        editor.setLocationRelativeTo(button)
        editor.open()
        ConstraintsSelector.this.publish(TimeRangeChanged)
      },
      QvGui.EditIcon
    )
  }


  listenTo(buttons:_*)
  listenTo(SolutionProvider(ctx))
  listenTo(ctx)

  reactions += {

    case ConstraintCalculationStart(c, _) =>
      buttons.find(_.constraints.contains(c)).map(_.enabled = false)

    case ConstraintCalculationEnd(c, _) =>
      buttons.find(_.constraints.contains(c)).map(_.enabled = true)

    case ConstraintsChanged =>
      buttons.foreach(b => b.selected = b.constraints.intersect(ctx.selectedConstraints).isEmpty)

    case ButtonClicked(b) => b match {
      case b: ConstraintBtn =>
        ctx.selectedConstraints = selected
      case _ => // Ignore
    }

  }

  /**
   * Returns all currently selected constraints.
   * Note: We use those toggle buttons "the wrong way round", i.e. selecting one, deselects the constraint
   * and vice versa. By default all constraints are selected, i.e. at startup all buttons are deselected.
   * Ok, that's confusing, but it is as it is.
   * Note also: Above horizon constraint is always selected in order never to show observations as available
   * when they are below the horizon.
   * @return
   */
  def selected = buttons.filter(!_.selected).filter(_.enabled).map(_.constraints).flatten.toSet + AboveHorizon

  case class ConstraintBtn(label: String, tip: String, constraints: Set[ConstraintType]) extends ToggleButton {
    focusable = false
    text = label
    icon = QvGui.CheckIcon
    selectedIcon = QvGui.DelIcon
    disabledIcon = QvGui.Spinner16Icon
    tooltip = tip
    selected = constraints.intersect(ctx.selectedConstraints).isEmpty
  }

}
