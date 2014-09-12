package jsky.app.ot.too

import edu.gemini.pot.client.SPDB
import edu.gemini.pot.sp.{ISPProgram, ISPObservation}
import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.obs.ObsSchedulingReport
import edu.gemini.sp.vcs.{VcsFailure, VersionControlSystem, TrpcVcsServer}
import edu.gemini.too.event.api.TooEvent

import jsky.app.ot.userprefs.observer.ObserverPreferences
import jsky.app.ot.vcs.VcsGui
import jsky.app.ot.viewer.{ViewerService, SPViewer, ViewerManager}
import jsky.app.ot.viewer.open.OpenDialog

import java.util.logging.Logger
import javax.swing.{JOptionPane, JComponent}
import javax.swing.JOptionPane.WARNING_MESSAGE

import scala.collection.JavaConverters._
import jsky.app.ot.viewer.action.OpenAction
import scala.swing.Swing
import jsky.app.ot.OT


object TooHandler {
  val LOG = Logger.getLogger(classOf[TooHandler].getName)
}

import TooHandler.LOG

final class TooHandler(evt: TooEvent, peer: Peer, parent: JComponent) extends Runnable {

  // Handle opening a program viewer with the observation selected.  There are
  // several cases:
  //
  // 1) The program exists in the local database and it contains an observation
  //    with the matching observation id.  Just open it.
  //
  // 2) The program doesn't exist in the local database.  Check it out and open
  //    it (assuming the observation hasn't been deleted remotely).
  //
  // 3) The program exists in the local database but doesn't have the indicated
  //    observation.  Update the program and then open it.
  //
  private def openObs(report: ObsSchedulingReport) {
    val db = SPDB.get
    val oid = report.getObservationId
    val pid = oid.getProgramID

    def currentUser = OT.getKeyChain.subject.getPrincipals.asScala.toSet

    def update(p: ISPProgram): Either[String, ISPProgram] = {
      val srv = TrpcVcsServer(OT.getKeyChain, peer.host, peer.port)
      VersionControlSystem.apply(db, srv).update(pid, currentUser).toEither.left.map(failure => VcsFailure.explain(failure, pid, "update", Some(peer)))
    }

    // Left means the updated failed, Right(None) means we don't have the
    // program locally, Right(Some) means we have successfully updated the
    // local version of the program
    def lookupAndUpdate: Either[String, Option[ISPProgram]] =
      db.lookupProgramByID(pid) match {
        case null => Right(None)
        case p => update(p).right.map(Some.apply)
      }

    def checkout: Option[ISPProgram] =
      Option(OpenDialog.checkout(db, pid, peer, parent, VcsGui.registrar.get))

    def lookupObs(p: ISPProgram): Option[ISPObservation] =
      Option(p.getAllObservations).map(_.asScala.toList).flatMap(_.find(_.getObservationID == oid))

    val obs = for {
      updatedProg <- lookupAndUpdate.right
      p <- (updatedProg orElse checkout).toRight("Could not checkout program '%s'".format(pid)).right
      o <- lookupObs(p).toRight("Observation '%s' was removed remotely".format(oid)).right
    } yield o

    // If we failed, show the appropriate message.
    obs.left foreach { msg =>
        JOptionPane.showMessageDialog(parent, msg, "Could not show observation '%s'", WARNING_MESSAGE)
    }

    // If we succeeded, open the program and select the observation.  This is
    // easier said than done.
    obs.right.foreach { o =>
      // We're already on the EDT but there's an event that will happen.
      // See OT.initProgramReplacedSwap.  Something about that will screw up
      // the observation if we view it immediately.  This is poor.
      Swing.onEDT(ViewerManager.open(o))
    }
  }

  def notify(report: ObsSchedulingReport) {
    val audible = ObserverPreferences.fetch().isAudibleTooAlerts
    if (audible) {
      LOG.info("Starting audible ToO alert")
      TooAudioAlert.INSTANCE.alert()
    }

    LOG.info("Showing ToO dialog.")
    val tad = new TooAlertDialog
    tad.show(report, null)
    LOG.info("ToO alert dialog dismissed")

    if (audible) {
      TooAudioAlert.INSTANCE.dismiss()
      LOG.info("Stopped audible ToO alert")
    }

    if (tad.shouldViewObservation()) openObs(report)
  }

  def run() {
    LOG.info("OT now handling ToO alert.")
    notify(evt.report)
    LOG.info("ToO handled.")
  }
}

