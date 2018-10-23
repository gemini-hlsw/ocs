package edu.gemini.dbTools.timingwindowcheck

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.pot.spdb.DBLocalDatabase
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.skycalc.Interval
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow

import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import java.security.Principal
import scala.collection.JavaConverters._

object TimingWindowFunctorSpec extends Specification with ScalaCheck with Arbitraries {
  import Setup._

  "TimingWindowFunctor" should {

    "find nothing if there are no timing windows" !
      forAll { (ps: List[Prog]) =>
        Env(ps, Nil).testEmpty(0L, Long.MaxValue)
      }

    "find nothing if the timing window expired before this period" !
      forAll { (ps: List[Prog]) =>
        Env(ps, List(new TimingWindow(0L, 1L, 0, 0))).testEmpty(2L, Long.MaxValue)
      }

    "find nothing if the timing window hasn't yet expired" !
      forAll { (ps: List[Prog]) =>
        Env(ps, List(new TimingWindow(3L, 1L, 0, 0))).testEmpty(0L, 3L)
      }

    "find nothing if the timing window doesn't expire" !
      forAll { (ps: List[Prog]) =>
        Env(ps, List(new TimingWindow(0L, 1L, TimingWindow.REPEAT_FOREVER, 10))).testEmpty(0L, Long.MaxValue)
      }

    "find nothing if there is at least one timing window that doesn't expire" ! {
      val tws = List(
        new TimingWindow(0L, 1L, TimingWindow.REPEAT_FOREVER, 10),
        new TimingWindow(2L, 2L, TimingWindow.REPEAT_NEVER,    0)
      )

      forAll { (ps: List[Prog]) => Env(ps, tws).testEmpty(0L, Long.MaxValue) }
    }

    "find nothing even if there is an expiring window but there are other windows later" ! {
      val tws = List(
        new TimingWindow( 1L,  1L, 0, 0),
        new TimingWindow(10L, 10L, 0, 0)
      )

      forAll { (ps: List[Prog]) => Env(ps, tws).testEmpty(0L, 3L) }
    }

    "find valid obs if they have an expiring timing window" !
      forAll { (ps: List[Prog]) =>
        Env(ps, List(new TimingWindow(1L, 1L, 0, 0))).testValid(0L, 3L)
      }

  }


  val noUser: java.util.HashSet[Principal] =
    new java.util.HashSet

  case class Env(
    odb:   IDBDatabaseService,
    valid: Set[SPObservationID]
  ) {

    def test(s: Long, e: Long, expected: Set[SPObservationID]): MatchResult[Any] =
      try {
        val interval = new Interval(s, e)
        val actual   = TimingWindowFunctor.unsafeQuery(interval, odb, noUser).toSet
        actual shouldEqual expected
      } finally {
        odb.getDBAdmin.shutdown()
      }

    def testEmpty(s: Long, e: Long): MatchResult[Any] =
      test(s, e, Set.empty)

    def testValid(s: Long, e: Long): MatchResult[Any] =
      test(s, e, valid)
  }

  object Env {
    def apply(ps: List[Prog], tws: List[TimingWindow]): Env = {
      val odb = DBLocalDatabase.createTransient

      // Remove duplicate pids
      val ps2 = ps.groupBy(_.pid).mapValues(_.head).toList.unzip._2

      // Create program nodes
      val pns = ps2.map(_.create(odb.getFactory, tws))

      // Add them to the database
      pns.foreach(odb.put)

      // Figure out which observations are expected, ignoring timing windows
      val valid = pns.zip(ps2).foldLeft(Set.empty[SPObservationID]) { case (s, (n, p)) =>
        if (p.valid)
          s ++ n.getAllObservations.asScala
                .map(_.getObservationID)
                .zip(p.obs)
                .collect { case (i, o) if o.valid => i }
        else
          s
      }

      Env(odb, valid)
    }
  }


}
