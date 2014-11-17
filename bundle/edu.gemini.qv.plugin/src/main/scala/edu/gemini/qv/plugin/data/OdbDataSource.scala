package edu.gemini.qv.plugin.data

import java.util.logging.Logger

import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.pot.spdb.IDBQueryRunner
import edu.gemini.qpt.shared.sp.{MiniModel, Obs, ObsQueryFunctor}
import edu.gemini.qv.plugin.QvTool
import edu.gemini.qv.plugin.data.OdbDataSource._
import edu.gemini.qv.plugin.ui.QvGui
import edu.gemini.spModel.core.{Peer, ProgramType, Semester}
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.util.trpc.client.TrpcClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.Swing._

/** Logging. */
object OdbDataSource {
  val LOG = Logger.getLogger(OdbDataSource.getClass.getName)
}

/**
 * Implementation of an observation data source that gets observations from the ODB.
 */
class OdbDataSource(val peer: Peer, mt: MagnitudeTable) extends DataSource {

  def site = peer.site

  /**
   * Initiates refresh of data and update in the background.
   *
   * @return
   */
  def refresh: Future[Set[Obs]] = {

    publish(DataSourceRefreshStart)

    LOG.info(s"refreshing data in background; peer=${peer.displayName}")
    LOG.info("  >>> completed = " + includeCompletedPrograms)
    LOG.info("  >>> semesters = " + selectedSemesters)
    LOG.info("  >>> types     = " + selectedTypes)
    LOG.info("  >>> classes   = " + selectedClasses)
    LOG.info("  >>> statuses  = " + selectedStatuses)

    // create real java collections to avoid problem with serialization of scala/java wrappers
    val javaSemesters = new java.util.HashSet[Semester]; selectedSemesters.foreach(javaSemesters.add)
    val javaClasses  = new java.util.HashSet[ObsClass](); selectedClasses.foreach(javaClasses.add)
    val javaStatuses = new java.util.HashSet[ObservationStatus](); selectedStatuses.foreach(javaStatuses.add)
    val javaTypes = new java.util.ArrayList[ProgramType](); selectedTypes.foreach(javaTypes.add)
    val functor = new ObsQueryFunctor(peer.site, javaSemesters, javaTypes, javaClasses, javaStatuses, !includeCompletedPrograms, !includeInactivePrograms, mt)

    // create and initiate db read operation
    val c = {
      val b = TrpcClient(peer)
      QvTool.authClient.map(b.withKeyChain).getOrElse(b.withoutKeys)
    }
    val f = c future { r =>
      val result = r[IDBQueryRunner].queryPrograms(functor)
      val model = MiniModel.newInstanceFromExecuted(peer, result)
      val obs = model.getAllObservations
      // transform from mutable java to immutable scala set
      val sObs = scala.collection.JavaConversions.asScalaSet[Obs](obs).toSet
      observations = sObs
      observations

    } andThen {
      case _ => onEDT(publish(DataSourceRefreshEnd(observations)))
    }

    f.onFailure {
      case t: Throwable => QvGui.showError("Could not load observations", s"An error happened when trying to load observations from ${peer.displayName}.", t)
    }

    f

  }


}
