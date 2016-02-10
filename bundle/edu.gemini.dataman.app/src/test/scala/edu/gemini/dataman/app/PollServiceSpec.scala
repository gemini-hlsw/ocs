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

import scala.concurrent.{ExecutionContext, TimeoutException, Await, Future, blocking}
import scala.concurrent.duration._

object PollServiceSpec extends Specification with ScalaCheck with Arbitraries {
  
  // These tests rely on assumptions that events will happen within a given
  // amount of time, which isn't compatible with Travis and probably not a
  // great idea anyway.  Still, the tests could be useful to run on-demand
  // when working with the PollService so I'll not delete them altogether.
  args(skipAll = true)

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
  val ShortWait = Duration(1000, MILLISECONDS)
  val LongWait  = Duration(5000, MILLISECONDS)

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
      val oids = (1 to 10).map(i => Obs(new SPObservationID(Q1.pid, i))).toList

      val log = List.newBuilder[(DmanId, String)]

      val ps = PollService("Test", 2) { id =>
        Thread.`yield`()
        log.synchronized {
          log += ((id, Thread.currentThread().getName))
        }
      }

      ps.addAll(oids)
      Thread.sleep(LongWait.toMillis)
      ps.shutdown()

      val results = log.result()
      (results.map(_._1).toSet == oids.toSet) && (results.map(_._2).toSet.size == 2)
    }
  }
}
