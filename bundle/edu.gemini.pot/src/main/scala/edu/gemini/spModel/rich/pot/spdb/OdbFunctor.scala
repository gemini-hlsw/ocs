package edu.gemini.spModel.rich.pot.spdb

import edu.gemini.pot.spdb.{DBAbstractFunctor, IDBDatabaseService}
import edu.gemini.pot.sp.ISPNode
import java.security.Principal


/** An ODB functor for making an update to the database. */
private[spdb] class OdbFunctor[T](f: IDBDatabaseService => T) extends DBAbstractFunctor with Serializable {
  private var res: Either[Throwable, T] = null

  def result: Either[Throwable, T] = 
    res

  def execute(db: IDBDatabaseService, node: ISPNode, ps: java.util.Set[Principal]): Unit = 
    res = try Right(f(db)) catch { case ex: Throwable => Left(ex) }

}

object OdbFunctor {

  def apply[T](db:IDBDatabaseService, f: (IDBDatabaseService => T), user: java.util.Set[Principal]): Either[OdbError, T] = {
    val funIn = new OdbFunctor(f)
    val funOut = db.getQueryRunner(user).execute(funIn, null)
    funOut.result.left map { ex => new OdbError.RemoteFailure(ex) }
  }

}

