package edu.gemini.spModel.rich.pot.spdb

import edu.gemini.pot.sp.{ISPNode, ISPProgram}
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}
import java.security.Principal

/**
 *
 */
class RichOdb(odb: IDBDatabaseService) {

  def query[T](user: java.util.Set[Principal])(f: List[ISPProgram] => T): Either[OdbError, T] = {
    val funIn = new OdbQuery(f)
      val funOut = odb.getQueryRunner(user).execute(funIn, null)
      Option(funOut.getException).toLeft(funOut.res.get).left map { ex =>
        new OdbError.RemoteFailure(ex)
      }
  }

  def apply[T](user: java.util.Set[Principal])(f: IDBDatabaseService => T): Either[OdbError, T] = {
    val funIn = new OdbFunctor(f)
    val funOut = odb.getQueryRunner(user).execute(funIn, null)
    funOut.result.left map { ex => new OdbError.RemoteFailure(ex) }
  }

  def allPrograms(user: java.util.Set[Principal]): Vector[ISPProgram] = {
    var all = Vector.empty[ISPProgram]
    odb.getQueryRunner(user).queryPrograms(new DBAbstractQueryFunctor {
      def execute(db: IDBDatabaseService, node: ISPNode, ps: java.util.Set[Principal]) {
        all = all :+ node.asInstanceOf[ISPProgram]
      }
    })
    all
  }

}