package edu.gemini.spModel.obsrecord

import edu.gemini.pot.spdb.{IDBDatabaseService, DBAbstractFunctor}
import edu.gemini.pot.sp.ISPNode
import edu.gemini.skycalc.ObservingNight
import java.security.Principal

final class ObsVisitFunctor(val night: ObservingNight) extends DBAbstractFunctor {
  private var visitList: List[ObsVisit] = Nil

  override def execute(db: IDBDatabaseService, node: ISPNode, ps: java.util.Set[Principal]): Unit = {
    visitList = ObsVisitService.visitsForNight(db, night)
  }

  def visits: List[ObsVisit] = visitList
}
