package edu.gemini.catalog.image

import java.util.concurrent.{ExecutorService, Executors, ThreadFactory, TimeUnit}

import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scalaz._
import Scalaz._
import scalaz.NonEmptyList._
import scalaz.concurrent.Task

class TaskHelperSpec extends FlatSpec with Matchers with ScalaCheckPropertyChecks with ImageCatalogArbitraries {
  private val DaemonThreadFactory = new ThreadFactory {
    private val defaultThreadFactory = Executors.defaultThreadFactory()

    override def newThread(r: Runnable): Thread = {
      defaultThreadFactory.newThread(r) <| {_.setDaemon(true)}
    }
  }

  /**
    * Execution context with many daemon threads
    */
  val daemonEC: ExecutorService = Executors.newFixedThreadPool(50, DaemonThreadFactory)

  "TaskHelper" should
    "run multiple tasks in parallel and select only one" in {
      forAll { (i: Int) =>
        val tasks = NonEmptyList(Task.delay(i))
        TaskHelper.selectFirstToComplete(tasks)(daemonEC).unsafePerformSyncAttempt should matchPattern {
          case \/-(k) if k === i =>
        }
      }
    }
    it should "select one of the values on the list" in {
      forAll { (i: List[Int]) =>
        whenever(i.nonEmpty) {
          val l = i.map(Task.delay(_)).toIList
          val tasks = l.toNel.get // we are non empty
          TaskHelper.selectFirstToComplete(tasks)(daemonEC).unsafePerformSyncAttempt should matchPattern {
            case \/-(k) if i.contains(k) =>
          }
        }
      }
    }
    it should "select one even if other fails" in {
      forAll { (i: List[Int]) =>
        val l = i.map(Task.delay(_)).toIList
        val tasks = nel(Task.fail(new RuntimeException()), l)
        TaskHelper.selectFirstToComplete(tasks)(daemonEC).unsafePerformSyncAttempt should matchPattern {
          case \/-(k) if i.contains(k)               =>
          case -\/(_: RuntimeException) if i.isEmpty =>
        }
      }
    }
    it should "select one even if other fails II" in {
      forAll { (i: Int) =>
        val tasks = nels(Task.delay(i), Task.fail(new RuntimeException()))
        TaskHelper.selectFirstToComplete(tasks)(daemonEC).unsafePerformSyncAttempt should matchPattern {
          case \/-(k) if k === i =>
        }
      }
    }
    it should "take failure if it is the only available" in {
      forAll { (i: List[Int]) =>
        whenever(i.nonEmpty) {
          val l = i.map(_ => Task.fail(new RuntimeException())).toIList
          val tasks = nel(Task.fail(new RuntimeException()), l)
          TaskHelper.selectFirstToComplete(tasks)(daemonEC).unsafePerformSyncAttempt should matchPattern {
            case -\/(_: RuntimeException) =>
          }
        }
      }
    }
    it should "take the first to complete" in {
      forAll { (a: Int, b: Int) =>
        val slowTask = Task.delay {
          TimeUnit.MILLISECONDS.sleep(20000)
          a
        }
        val tasks = nels(slowTask, Task.delay(b))
        TaskHelper.selectFirstToComplete(tasks)(daemonEC).unsafePerformSyncAttempt should matchPattern {
          case \/-(k) if k === b =>
        }
      }
    }
}
