package edu.gemini.spModel.rich.pot.spdb

/**
 *
 */
import edu.gemini.pot.sp.{ISPProgram, ISPNode}
import edu.gemini.pot.spdb.{IDBDatabaseService, DBAbstractQueryFunctor}

import scala.collection.mutable.ListBuffer
import java.security.Principal

/**
 * Creates a Stream of science programs from the database. Restricted to this
 * package in order to ensure that it is used inside of a functor.
 */
private[spdb] class OdbProgramList extends DBAbstractQueryFunctor with Serializable {
  val lst = ListBuffer.empty[ISPProgram]

  def result: List[ISPProgram] = 
    lst.toList

  def execute(db: IDBDatabaseService, node: ISPNode, ps: java.util.Set[Principal]): Unit =
    lst.append(node.asInstanceOf[ISPProgram])

}

object OdbProgramList {

  def apply(db: IDBDatabaseService, user: java.util.Set[Principal]): List[ISPProgram] = {
    val qr     = db.getQueryRunner(user)
    val funIn  = new OdbProgramList
    val funOut = qr.queryPrograms(funIn).asInstanceOf[OdbProgramList]
    funOut.result
  }

}
