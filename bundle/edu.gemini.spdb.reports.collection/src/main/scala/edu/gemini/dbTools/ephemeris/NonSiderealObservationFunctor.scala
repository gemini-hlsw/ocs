package edu.gemini.dbTools.ephemeris

import edu.gemini.pot.sp.{ISPProgram, ISPNode}
import edu.gemini.pot.spdb.{IDBDatabaseService, DBAbstractQueryFunctor}

import java.security.Principal
import java.util.{Set => JSet}


import scalaz._, Scalaz._

/** An ODB query functor that finds all scheduleable non-sidereal
  * observations.
  */
object NonSiderealObservationFunctor {
  def query(db: IDBDatabaseService, users: JSet[Principal]): List[NonSiderealObservation] =
    new NonSiderealObservationFunctor |> (f => db.getQueryRunner(users).queryPrograms(f).results)
}

private class NonSiderealObservationFunctor extends DBAbstractQueryFunctor{
  import NonSiderealObservation.findRelevantIn

  var results: List[NonSiderealObservation] = Nil

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: JSet[Principal]): Unit =
    node match {
      case p: ISPProgram => results = findRelevantIn(p) ++ results
      case _             => // do nothing
    }
}
