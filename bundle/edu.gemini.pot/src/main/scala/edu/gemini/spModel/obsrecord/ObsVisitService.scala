package edu.gemini.spModel.obsrecord

import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.plan.NightlyRecord
import edu.gemini.spModel.obs.{ InstrumentService, ObsClassService }
import edu.gemini.spModel.obslog.ObsLog

import scala.collection.JavaConverters._
import scala.math.Ordering.comparatorToOrdering
import edu.gemini.skycalc.ObservingNight


object ObsVisitService {
  def visitsForNight(db: IDBDatabaseService, night: ObservingNight): List[ObsVisit] = {
    def pid    = SPProgramID.toProgramID(s"${night.getSite.name}-PLAN${night.getNightString}")
    def plan   = Option(db.lookupNightlyRecordByID(pid))
    def oidSet = plan.map(_.getDataObject.asInstanceOf[NightlyRecord].getObservationList.asScala.toSet).getOrElse(Set.empty)

    oidSet.toList.flatMap { oid =>
      (for {
        obs <- Option(db.lookupObservationByID(oid))
        oc   = ObsClassService.lookupObsClass(obs)
        log <- Option(ObsLog.getIfExists(obs))
        inst = InstrumentService.lookupInstrument(obs)
      } yield log.getVisits(inst, oc, night.getStartTime, night.getEndTime).toList).getOrElse(Nil)
    }.sorted(comparatorToOrdering(ObsVisit.START_TIME_COMPARATOR))
  }
}
