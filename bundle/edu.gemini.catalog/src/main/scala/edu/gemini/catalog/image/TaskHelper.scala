package edu.gemini.catalog.image

import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import scalaz.{-\/, INil, NonEmptyList, \/, \/-}
import scalaz.concurrent.{Future, Task}

object TaskHelper {
  /**
   * From a list of task create a new Task that will return the first available results
   * or a failure if all the tasks fail
   */
  def selectFirstToComplete[A](tasks: NonEmptyList[Task[A]])(implicit pool: ExecutorService): Task[A] = {
    tasks match {
      case NonEmptyList(t, INil()) => t
      case _                       =>
        // Create a new task with a Future.Async and call the callback when appropriate
        new Task(Future.Async[Throwable \/ A](callback => {
          val interrupt = new AtomicBoolean(false)
          val failures = new AtomicInteger(tasks.size)
          val handle: (Throwable \/ A) => Unit = {
            case s @ \/-(_) if interrupt.compareAndSet(false, true) =>
              callback(s).run
            case e @ -\/(_) if failures.decrementAndGet() == 0      => // If all fail return the failure
              callback(e).run
            case _                                                  => // Other failures are ignored
          }
          // Call each task in its own thread on the pool
          tasks.map(Task.fork(_)(pool).unsafePerformAsyncInterruptibly(handle))
        }))
    }
  }

}
