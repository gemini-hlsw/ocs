package jsky.app.ot.viewer

import edu.gemini.pot.sp.{ISPProgram, ISPObservation, SPObservationID}
import edu.gemini.pot.spdb.{ProgramSummoner, IDBDatabaseService}
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.sp.vcs.{VcsFailure, VcsServer, VersionControlSystem}
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.util.trpc.client.TrpcClient

import jsky.app.ot.plugin.OtViewerService
import jsky.app.ot.userprefs.observer.ObservingPeer

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._
import jsky.app.ot.OT

object ViewerService {
  var instance: Option[ViewerService] = None
}

/**
 * Service that keeps track of main views of the application in order to decide when to shutdown the whole app
 * and also allows to open programs and observations in separate viewers (SPViewer).
 */
class ViewerService(localOdb: IDBDatabaseService, reg: VcsRegistrar) extends OtViewerService {

  // -- Manage all views that should keep application open (e.g. plugin windows, SPViewers etc).

  /** A set of views that keep the application open. Once all of these views are closed, the application shuts down. */
  var views: Set[AnyRef] = Set()
  /** Register a main view. */
  def registerView(view: AnyRef) = synchronized {
    views += view
  }
  /** Unregister a view; if this is the last registered view the application is shut down. */
  def unregisterView(view: AnyRef) = synchronized {
    views -= view
    if (views.size == 0) {
      // this is a bit brutal, would be nice to have a softer shutdown; e.g. notify all registered views/plugins first
      System.exit(0)
    }
  }

  // -- Provide functionality to display programs and observations in separate viewer windows.

  /**
   * Load the program with the given program id either from the provided
   * local ODB or from the remote ODB associated with the id.  Performs a
   * checkout of the program into the local ODB if necessary.
   */
  def load(pid: SPProgramID): \/[VcsFailure, ISPProgram] = {
    def notFound     = VcsFailure.SummonFailure(ProgramSummoner.IdNotFound(pid))
    def localProgram = Option(localOdb.lookupProgramByID(pid)).\/>(notFound)
    def peer         = (reg.registration(pid) orElse ObservingPeer.get).\/>(notFound)

    // Sorry for the hoop-jumping.  Going from Validation to \/ complicates it
    def checkoutProgram = peer.flatMap { p =>
      TrpcClient(p).withKeyChain(OT.getKeyChain) { r =>
        val res = VersionControlSystem(localOdb, r[VcsServer]).checkout(pid)
        // perform the side-effect or registering this result in the vcs reg ...
        res.foreach { _ => reg.register(pid, p) }
        res
      }.fold(ex => VcsFailure.VcsException(ex).left[ISPProgram], identity)
    }

    localProgram ||| checkoutProgram
  }

  /**
   * Loads the program in which this observation is found into the local ODB
   * (checking it out if necessary) and then finds the observation inside of
   * that program (if defined).
   */
  def load(oid: SPObservationID): \/[VcsFailure, Option[ISPObservation]] =
    load(oid.getProgramID).rightMap { p =>
      p.getAllObservations.asScala.find(_.getObservationID == oid)
    }

  /**
   * Loads the program associated with the given id, checking it out if
   * necessary, and then shows it in a viewer if successful.
   */
  def loadAndView(pid: SPProgramID): \/[VcsFailure, ISPProgram] = {
    val res = load(pid)
    res.foreach { prog => ViewerManager.open(prog) }
    res
  }

  /**
   * Loads the program associated with the given observation id, checking it
   * out if necessary, and then shows the given observation in a viewer.
   */
  def loadAndView(oid: SPObservationID): \/[VcsFailure, Option[ISPObservation]] = {
    val res = load(oid)
    res.foreach { _.foreach { obs => ViewerManager.open(obs) } }
    res
  }
}
