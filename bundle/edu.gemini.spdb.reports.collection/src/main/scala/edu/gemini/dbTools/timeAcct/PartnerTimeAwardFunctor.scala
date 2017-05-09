package edu.gemini.dbTools.timeAcct

import edu.gemini.pot.sp.{ISPNode, ISPProgram}
import edu.gemini.pot.spdb.{IDBDatabaseService, DBAbstractQueryFunctor}
import edu.gemini.spModel.core.{ProgramId, Semester}
import edu.gemini.spModel.obs.PartnerTimeAwardUtil
import edu.gemini.spModel.rich.core._

import java.security.Principal
import java.util.{Set => JSet}


/** A functor that will set the partner time award to the amount of executed
  * partner time for all pre-2017B programs.  Once there are no longer active
  * pre-2017B programs, this code can be removed.
  */
object PartnerTimeAwardFunctor {
  def query(db: IDBDatabaseService, users: JSet[Principal]): Unit =
    db.getQueryRunner(users).queryPrograms(new PartnerTimeAwardFunctor)

  val sem2017B = new Semester(2017, Semester.Half.B)

  /** Determines whether the given program definitely belongs to a pre-2017B
    * semester based on its program id.  If not known, we return false.
    */
  def isPre2017B(p: ISPProgram): Boolean =
    Option(p.getProgramID).flatMap(pid => ProgramId.parse(pid.stringValue).semester).exists(_ < sem2017B)
}

private class PartnerTimeAwardFunctor extends DBAbstractQueryFunctor {

  import PartnerTimeAwardFunctor._

  override def execute(db: IDBDatabaseService, node: ISPNode, principals: JSet[Principal]): Unit =
    node match {
      case p: ISPProgram if isPre2017B(p) => PartnerTimeAwardUtil.setPartnerAwardToExecuted(p)
      case _                              => // do nothing
    }
}
