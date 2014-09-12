package edu.gemini.dbTools.tigratable

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.sp.vcs.VcsServer
import edu.gemini.sp.vcs.log.VcsEventSet
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.util.security.principal.{ProgramPrincipal, UserPrincipal, GeminiPrincipal}
import java.util.Date

/**
 * Extracts the last sync time for a PI, if any.
 */
object SyncTimestamp {
  private val PageSize = 50

  private def piPrincipals(p: ISPProgram): Set[GeminiPrincipal] = {
    def splitEmails(s:String): Set[String] =
      Option(s).map(_.split("""[^\w@.\-]+""").toSet).getOrElse(Set.empty)

    def userPrincipalsForEmails(s:String): Set[UserPrincipal] =
      splitEmails(s).map(UserPrincipal(_))

    Option(p.getProgramID).map(ProgramPrincipal).toSet ++
      userPrincipalsForEmails(p.getDataObject.asInstanceOf[SPProgram].getPIInfo.getEmail)
  }

  def lookup(p: ISPProgram, vcs: VcsServer): Option[Long] = {
    val pis = piPrincipals(p)

    def searchLog(pid: SPProgramID): Option[Long] = {
      type EventSetPage = List[VcsEventSet]

      def pageStream(start: Int): Stream[EventSetPage] =
        vcs.log(pid, start, PageSize).toOption.fold(Stream.empty[EventSetPage]) { case (lst, more) =>
          if (more) lst #:: pageStream(start + PageSize) else lst #:: Stream.empty[EventSetPage]
        }

      def matches(es: VcsEventSet): Boolean =
        !es.principals.intersect(pis).isEmpty && es.ops.values.exists(_ > 0)

      def lookup(s: Stream[EventSetPage]): Option[Long] =
        s.headOption.fold(Option.empty[Long]) {
          _.find(matches).fold(lookup(s.tail))(es => Some(es.timestamps._2))
        }

      lookup(pageStream(0))
    }

    for {
      pid       <- Option(p.getProgramID)
      timestamp <- searchLog(pid)
    } yield timestamp
  }


  def lookupDateOrNull(p: ISPProgram, vcs: VcsServer): Date =
    lookup(p, vcs).map(l => new Date(l)).orNull
}
