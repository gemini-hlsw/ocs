package edu.gemini.spModel.rich.pot.spdb

import edu.gemini.pot.spdb.{IDBDatabaseService, DBAbstractFunctor}
import edu.gemini.pot.sp.{ISPProgram, ISPNode}
import java.security.Principal

/**
 * Wraps a normal function from a list of science programs to a generic type
 * with all the OdbFunctor dressing necessary for executing the function on
 * the Observing Database.
 */
private[spdb] class OdbQuery[T](xform: List[ISPProgram] => T) extends DBAbstractFunctor with Serializable {

  var res: Option[T] = None

  def execute(db: IDBDatabaseService, node: ISPNode, ps: java.util.Set[Principal]): Unit = {
    val progList = OdbProgramList(db, ps)
    res = Some(xform(progList))
  }

}

private[spdb] object OdbQuery {

  def query[T](db: IDBDatabaseService, f: List[ISPProgram] => T, user: java.util.Set[Principal]): Either[OdbError, T] = {
    val funIn = new OdbQuery(f)
    val funOut = db.getQueryRunner(user).execute(funIn, null)
    Option(funOut.getException).toLeft(funOut.res.get).left map {
      ex => new OdbError.RemoteFailure(ex)
    }
  }

}