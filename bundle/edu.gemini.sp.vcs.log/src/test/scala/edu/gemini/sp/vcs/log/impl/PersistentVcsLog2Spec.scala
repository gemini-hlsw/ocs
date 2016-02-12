package edu.gemini.sp.vcs.log.impl

import edu.gemini.sp.vcs.log._
import doobie.imports._
import org.specs2.mutable.Specification
import scalaz._, Scalaz._, effect.IO
import edu.gemini.util.security.principal._
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Affiliate.CHILE
import java.io.File
import java.sql.Timestamp

object PersistentVcsLog2Spec extends Specification {
  import PersistentVcsLog2._

  val serialId = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis)

  val principals: List[GeminiPrincipal] = 
    List(
      AffiliatePrincipal(CHILE),
      ProgramPrincipal(SPProgramID.toProgramID("GS-2010A-Q-11")),
      StaffPrincipal.Gemini,
      UserPrincipal("bob@dobbs.com"),
      VisitorPrincipal(SPProgramID.toProgramID("GS-2014B-C-2"))
    )

  val pid = SPProgramID.toProgramID("GS-2010A-Q-11")

  def go[A](f: ConnectionIO[A]): A = {
    val xa = DriverManagerTransactor[IO]("org.h2.Driver", s"jdbc:h2:mem:ks${serialId.getAndIncrement};DB_CLOSE_DELAY=-1")
    f.transact(xa).ensuring(sql"SHUTDOWN IMMEDIATELY".update.run.transact(xa).attempt).unsafePerformIO
  }

  "compatibility" should {

    "query old database with indentical result" in go {

      // result as constructed by the old implementation
      val expected = {
        val pid = SPProgramID.toProgramID("GN-2015B-Q-10")
        (List(
          VcsEventSet(2061 to 2061, Map(OpFetch -> 1), (1449531008701L,1449531008701L), pid, Set(ProgramPrincipal(pid))), 
          VcsEventSet(1982 to 1988, Map(OpFetch -> 2, OpStore -> 1), (1449523267535L,1449523650543L), pid, Set(StaffPrincipal.Gemini)),
          VcsEventSet(1960 to 1965, Map(OpFetch -> 2, OpStore -> 2), (1449516911922L,1449517556842L), pid, Set(ProgramPrincipal(pid))) //,
        ), true)
      }

      for {
        _ <- sql"runscript from 'classpath:/testdb.sql' charset 'utf-8'".update.run
        _ <- checkSchema("«in memory»")
        x <- doSelectByProgram(SPProgramID.toProgramID("GN-2015B-Q-10"), 1, 3)
      } yield x must_== expected

    }

  }

  "checkSchema" should {

    "initialize with new database" in go {
      checkSchema("«in memory»") >| true
    }

    "initialize with existing database" in go {
      checkSchema("«in memory»").replicateM(2) >| true
    }
  
  }

  "log" should {

    def checkLog(op: VcsOp, principals: List[GeminiPrincipal]) =
      for {
        _  <- checkSchema("«in memory»")
        ts <- FC.delay(new Timestamp(System.currentTimeMillis))
        e  <- doLog(op, ts, pid, principals)
      } yield {
        (e.op         must_== op)         and
        (e.timestamp  must_== ts.getTime) and
        (e.pid        must_== pid)        and
        (e.principals must_== principals.toSet)
      }

    "accurately log a fetch event" in go {
      checkLog(OpFetch, principals)
    }

    "accurately log a store event" in go {
      checkLog(OpStore, principals)
    }

    "use the anonymous principal if none are given" in go {
      for {
        _  <- checkSchema("«in memory»")
        ts <- FC.delay(new Timestamp(System.currentTimeMillis))
        e  <- doLog(OpFetch, ts, pid, Nil)
      } yield {
        (e.principals must_== Anonymous.toList.toSet)
      }
    }

  }

  "log" should {

    def checkLog(op: VcsOp, principals: List[GeminiPrincipal]) =
      for {
        _  <- checkSchema("«in memory»")
        ts <- FC.delay(new Timestamp(System.currentTimeMillis))
        e  <- doLog(op, ts, pid, principals)
      } yield {
        (e.op         must_== op)         and
        (e.timestamp  must_== ts.getTime) and
        (e.pid        must_== pid)        and
        (e.principals must_== principals.toSet)
      }

    "accurately log a fetch event" in go {
      checkLog(OpFetch, principals)
    }

    "accurately log a store event" in go {
      checkLog(OpStore, principals)
    }

    "use the anonymous principal if none are given" in go {
      for {
        _  <- checkSchema("«in memory»")
        ts <- FC.delay(new Timestamp(System.currentTimeMillis))
        e  <- doLog(OpFetch, ts, pid, Nil)
      } yield {
        (e.principals must_== Anonymous.toList.toSet)
      }
    }

  }

  "selectByProgram" should {

  // /** Select `VcsEventSet`s for the specified program, from newest to oldest. Because there may be many such sets,
  //   * `offset` and `size` must be specified. This mechanism can be used to provide a "paged" user interface.
  //   * @param pid science program
  //   * @param offset offset into the result set
  //   * @param size number of events to return
  //   * @return A page of event sets and a flag (`true` if there are more pages)
  //   */

    "select all event information" in pending
 
    "time-chunk events with the same program/principals" in pending

    "handle paging correctly" in pending


  }


}



