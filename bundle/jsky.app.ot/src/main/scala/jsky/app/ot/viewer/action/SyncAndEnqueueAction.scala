package jsky.app.ot.viewer.action

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.obs.ObservationStatus
import jsky.app.ot.OTOptions
import jsky.app.ot.session.SessionQueue
import jsky.app.ot.viewer.{SPViewerActions, SPViewer}
import jsky.util.gui.DialogUtil

import java.awt.event.ActionEvent
import javax.swing.Action

//import scala.collection.JavaConverters._

final class SyncAndEnqueueAction(viewer: SPViewer) extends AbstractViewerAction(viewer, "Add to Session Queue", jsky.util.Resources.getIcon("Add24.gif", classOf[SPViewerActions])) {
  putValue(AbstractViewerAction.SHORT_NAME, "Queue")
  putValue(Action.SHORT_DESCRIPTION, "Sync changes and add the currently selected observation(s) to the session queue.")


  override def actionPerformed(evt: ActionEvent) {
    val selectedObservations: List[ISPObservation] = {
      val selNodes = Option(viewer.getTree.getSelectedNodes).map(_.toList).getOrElse(Nil)
      selNodes collect { case o: ISPObservation => o } match {
        case Nil => Option(viewer.getTree.getContextObservation).toList
        case os  => os
      }
    }

    try {
      selectedObservations match {
        case Nil => DialogUtil.error("No observation is currently selected")
        case os  => new VcsSyncAction(viewer).doAction(evt) foreach {
          _ foreach { _ => os foreach { SessionQueue.INSTANCE.addObservation(_) } }
        }
      }
    } catch {
      case ex: Exception => DialogUtil.error(ex)
    }
  }

  override def computeEnabledState: Boolean = {
    val pid = for {
      ed <- Option(viewer.getCurrentEditor)
      p  <- Option(ed.getProgram)
      id <- Option(p.getProgramID)
    } yield id

    def contextObsIsScheduleable: Boolean =
      Option(getContextNode(classOf[ISPObservation])) exists { ObservationStatus.computeFor(_).isScheduleable }

    pid exists { OTOptions.isStaff(_) && contextObsIsScheduleable }
  }
}
