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
import java.util.TimeZone
import scala.sys.process._

object PersistentVcsLog2Spec extends Specification {
  import PersistentVcsLog2._
  import PersistentVcsMappers._

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

  // Test with a persistent db
  def go[A](db: File)(f: File => ConnectionIO[A]): A = {
    val xa = DriverManagerTransactor[IO]("org.h2.Driver", s"jdbc:h2:file:${db.getAbsolutePath}")
    f(db).transact(xa).ensuring(sql"SHUTDOWN IMMEDIATELY".update.run.transact(xa).attempt).unsafePerformIO
  }

  // Read in the test database and offset the event timestamps to what they would be if they had
  // been read with a local timezone of PST, which is the timezone where the example data was
  // written. This ensures that the hardcoded values below match what's in the db.
  val initTestData: ConnectionIO[Unit] =
    for {
      now <- FC.delay(System.currentTimeMillis)
      _   <- sql"runscript from 'classpath:/testdb.sql' charset 'utf-8'".update.run
      pst <- FC.delay(TimeZone.getTimeZone("PST").getOffset(now))
      loc <- FC.delay(TimeZone.getDefault.getOffset(now))
      _   <- sql"update event e set e.timestamp = timestampadd('MS', ${ loc - pst }, e.timestamp)".update.run
      _   <- checkSchema("«in memory»")
    } yield ()

  // A query to count some rows, for checking archive fidelity
  val count: ConnectionIO[Int] =
    sql"select count(*) from event".query[Int].unique

  // unzip a file and return output directory
  def unzip(zip: File): ConnectionIO[File] =
    for {
      d <- HC.delay(File.createTempFile("unzipped-", ".dir"))
      _ <- HC.delay(d.delete) // will re-create as a dir
      n <- HC.delay(Seq("unzip", zip.getAbsolutePath, "-d", d.getAbsolutePath).!)
    } yield if (n == 0) d else sys.error("unzip failed!")

  "checkArchive" should {
    "write a readable archive file" in go(File.createTempFile("test-db-", ".sql")) { db =>
      for {
        _  <- initTestData
        n1 <- count
        f  <- FC.delay(File.createTempFile("test-archive-", ".zip"))
        _  <- doArchive(f)
        d  <- unzip(f).map(new File(_, db.getName))
        n2 <- FC.delay(go(d)(_ => count))
      } yield n1 == 2406 && n1 == n2
    }
  }

//  "compatibility" should {
//
//    "query old database with identical result" in go {
//
//      // result as constructed by the old implementation
//      val expected = {
//        val pid = SPProgramID.toProgramID("GN-2015B-Q-10")
//        (List(
//          VcsEventSet(2061 to 2061, Map(OpFetch -> 1), (1449531008701L,1449531008701L), pid, Set(ProgramPrincipal(pid))),
//          VcsEventSet(1982 to 1988, Map(OpFetch -> 2, OpStore -> 1), (1449523267535L,1449523650543L), pid, Set(StaffPrincipal.Gemini)),
//          VcsEventSet(1960 to 1965, Map(OpFetch -> 2, OpStore -> 2), (1449516911922L,1449517556842L), pid, Set(ProgramPrincipal(pid))) //,
//        ), true)
//      }
//
//      for {
//        _ <- initTestData
//        x <- doSelectByProgram(SPProgramID.toProgramID("GN-2015B-Q-10"), 1, 3)
//      } yield x must_== expected
//
//    }
//
//  }

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

  "selectByProgram" should {
 
    val allPids: ConnectionIO[List[SPProgramID]] =
      sql"select distinct program_id from event".query[SPProgramID].list

    // all List[EventSet] in the database
    val all: ConnectionIO[List[List[VcsEventSet]]] =
      for {
        _    <- initTestData
        pids <- allPids
        ess  <- pids.traverse(doSelectByProgram(_, 0, 1000))
      } yield ess.map(_._1)

    "time-chunk events (1)" in go {
      all.map(_.forall { (es: List[VcsEventSet]) =>
        es.forall { e =>
          // no EventSet should be longer than total events * TimeSlice
          val events   = e.ops.values.sum
          val duration = e.timestamps._2 - e.timestamps._1
          duration < events * TimeSlice
        }
      })
    }

    "time-chunk events (2)" in go {
      all.map(_.forall { (es: List[VcsEventSet]) =>
        // adjacent eventsets should differ in principals or should be >= TimeSlice apart
        es.zip(es.tail)
          .filter { case (e0, e1) => (e0.principals == e1.principals) } 
          .forall { case (e0, e1) => (e0.timestamps._2 - e1.timestamps._2) > TimeSlice }
      })
    }

    "handle paging correctly" in go {

      // drop(1).drop(1).take(1) == drop(2).take(1)
      def checkPages(pid: SPProgramID): ConnectionIO[Boolean] =
        for {
          p2a <- doSelectByProgram(pid, 1, 2).map(_._1.drop(1))
          p2b <- doSelectByProgram(pid, 2, 1).map(_._1)
        } yield p2a == p2b

      for {
        _    <- initTestData
        pids <- allPids
        rs   <- pids.traverse(checkPages)
      } yield rs.forall(_ == true)
    
    }

  }


}



