package edu.gemini.sp.vcs.log

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.util.security.principal.GeminiPrincipal
import javax.security.auth.Subject
import collection.JavaConverters._
import java.io.File

trait VcsLog {

  /** Log an event to the database.
    * @param op the kind of operation
    * @param pid science program id
    * @param principals set of principals assocated with this event
    * @return the logged `LogEvent`
    */
  def log(op: VcsOp, pid: SPProgramID, principals: Set[GeminiPrincipal]): VcsEvent

  /** Log an event to the database.
    * @param op the kind of operation
    * @param pid science program id
    * @param subject the `Subject` assocated with this event
    * @return the logged `LogEvent`
    */
  def log(op: VcsOp, pid: SPProgramID, subject: Subject): VcsEvent =
    log(op, pid, geminiPrincipals(subject))

  /** Select `VcsEventSet`s for the specified program, from newest to oldest. Because there may be many such sets,
    * `offset` and `size` must be specified. This mechanism can be used to provide a "paged" user interface.
    * @param pid science program
    * @param offset offset into the result set
    * @param size number of events to return
    * @return A page of event sets and a flag (`true` if there are more pages)
    */
  def selectByProgram(pid: SPProgramID, offset: Int, size: Int): (List[VcsEventSet], Boolean)

  /** Archive the log database to the specified file. */
  def archive(f: File): Unit

  private def geminiPrincipals(s: Subject): Set[GeminiPrincipal] =
    s.getPrincipals.asScala.collect {
      case p: GeminiPrincipal => p
    }.toSet

}

/**
 * This interface is provided for the log migrator for 2014A, which does not rely on the system clock. It can be
 * removed in a future release.
 */
trait VcsLogEx extends VcsLog {

  def log(op: VcsOp, time:Long, pid: SPProgramID, principals: Set[GeminiPrincipal]): VcsEvent

}