package edu.gemini.dataman.app

import edu.gemini.dataman.core.DmanId.{Obs, Dset, Prog}
import edu.gemini.dataman.core.DmanId
import edu.gemini.gsa.query.Arbitraries
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.dataset.DatasetLabel
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import java.time.Instant

import scala.concurrent.{ExecutionContext, TimeoutException, Await, Future, blocking}
import scala.concurrent.duration._

object PollServiceSpec extends Specification with ScalaCheck with Arbitraries {

  val genPid: Gen[SPProgramID] =
    Gen.chooseNum(1, 5).map(n => SPProgramID.toProgramID(s"GS-2016A-Q-$n"))

  val genOid: Gen[SPObservationID] =
    for {
      pid <- genPid
      num <- Gen.chooseNum(1, 5)
    } yield new SPObservationID(pid, num)

  val genLabel: Gen[DatasetLabel] =
    for {
      oid <- genOid
      num <- Gen.chooseNum(1, 5)
    } yield new DatasetLabel(oid, num)

  val genDmanId: Gen[DmanId] =
    Gen.oneOf(genPid.map(Prog), genOid.map(Obs), genLabel.map(Dset))

  implicit val arbDmanId: Arbitrary[DmanId] = Arbitrary { genDmanId }

  def partition(ids: List[DmanId]): (List[DmanId], List[DmanId], List[DmanId]) = {
    val emptyIds = List.empty[DmanId]
    (ids:\(emptyIds, emptyIds, emptyIds)) { case (id,(d,o,p)) =>
        id match {
          case _: Dset => (id :: d, o, p)
          case _: Obs  => (d, id :: o, p)
          case _: Prog => (d, o, id :: p)
        }
    }
  }

  val Q1        = Prog(SPProgramID.toProgramID("GS-2016A-Q-1"))
  val ShortWait = Duration(  1000, MILLISECONDS)
  val LongWait  = Duration(120000, MILLISECONDS)

  "RequestQueue" should {
    "respect id priority" in {
      forAll { (ids: List[DmanId]) =>
        val queue = PollService.RequestQueue.empty()
        queue.addAll(ids)

        val pending = queue.pending
        val (d,o,p) = partition(pending)

        d ++ o ++ p == pending
      }
    }

    "respect insertion order priority" in {
      forAll { (ids: List[DmanId]) =>
        val queue = PollService.RequestQueue.empty()
        queue.addAll(ids)

        val pending  = queue.pending
        val (d,o,p)  = partition(pending)
        val ordering = ids.distinct.zipWithIndex.toMap

        List(d, o, p).forall { l => l.sortBy(ordering) == l }
      }
    }

    "remove duplicates" in {
      forAll { (ids: List[DmanId]) =>
        val queue = PollService.RequestQueue.empty()
        queue.addAll(ids)

        val pending = queue.pending
        (pending.distinct == pending) && (ids.toSet &~ pending.toSet).isEmpty
      }
    }

    "not block in doNext when something available" in {
      val queue = PollService.RequestQueue.empty()
      queue.add(Q1)

      val fut = Future {
        blocking { queue.doNext(identity) }
      }(ExecutionContext.Implicits.global)

      Await.result(fut, LongWait) == Q1
    }

    "block in doNext when nothing available" in {
      val queue = PollService.RequestQueue.empty()

      val fut = Future {
        blocking { queue.doNext(identity) }
      }(ExecutionContext.Implicits.global)

      try {
        Await.result(fut, ShortWait)
        false
      } catch {
        case _: TimeoutException =>
          queue.add(Q1)
          Await.result(fut, LongWait) == Q1
      }
    }

    "not allow a new request to be added if already active" in {
      val queue = PollService.RequestQueue.empty()
      queue.add(Q1)

      val fut = Future {
        blocking {
          queue.doNext { id =>
            !queue.isPending(id) && queue.isActive(id) && !queue.add(id)
          }
        }
      }(ExecutionContext.Implicits.global)

      Await.result(fut, LongWait) && !queue.isPending(Q1) && !queue.isActive(Q1)
    }
  }

  "PollService" should {
    "execute tasks in parallel" in {
      val threadCount = 2
      val oids        = (1 to 20).map(i => Obs(new SPObservationID(Q1.pid, i))).toList
      val log         = List.newBuilder[(DmanId, String)]

      val ps = PollService("Test", threadCount, Thread.NORM_PRIORITY - 1) { id =>
        // We want to simulate work and give the other poll service thread an
        // opportunity to run.  This is probably a bit sketchy since there is
        // no guarantee that the other thread will actually run.
        Thread.sleep(100)
        Thread.`yield`()
        log.synchronized {
          log += ((id, Thread.currentThread().getName))
        }
      }

      ps.addAll(oids)

      // Poll, waiting for completion.
      val end = Instant.now().plusMillis(LongWait.toMillis)
      while (Instant.now().isBefore(end) && ps.nonEmpty) {
        Thread.sleep(ShortWait.toMillis)
      }

      val finishedWorking = ps.isEmpty

      ps.shutdown()

      val (loggedOids, loggedThreads) = log.result().unzip
      finishedWorking &&                          // all the work eventually finished (i.e., didn't time out)
        (loggedOids.toSet == oids.toSet) &&       // every obs id was processed
        (loggedThreads.toSet.size == threadCount) // all threads participated in the work
    }
  }
}
