package edu.gemini.too.event.service

import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.{ProgramEvent, ProgramEventListener, IDBTriggerAction, IDBDatabaseService}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.obs.{ObsClassService, ObservationStatus, ObsSchedulingReport}
import ObservationStatus.{READY, ON_HOLD}
import edu.gemini.spModel.obsclass.ObsClass.SCIENCE
import edu.gemini.spModel.too.{Too, TooType}
import edu.gemini.too.event.api.{TooEvent, TooService => TooServiceApi, TooPublisher, TooTimestamp}
import edu.gemini.util.security.permission.ProgramPermission
import edu.gemini.util.security.policy.ImplicitPolicy

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.security.Principal

object TooService {
  val DefaultEventRetentionTime = 30 * 60 * 1000
}

/**
 * The TooService is notified by the database whenever the TooCondition matches
 * a change event.  It creates a correspond TooEvent, publishes it to any local
 * subscribers and holds on to it (for a limited time) in case remote clients
 * should poll for updates.
 *
 * @param eventRetentionTime minimum tme that ToO events will be kept
 */
class TooService(db: IDBDatabaseService, val site: Site, val eventRetentionTime: Long = TooService.DefaultEventRetentionTime) extends IDBTriggerAction with ProgramEventListener[ISPProgram] with TooPublisher { outer =>
  private var timestamp                    = TooTimestamp.now
  private var recentEvents: List[TooEvent] = Nil

  def lastEventTimestamp: TooTimestamp = synchronized { timestamp }

  def serviceApi(ps: java.util.Set[Principal]): TooServiceApi =
    new TooServiceApi {

      def events(since: TooTimestamp): java.util.List[TooEvent] = {
        def isVisible(evt: TooEvent): Boolean =
          ImplicitPolicy.forJava.hasPermission(db, ps, new ProgramPermission.Read(evt.report.getObservationId.getProgramID))

        (recentEvents takeWhile { _.timestamp > since} filter { isVisible }).reverse.asJava
      }

      def lastEventTimestamp: TooTimestamp =
        outer.lastEventTimestamp

      def eventRetentionTime: Long =
        outer.eventRetentionTime

    }

  private def trigger(obsList: List[ISPObservation]) {
    val time   = TooTimestamp.now
    val events = obsList map { obs =>
      val report  = new ObsSchedulingReport(obs, site, time.value)
      TooEvent(report, Too.get(obs), time)
    }

    val cutoff = time.less(eventRetentionTime)

    synchronized {
      recentEvents = events ++ (recentEvents filter { _.timestamp > cutoff })
      timestamp    = time
    }

    if (obsList.nonEmpty) Future {
      events foreach { evt => publish(evt) }
    }
  }

  def doTriggerAction(change: SPCompositeChange, handback: Object) {
    // This solution assumes you will never be able to process multiple events in
    // the same millisecond.  If you could and a client happened to poll in the
    // middle of doing that, it would miss subsequent events in the same
    // millisecond.
    val obs = handback.asInstanceOf[ISPObservation]
    trigger(List(obs).filter(o => Option(o.getObservationID).isDefined))
  }

  def programReplaced(pme: ProgramEvent[ISPProgram]) {
    def isTooProgram(p: ISPProgram) =
      if (Option(p.getProgramID).isEmpty) false
      else {
        val dObj = p.getDataObject.asInstanceOf[SPProgram]
        dObj.isActive && dObj.getTooType != TooType.none
      }

    def obsStatus(n: ISPProgram): Seq[(SPNodeKey, ObservationStatus)] = {
      val obsList = n.getAllObservations.asScala
      obsList.map { o => o.getNodeKey -> ObservationStatus.computeFor(o) }
    }

    def obsList(n: ISPProgram, ks: Set[SPNodeKey]): List[ISPObservation] =
      if (ks.isEmpty) Nil  // just a shortcut ...
      else n.getAllObservations.asScala.filter(o => ks.contains(o.getNodeKey)).toList

    val oldProg = pme.getOldProgram
    val newProg = pme.getNewProgram

    if (isTooProgram(oldProg) && isTooProgram(newProg)) {
      val oldStatuses = obsStatus(oldProg)
      val newStatuses = obsStatus(newProg)

      def keySet(tups: Seq[(SPNodeKey, ObservationStatus)], status: Option[ObservationStatus] = None): Set[SPNodeKey] =
        status.fold(tups)(s => tups.filter(_._2 == s)).map(_._1).toSet

      val allOldKeys = keySet(oldStatuses)
      val oldOnHold  = keySet(oldStatuses, Some(ON_HOLD))
      val newReady   = keySet(newStatuses, Some(READY))
      val transitionTrigger = oldOnHold & newReady
      val creationTrigger   = newReady.filterNot(allOldKeys.contains)

      val newlyReadyObsList = obsList(newProg, transitionTrigger ++ creationTrigger)
      val triggerObsList    = newlyReadyObsList.filter { o =>
        // REL-566: ignore anything but science observations.
        ObsClassService.lookupObsClass(o) == SCIENCE
      }

      trigger(triggerObsList)
    }
  }

  def programAdded(pme: ProgramEvent[ISPProgram]) { /* ignore */ }
  def programRemoved(pme: ProgramEvent[ISPProgram]) { /* ignore */ }
}

