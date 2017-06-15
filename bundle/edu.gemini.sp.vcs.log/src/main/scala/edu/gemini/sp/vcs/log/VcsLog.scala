package edu.gemini.sp.vcs.log

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.util.security.principal.GeminiPrincipal
import javax.security.auth.Subject
import collection.JavaConverters._
import java.io.File

import scalaz.==>>

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

  def selectLastSyncTimestamps(): SPProgramID ==>> Map[GeminiPrincipal, Long]
}

object VcsLog {
  import scalaz.effect.IO

  def apply(dir: File): IO[VcsLog] = {
    import impl.PersistentVcsLog2._
    import doobie.imports._
    import java.sql.Timestamp

    for {
      p <- IO(dir.getAbsolutePath) // can throw
      _ <- IO(require(dir.mkdirs() || dir.isDirectory, s"Not a valid directory: $p"))
      xa = DriverManagerTransactor[IO]("org.h2.Driver", s"jdbc:h2:$p;DB_CLOSE_ON_EXIT=FALSE;TRACE_LEVEL_FILE=4", "", "")
      x <- checkSchema(p).transact(xa)
    } yield new VcsLog {

      def archive(f: File): Unit =
        doArchive(f).transact(xa).unsafePerformIO

      def log(op: VcsOp, pid: SPProgramID, principals: Set[GeminiPrincipal]): VcsEvent =
        doLog(op, new Timestamp(System.currentTimeMillis), pid, principals.toList).transact(xa).unsafePerformIO

      def selectByProgram(pid: SPProgramID, offset: Int, size: Int): (List[VcsEventSet], Boolean) =
        doSelectByProgram(pid, offset, size).transact(xa).unsafePerformIO

      override def selectLastSyncTimestamps(): SPProgramID ==>> Map[GeminiPrincipal, Long] =
        doSelectLastSyncTimestamps().transact(xa).unsafePerformIO

    }
  }

}
