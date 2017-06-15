package edu.gemini.dbTools.tigratable

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.sp.vcs.log.VcsEventSet
import edu.gemini.sp.vcs.log.VcsLog
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.rich.core._
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.util.security.principal.{ProgramPrincipal, UserPrincipal, GeminiPrincipal}
import java.util.Date

import scalaz._
import Scalaz._

/**
 * Extracts the last sync time for a PI, if any.
 */
final class SyncTimestamp(vcs: VcsLog) {
  // timestampMap is a map of maps.  The key is the program id and the value is
  // a Map[GeminiPrincipal, Long].
  val timestampMap = vcs.selectLastSyncTimestamps()

  // The subset of program principals that are PIs.
  private def piPrincipals(p: ISPProgram): Set[GeminiPrincipal] = {
    def splitEmails(s:String): Set[String] =
      Option(s).map(_.split("""[^\w@.\-]+""").toSet).getOrElse(Set.empty)

    def userPrincipalsForEmails(s:String): Set[UserPrincipal] =
      splitEmails(s).map(UserPrincipal(_))

    Option(p.getProgramID).map(ProgramPrincipal).toSet ++
      userPrincipalsForEmails(p.getDataObject.asInstanceOf[SPProgram].getPIInfo.getEmail)
  }

  // Time of the last PI principal VCS op.
  private def lastPiSyncTime(p: ISPProgram, m: Map[GeminiPrincipal, Long]): Option[Long] = {
    val isPi       = piPrincipals(p)
    val timeStamps = m.filterKeys(isPi).values
    timeStamps.nonEmpty option timeStamps.max
  }

  def lookupDateOrNull(p: ISPProgram): Date =
    (for {
      pid <- Option(p.getProgramID)
      m   <- timestampMap.lookup(pid)
      l   <- lastPiSyncTime(p, m)
    } yield new Date(l)).orNull
}
