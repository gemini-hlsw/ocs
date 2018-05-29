package jsky.app.ot.viewer.action

import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.obs.context.ObsContext
import jsky.app.ot.gemini.schedulingBlock.{ SchedulingBlockDialog, SchedulingBlockUpdate }
import jsky.app.ot.viewer.SPViewer
import java.awt.event.ActionEvent

final class SchedulingBlockAction(viewer: SPViewer) extends AbstractViewerAction(viewer, "Set Scheduling Block ...") {

  override def actionPerformed(evt: ActionEvent): Unit =
    Option(viewer.getSelectedObservations).filter(_.nonEmpty).foreach { obsNs =>
      val owner = viewer.getParentFrame
      SchedulingBlockDialog.prompt(
        "Observation Scheduling",
        Some("Select the date and time to schedule the selected observations."),
        owner,
        obsNs.head,
        ObsContext.getSiteFromObservation(obsNs.head).asScalaOpt,
        SchedulingBlockDialog.DateTimeOnly
      ).foreach(SchedulingBlockUpdate.run(owner, _, obsNs.toList))
    }

  override def computeEnabledState: Boolean =
    Option(viewer.getSelectedObservations).exists(_.nonEmpty)

}
