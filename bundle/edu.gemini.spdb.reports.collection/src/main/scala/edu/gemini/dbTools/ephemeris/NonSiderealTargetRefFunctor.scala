package edu.gemini.dbTools.ephemeris

import edu.gemini.pot.sp.{ISPProgram, ISPNode}
import edu.gemini.pot.spdb.{IDBDatabaseService, DBAbstractQueryFunctor}

import java.security.Principal
import java.util.logging.{Level, Logger}
import java.util.{Set => JSet}

import scalaz._, Scalaz._

/** An ODB query functor that finds all scheduleable non-sidereal
  * observations.
  */
object NonSiderealTargetRefFunctor {
  private final val Log = Logger.getLogger(NonSiderealTargetRefFunctor.getClass.getName)

  def query(db: IDBDatabaseService, users: JSet[Principal]): List[NonSiderealTargetRef] =
    new NonSiderealTargetRefFunctor |> (f => db.getQueryRunner(users).queryPrograms(f).results)
}

private class NonSiderealTargetRefFunctor extends DBAbstractQueryFunctor{
  import NonSiderealTargetRefFunctor.Log
  import NonSiderealTargetRef.findRelevantIn

  var results: List[NonSiderealTargetRef] = Nil

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: JSet[Principal]): Unit = {

    // Get the non-sidereal target references for program p, but don't let any
    // non-fatal exceptions kill the functor for any remaining programs.
    def nonSidRefsFor(p: ISPProgram): List[NonSiderealTargetRef] =
       \/.fromTryCatchNonFatal(findRelevantIn(p)) match {
         case -\/(t)   =>
           Log.log(Level.WARNING, s"Couldn't get non-sidereal targets in program ${Option(p.getProgramID).getOrElse(p.getProgramKey)}", t)
           Nil

         case \/-(lst) =>
           lst
       }

    node match {
      case p: ISPProgram => results = nonSidRefsFor(p) ++ results
      case _             => // do nothing
    }
  }
}
