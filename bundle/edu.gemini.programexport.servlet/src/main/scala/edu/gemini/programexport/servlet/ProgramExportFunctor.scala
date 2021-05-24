package edu.gemini.programexport.servlet

import edu.gemini.pot.sp.{ISPNode, ISPProgram}
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}

import java.security.Principal
import java.util.{Set => JSet}

class ProgramExportFunctor(programName: String) extends DBAbstractQueryFunctor {
  // Called once per program by IDBQueryRunner implementation.
  override def execute(db: IDBDatabaseService, node: ISPNode, principals: JSet[Principal]): Unit = {
    val prog = node.asInstanceOf[ISPProgram]
  }
}
