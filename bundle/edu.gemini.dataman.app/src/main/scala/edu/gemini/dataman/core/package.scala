package edu.gemini.dataman

import edu.gemini.dataman.core.DmanFailure.DmanException
import edu.gemini.spModel.core.catchingNonFatal
import edu.gemini.spModel.dataset.{DatasetExecRecord, DatasetQaRecord}

import scalaz.concurrent._

import scalaz._
import Scalaz._

package object core {

  /** When an action is applied, it may update one or more QA and/or exec
    * records.
    */
  type DatasetUpdates = (List[DatasetQaRecord], List[DatasetExecRecord])

  type TryDman[A] = DmanFailure \/ A

  object TryDman {
    def apply[A](a: A): TryDman[A] = a.right[DmanFailure]
    def fail[A](msg: String): TryDman[A] = (DmanFailure.Unexpected(msg): DmanFailure).left[A]
  }

  implicit class OptionOps[A](o: Option[A]) {
    def toTryDman(msg: => String): TryDman[A] =
      o.toRightDisjunction(DmanFailure.Unexpected(msg))
  }

  type DmanAction[+A] = EitherT[Task, DmanFailure, A]

  object DmanAction {
    def apply[A](a: => A): DmanAction[A] = EitherT(Task.delay(a.right))
    def unit: DmanAction[Unit] = apply(())
    def fail(f: => DmanFailure): DmanAction[Nothing] = EitherT(Task.delay(f.left))

    def mergeFailure[A](result: Throwable \/ TryDman[A]): TryDman[A] =
      result.fold({
        case ie: InterruptedException => throw ie
        case ex                       => DmanFailure.DmanException(ex).left
      }, identity)
  }

  implicit class DmanActionOps[A](a: DmanAction[A]) {
    def unsafeRun: TryDman[A] = DmanAction.mergeFailure(a.run.attemptRun)

    def forkAsync(f: TryDman[A] => Unit): Unit =
      Task.fork(a.run).runAsync(result => f(DmanAction.mergeFailure(result)))
  }

  implicit object DmanActionMonad extends Monad[DmanAction] {
    def point[A](a: => A): DmanAction[A] = DmanAction(a)
    def bind[A, B](fa: DmanAction[A])(f: A => DmanAction[B]): DmanAction[B] = fa.flatMap(f)
  }

  implicit class TaskDmanActionOps[A](val a: Task[TryDman[A]]) extends AnyVal {
    def liftDman: DmanAction[A] = EitherT(a)
  }

  implicit class DmanEitherOps[A](e: => TryDman[A]) {
    def liftDman: DmanAction[A] = EitherT(Task.delay(e))
  }

  def tryOp[A](a: => A): TryDman[A] =
    catchingNonFatal(a).leftMap(t => DmanException(t): DmanFailure)
}
