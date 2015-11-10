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
  private val pids = List.newBuilder[SPProgramID]

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: java.util.Set[Principal]): Unit = {
    Option(node.getProgramID).foreach { pid =>
      pids += pid
    }
  }
}

object PidFunctor {
  def exec(odb: IDBDatabaseService, user: java.util.Set[Principal]): TryDman[List[SPProgramID]] =
    tryOp {
      val fun = new PidFunctor
      odb.getQueryRunner(user).queryPrograms(fun)
      fun.pids.result()
    }
}
