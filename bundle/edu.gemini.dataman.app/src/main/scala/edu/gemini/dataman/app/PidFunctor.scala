package edu.gemini.dataman.app

import edu.gemini.dataman.core._
import edu.gemini.pot.sp.{ISPProgram, ISPNode}
import edu.gemini.pot.spdb.{IDBDatabaseService, DBAbstractQueryFunctor}
import edu.gemini.spModel.core.SPProgramID

import java.security.Principal

/** Database Query Functor for extracting the list of all program IDs in the
  * ODB.
  */
final class PidFunctor extends DBAbstractQueryFunctor {
  private var pids = List.empty[SPProgramID]

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: java.util.Set[Principal]): Unit = {
    val pidOpt = node match {
      case p: ISPProgram => Option(p.getProgramID)
      case _             => None
    }
    pidOpt.foreach { pid =>
      pids = pid :: pids
    }
  }
}

object PidFunctor {
  def exec(odb: IDBDatabaseService, user: java.util.Set[Principal]): TryDman[List[SPProgramID]] =
    tryOp {
      val fun = new PidFunctor
      odb.getQueryRunner(user).queryPrograms(fun)
      fun.pids
    }
}
