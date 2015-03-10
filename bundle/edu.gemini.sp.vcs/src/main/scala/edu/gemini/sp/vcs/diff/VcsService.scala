package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.sp.version.VersionMap
import edu.gemini.sp.vcs.diff.VcsFailure.VcsException
import edu.gemini.sp.vcs.log.VcsEventSet
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.trpc.client.TrpcClient

import scalaz._
import Scalaz._

/** Public interface for VCS service. Defines the API for low-level inter-JVM
  * operations that are conducted over trpc.  There is a server implementation
  * in [[edu.gemini.sp.vcs.diff.VcsServer]] and a trpc client in
  * [[edu.gemini.sp.vcs.diff.Vcs]]. */
trait VcsService {

  /** Fetches the `VersionMap`. */
  def version(id: SPProgramID): TryVcs[VersionMap]

  /** Add the given program, copying it to the remote database. */
  def add(p: ISPProgram): TryVcs[Unit]

  /** Checkout the corresponding program, copying it to the local database. */
  def checkout(id: SPProgramID): TryVcs[ISPProgram]

  /** Gets the `VersionMap` and the set of `SPNodeKey` that correspond to
    * deleted nodes. */
  def diffState(id: SPProgramID): TryVcs[DiffState]

  /** Obtains remote differences based on the provided local diff state. */
  def fetchDiffs(id: SPProgramID, ds: DiffState): TryVcs[MergePlan.Transport]

  /** Applies the given `MergePlan` to the remote program, returning `true`
    * if the program is actually updated; `false` otherwise. */
  def storeDiffs(id: SPProgramID, mp: MergePlan.Transport): TryVcs[Boolean]

  /** Fetches a chunk of the vcs log. */
  def log(p: SPProgramID, offset:Int, length:Int): TryVcs[(List[VcsEventSet], Boolean)]
}

object VcsService {
  def client(peer: Peer, kc: KeyChain): VcsService = new VcsService {
    val trpc = TrpcClient(peer).withKeyChain(kc)

    private def call[A](op: VcsService => TryVcs[A]): TryVcs[A] =
      trpc { remote => op(remote[VcsService]) }.valueOr(ex => VcsException(ex).left)

    override def fetchDiffs(id: SPProgramID, ds: DiffState) =
      call(_.fetchDiffs(id, ds))

    override def log(p: SPProgramID, offset: Int, length: Int) =
      call(_.log(p, offset, length))

    override def diffState(id: SPProgramID) =
      call(_.diffState(id))

    override def checkout(id: SPProgramID) =
      call(_.checkout(id))

    override def storeDiffs(id: SPProgramID, mp: MergePlan.Transport) =
      call(_.storeDiffs(id, mp))

    override def add(p: ISPProgram) =
      call(_.add(p))

    override def version(id: SPProgramID) =
      call(_.version(id))
  }
}