package edu.gemini.sp.vcs

import edu.gemini.pot.sp.{ISPProgram, SPNodeKey, ISPNode}
import edu.gemini.sp.vcs.diff.VcsFailure.{Unexpected, VcsException}
import edu.gemini.spModel.rich.pot.sp._

import scalaz._
import Scalaz._
import scalaz.concurrent._

package object diff {

  implicit class IspNodeTreeOps(val node: ISPNode) extends AnyVal {
    /** A Map with entries for all nodes rooted at this node, keyed by
      * `SPNodeKey`.
      */
    def nodeMap: Map[SPNodeKey, ISPNode] =
      node.fold(Map.empty[SPNodeKey, ISPNode]) { (m, n) => m + (n.key -> n) }

    /** Set of all the `SPNodeKey` in the subtree of nodes rooted at this node.
      */
    def keySet: Set[SPNodeKey] =
      node.fold(Set.empty[SPNodeKey]) { _ + _.key }
  }

  /** Returns the set of `SPNodeKey` for all nodes no longer in program `p`.
    *
    * Note, requires a complete tree traversal so this is somewhat expensive.
    */
  def removedKeys(p: ISPProgram): Set[SPNodeKey] =
    p.fold(p.getVersions.keySet) { _ - _.key }

  type TryVcs[A] = VcsFailure \/ A

  object TryVcs {
    def apply[A](a: A): TryVcs[A] = a.right[VcsFailure]
    def fail[A](msg: String): TryVcs[A] = (Unexpected(msg): VcsFailure).left[A]
  }

  implicit class OptionOps[A](o: Option[A]) {
    def toTryVcs(msg: => String): TryVcs[A] =
      o.toRightDisjunction(Unexpected(msg))
  }

  /**
   * Safe access to screwy science program model.  Handles exceptions and null
   * elements as VcsFailures.
   */
  def safeGet[A](a: => A, failureMessage: => String): TryVcs[A] =
    \/.fromTryCatch(a).leftMap { t =>
      VcsException(t): VcsFailure
    }.flatMap { Option(_).toTryVcs(failureMessage) }

  type VcsAction[+A] = EitherT[Task, VcsFailure, A]

  implicit object VcsActionMonad extends Monad[VcsAction] {
    def point[A](a: => A): VcsAction[A] = VcsAction(a)
    def bind[A, B](fa: VcsAction[A])(f: A => VcsAction[B]): VcsAction[B] = fa.flatMap(f)
  }

  object VcsAction {
    def apply[A](a: => A): VcsAction[A] = EitherT(Task.delay(a.right))
    def unit: VcsAction[Unit] = apply(())
    def fail(vf: => VcsFailure): VcsAction[Nothing] = EitherT(Task.delay(vf.left))

    implicit class VcsActionOps[A](a: VcsAction[A]) {
      /** Execute this action, performing any side-effects. */
      def unsafeRun: TryVcs[A] = {
        a.run.attemptRun.fold(ex => VcsFailure.VcsException(ex).left, identity)
      }
    }
  }

  implicit class TaskVcsActionOps[A](val a: Task[TryVcs[A]]) extends AnyVal {
    def liftVcs: VcsAction[A] = EitherT(a)
  }

  implicit class VcsEitherOps[A](e: => TryVcs[A]) {
    def liftVcs: VcsAction[A] = EitherT(Task.delay(e))
  }
}
