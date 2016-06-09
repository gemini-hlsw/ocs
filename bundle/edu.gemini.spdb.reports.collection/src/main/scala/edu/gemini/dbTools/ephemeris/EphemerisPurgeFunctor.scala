package edu.gemini.dbTools.ephemeris

import edu.gemini.pot.sp.{ISPObservation, ISPNode}
import edu.gemini.pot.spdb.{IDBDatabaseService, DBAbstractQueryFunctor}
import edu.gemini.spModel.rich.pot.sp.obsWrapper
import edu.gemini.spModel.target.EphemerisPurge

import java.security.Principal
import java.util.logging.{Level, Logger}
import java.util.{Set => JSet}

/** An ODB observation query functor that finds all executed non-sidereal
  * observations and purges ephemeris data.
  */
object EphemerisPurgeFunctor {
  private final val Log = Logger.getLogger(EphemerisPurgeFunctor.getClass.getName)

  def query(db: IDBDatabaseService, users: JSet[Principal]): Unit =
    db.getQueryRunner(users).queryObservations(new EphemerisPurgeFunctor)
}

private class EphemerisPurgeFunctor extends DBAbstractQueryFunctor {
  import EphemerisPurgeFunctor.Log

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: JSet[Principal]): Unit =
    node match {
      case o: ISPObservation =>
        EphemerisPurge.purge(o).filter(_ => o.isObserved).foreach { action =>
          Log.log(Level.INFO, s"Purge ephemeris data in: ${Option(o.getObservationID).getOrElse(o.getNodeKey)}.")
          action.unsafePerformIO()
        }

      case _                 => // do nothing
    }
}
