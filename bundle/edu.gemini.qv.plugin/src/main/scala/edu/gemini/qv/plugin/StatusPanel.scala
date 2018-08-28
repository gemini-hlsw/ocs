package edu.gemini.qv.plugin

import edu.gemini.qv.plugin.data._
import edu.gemini.qv.plugin.selector.ReferenceDateSelector
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.qv.plugin.util.ConstraintsCache.{ConstraintCalculationEnd, ConstraintCalculationProgress, ConstraintCalculationStart}
import edu.gemini.qv.plugin.util.ScheduleCache.{ScheduleLoadStart, ScheduleLoadEnd, ScheduleLoadFailed}
import edu.gemini.qv.plugin.util._
import scala.swing.GridBagPanel.Anchor._
import scala.swing.GridBagPanel.Fill._
import scala.swing._


/**
 * Status panel that shows global messages and information about background tasks.
 * It also shows error messages in case any of the background tasks fail to complete successfully.
 */
class StatusPanel(ctx: QvContext) extends GridBagPanel {

  var constraintsTotal = 0
  var constraintsDone = 0

  val messageLabel = new Label()

  val observationsProgress = new ProgressBar {
    indeterminate = true
    visible = false
  }
  val observationsLabel = new Label(s"Total ${ctx.source.observations.size} loaded.")
  val observations = new FlowPanel {
    contents += new Label("Observations: ")
    contents += observationsProgress
    contents += observationsLabel
  }

  val constraintsProgress = new ProgressBar {
    visible = false
  }
  val constraintsLabel = new Label("None.")
  val constraints = new FlowPanel {
    contents += new Label("Constraints: ")
    contents += constraintsProgress
    contents += constraintsLabel
  }
  val scheduleLabel = new Label("None.")
  val schedule = new FlowPanel {
    contents += new Label("Schedule: ")
    contents += scheduleLabel
  }

  val referenceDate = new ReferenceDateSelector(ctx)

  layout(messageLabel) = new Constraints {
    gridx = 0
    weightx = 1
    anchor = West
    fill = Both
  }
  layout(new VerticalSeparator) = new Constraints {
    gridx = 1
    weighty = 1
    fill = Vertical
  }
  layout(referenceDate) = new Constraints {
    gridx = 2
  }
  layout(new VerticalSeparator) = new Constraints {
    gridx = 3
    weighty = 1
    fill = Vertical
  }
  layout(observations) = new Constraints {
    gridx = 4
  }
  layout(new VerticalSeparator) = new Constraints {
    gridx = 5
    weighty = 1
    fill = Vertical
  }
  layout(constraints) = new Constraints {
    gridx = 6
  }
  layout(new VerticalSeparator) = new Constraints {
    gridx = 7
    weighty = 1
    fill = Vertical
  }
  layout(schedule) = new Constraints {
    gridx = 8
  }

  listenTo(ctx.source, SolutionProvider(ctx))
  reactions += {

    // === events from data source (ODB)

    case DataSourceRefreshStart =>
      observationsProgress.visible = true
      observationsLabel.text = "Loading..."
      revalidate()

    case DataSourceRefreshEnd((os, _)) =>
      observationsProgress.visible = false
      observationsLabel.text = s"Total ${os.size} loaded."
      revalidate()

    // === events from caches (SolutionProvider)

    case ConstraintCalculationStart(c, cnt) =>
      constraintsTotal += cnt
      constraintsProgress.visible = true
      constraintsProgress.min = 0
      constraintsProgress.max = constraintsTotal
      constraintsProgress.labelPainted = true
      constraintsProgress.label = "0%"
      constraintsLabel.text = "Calculating..."
      revalidate()

    case ConstraintCalculationProgress(c, cnt) =>
      constraintsDone += cnt
      constraintsProgress.value = constraintsDone
      constraintsProgress.label = f"${100.0*constraintsDone/constraintsTotal}%3.0f%%"

    case ConstraintCalculationEnd(c, cnt) =>
      constraintsDone += cnt
      if (constraintsDone >= constraintsTotal) {
        constraintsTotal = 0
        constraintsDone = 0
        constraintsProgress.visible = false
        constraintsLabel.text = "All up to date."
        revalidate()
      }

    case ScheduleLoadStart =>
      scheduleLabel.icon = QvGui.Spinner16Icon
      scheduleLabel.text = "Loading..."

    case ScheduleLoadEnd =>
      scheduleLabel.icon = null
      scheduleLabel.text = "Loaded."

    case ScheduleLoadFailed =>
      scheduleLabel.icon = null
      scheduleLabel.text = "Not Available."

  }

  def showMessage(message: String) = messageLabel.text = message


  class VerticalSeparator extends Separator(Orientation.Vertical) {
    preferredSize = new Dimension(5,1)
  }

}
