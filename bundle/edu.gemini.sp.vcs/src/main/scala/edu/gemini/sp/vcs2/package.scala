package edu.gemini.sp

import edu.gemini.pot.sp._
import edu.gemini.sp.vcs2.VcsFailure.{Unexpected, VcsException}
import edu.gemini.spModel.rich.pot.sp._

import edu.gemini.shared.util.immutable.ScalaConverters._

import scalaz._
import Scalaz._
import scalaz.concurrent._

package object vcs2 {

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
    \/.fromTryCatchNonFatal(a).leftMap { t =>
      VcsException(t): VcsFailure
    }.flatMap { Option(_).toTryVcs(failureMessage) }

  type VcsAction[A] = EitherT[Task, VcsFailure, A]

  implicit object VcsActionMonad extends Monad[VcsAction] {
    def point[A](a: => A): VcsAction[A] = VcsAction(a)
    def bind[A, B](fa: VcsAction[A])(f: A => VcsAction[B]): VcsAction[B] = fa.flatMap(f)
  }

  object VcsAction {
    def apply[A](a: => A): VcsAction[A] = EitherT(Task.delay(a.right))
    def unit: VcsAction[Unit] = apply(())
    def fail[A](vf: => VcsFailure): VcsAction[A] = EitherT(Task.delay(vf.left))

    implicit class VcsActionOps[A](a: VcsAction[A]) {
      private def merge(result: Throwable \/ TryVcs[A]): TryVcs[A] =
        result.fold(ex => VcsFailure.VcsException(ex).left, identity)

      /** Execute this action, performing any side-effects. */
      def unsafeRun: TryVcs[A] = merge(a.run.unsafePerformSyncAttempt)

      /** Executes the action in a separate thread and calls the supplied
        * handler when finished. */
      def forkAsync(f: TryVcs[A] => Unit): Unit =
        Task.fork(a.run).unsafePerformAsync(result => f(merge(result)))
    }
  }

  implicit class TaskVcsActionOps[A](val a: Task[TryVcs[A]]) extends AnyVal {
    def liftVcs: VcsAction[A] = EitherT(a)
  }

  implicit class VcsEitherOps[A](e: => TryVcs[A]) {
    def liftVcs: VcsAction[A] = EitherT(Task.delay(e))
  }

  implicit class VcsFailureOps(f: => VcsFailure) {
    def liftVcs[A]: VcsAction[A] = f.left[A].liftVcs
  }

  implicit val ShowConflicts: Show[Conflicts] = Show.shows[Conflicts] { c =>
    def showNote(cn: Conflict.Note): String =
      cn match {
        case n: Conflict.Moved                  => s"Move(${n.nodeKey}, ${n.to})"
        case n: Conflict.ResurrectedLocalDelete => s"ResurrectedLocalDelete(${n.nodeKey})"
        case n: Conflict.ReplacedRemoteDelete   => s"ReplacedRemoteDelte(${n.nodeKey})"
        case n: Conflict.CreatePermissionFail   => s"CreatePermissionFail(${n.nodeKey})"
        case n: Conflict.UpdatePermissionFail   => s"UpdatePermissionFail(${n.nodeKey})"
        case n: Conflict.DeletePermissionFail   => s"DeletePermissionFail(${n.nodeKey})"
        case n: Conflict.ConstraintViolation    => s"ConstraintViolation(${n.nodeKey})"
        case n: Conflict.ConflictFolder         => s"ConflictFolder(${n.nodeKey})"
      }

    val doc   = c.dataObjectConflict.asScalaOpt.map(doc => s"DataObjectConflict(${doc.dataObject.getType}, ${doc.perspective})")
    val notes = c.notes.asScalaList.map(showNote).mkString(", ")
    doc.map(d => s"$d, $notes") | notes
  }
}
