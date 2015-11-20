package edu.gemini.dataman.app

import edu.gemini.dataman.core.DmanId
import edu.gemini.dataman.core.DmanId.{Prog, Obs, Dset}

import java.util.logging.{Level, Logger}

import scala.collection.mutable
import scalaz._
import Scalaz._


/** PollService allows clients to request poll updates associated with given
  * dataset, observation, or program ids.  The requests will be executed at a
  * future time according to workload and priority.  A new request is ignored
  * if there is already an identical existing request that is either pending
  * or currently active.  Dataset requests have priority over observation
  * requests, which in turn have priority over program requests.  Requests of
  * the same kind of id are executed in FIFO order.
  */
sealed trait PollService {

  /** Requests a poll update for the given dataset, observation, or program.
    * If there is already a pending request or active request for the same id,
    * nothing is added.
    *
    * @return `true` if a new request is added, `false` otherwise
    */
  def add(id: DmanId): Boolean

  /** Requests a poll update for the given datasets, observations, or programs
    *
    * If there is already a pending request or active request for the same id,
    * nothing is added.
    *
    * @return `true` if a new request is added, `false` otherwise
    */
  def addAll(id: List[DmanId]): Boolean

  /** Stops the poll service and cleans up resources. */
  def shutdown(): Unit
}

object PollService {

  /** A mutable blocking queue of poll requests.  The `add` method prevents
    * duplicate requests from being included in the queue.  The `doNext` method
    * is blocking.
    */
  trait RequestQueue {
    def isActive(id: DmanId): Boolean
    def isPending(id: DmanId): Boolean
    def add(id: DmanId): Boolean
    def addAll(ids: List[DmanId]): Boolean
    def doNext[A](f: DmanId => A): A
    def clearPending(): Unit
    def pending: List[DmanId]
    def active: Set[DmanId]
  }

  object RequestQueue {
    def empty(): RequestQueue = new RequestQueue {

      val dsetQueue     = mutable.LinkedHashSet.empty[DmanId]
      val obsQueue      = mutable.LinkedHashSet.empty[DmanId]
      val progQueue     = mutable.LinkedHashSet.empty[DmanId]

      val pendingQueues = List(dsetQueue, obsQueue, progQueue)
      val activeSet     = mutable.HashSet.empty[DmanId]

      override def pending: List[DmanId] = synchronized {
        pendingQueues.flatMap(_.toList)
      }

      override def active: Set[DmanId] = synchronized {
        activeSet.toSet
      }

      override def isActive(id: DmanId): Boolean = synchronized {
        activeSet(id)
      }

      override def isPending(id: DmanId): Boolean = synchronized {
        pendingQueues.exists(_.apply(id))
      }

      override def clearPending(): Unit = synchronized {
        pendingQueues.foreach(_.clear())
      }

      override def add(id: DmanId): Boolean = synchronized {
        val added = !activeSet(id) && (id match {
          case _: Dset => dsetQueue.add(id)
          case _: Obs  => obsQueue.add(id)
          case _: Prog => progQueue.add(id)
        })
        if (added) notifyAll()
        added
      }

      override def addAll(ids: List[DmanId]): Boolean = synchronized {
        ids.map(add).foldMap(Tags.Disjunction)
      }

      def next: Option[mutable.LinkedHashSet[DmanId]] = synchronized {
        pendingQueues.find(_.nonEmpty)
      }

      override def doNext[A](body: DmanId => A): A = {
        def blockingTake(): DmanId = synchronized {
          while (next.isEmpty) wait()

          val queue = next.get
          queue.head <| queue.remove <| activeSet.add
        }

        val id = blockingTake()

        try {
          body(id)
        } finally {
          synchronized {
            activeSet.remove(id)
          }
        }
      }
    }
  }

  private val Log = Logger.getLogger(getClass.getName)

  def apply(name: String, workerCount: Int)(poll: DmanId => Unit): PollService =
    new PollService {
      val queue = RequestQueue.empty()

      class PollRunnable(name: String) extends Runnable {
        override def run(): Unit = {
          import scala.util.control.Breaks.{break, breakable}

          breakable {
            while (true) {
              try {
                Log.fine(s"$name waiting for job")
                queue.doNext { id =>
                  Log.info(s"$name polling $id")
                  poll(id)
                }
              } catch {
                case _: InterruptedException =>
                  Log.log(Level.INFO, s"$name stopped")
                  break()
                case t: Throwable =>
                  Log.log(Level.SEVERE, s"$name exception in poll task", t)
              }
            }
          }
        }
      }

      Log.info(s"Dataman startup PollService.")

      val workers = (0 until (workerCount max 1)).toList.map { n =>
        val threadName = s"$name PollService Worker $n"
        new Thread(new PollRunnable(threadName), threadName) {
          setPriority(Thread.NORM_PRIORITY - 1)
          setDaemon(true)
        }
      }

      workers.foreach(_.start())

      override def add(id: DmanId): Boolean           = queue.add(id)
      override def addAll(ids: List[DmanId]): Boolean = queue.addAll(ids)

      override def shutdown(): Unit = {
        Log.info(s"Dataman shutdown PollService.")
        queue.clearPending()
        workers.foreach(_.interrupt())
      }
    }
}