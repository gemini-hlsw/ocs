package edu.gemini.dbTools.maskcheck

import edu.gemini.pot.sp.{ISPProgram, ISPNode}
import edu.gemini.pot.spdb.{IDBDatabaseService, DBAbstractQueryFunctor}
import edu.gemini.spModel.core.{ ProgramId, ProgramType, SPProgramID }
import edu.gemini.spModel.gemini.obscomp.SPProgram

import java.security.Principal
import java.util.{Set => JSet}

import scala.collection.mutable.Buffer

import scalaz._
import Scalaz._
import scalaz.effect.IO


/**
 * An ODB query functor that finds all active science programs.
 */
object ActiveScienceProgramFunctor {

  private def isScience(p: ISPProgram): Boolean = {
    val programType: Option[ProgramType] =
      for {
        i <- Option(p.getProgramID).map(pid => ProgramId.parse(pid.stringValue))
        t <- i.ptype
      } yield t

    programType.exists(_.isScience)
  }

  private def isActive(p: ISPProgram): Boolean = {
    val obj = p.getDataObject.asInstanceOf[SPProgram]
    obj.isActive && !obj.isCompleted
  }

  def unsafeQuery(db: IDBDatabaseService, user: JSet[Principal]): List[SPProgramID] =
    new ActiveScienceProgramFunctor |>
            (f => db.getQueryRunner(user).queryPrograms(f).results.toList)

  def query(db: IDBDatabaseService, user: JSet[Principal]): Action[List[SPProgramID]] =
    Action.catchLeft(unsafeQuery(db, user))
}

private class ActiveScienceProgramFunctor extends DBAbstractQueryFunctor {
  import ActiveScienceProgramFunctor.{ isActive, isScience }

  val results: Buffer[SPProgramID] = Buffer.empty

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: JSet[Principal]): Unit = {
    node match {
      case p: ISPProgram => if (isActive(p) && isScience(p)) results += p.getProgramID
      case _             => // do nothing
    }
  }
}
