package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPFactory, ISPProgram}
import edu.gemini.pot.sp.version._
import edu.gemini.shared.util.VersionComparison.{Same, Newer}
import edu.gemini.sp.vcs.diff.MergePlan.Transport
import edu.gemini.sp.vcs.diff.ProgramLocation.{LocalOnly, Neither, Remote}
import edu.gemini.sp.vcs.diff.VcsFailure.{NeedsUpdate, VcsException}
import edu.gemini.sp.vcs.log.VcsEventSet
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.util.security.auth.keychain.KeyChain

import java.security.Principal

import edu.gemini.util.trpc.client.TrpcClient

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

import Vcs._

import scalaz.concurrent.Task

/** Vcs provides the public API for vcs commands such as push, pull and sync. */
class Vcs(user: VcsAction[Set[Principal]], server: VcsServer, service: Peer => VcsService) {

  /** Checks-out the indicated program from the remote peer, copying it into
    * the local database. */
  def checkout(id: SPProgramID, peer: Peer): VcsAction[ISPProgram] =
    for {
      p <- Client(peer).checkout(id)
      u <- user
      _ <- server.add(p, u)
    } yield p

  /** Adds the given program to the remote peer, copying it into the remote
    * database. */
  def add(id: SPProgramID, peer: Peer): VcsAction[Unit] =
    for {
      p <- server.lookup(id)
      _ <- Client(peer).add(p)
    } yield ()

  // pull0 is shared by `pull` and `sync`, since the first half of a sync is
  // to merge in changes from the remote peer.  The local merge is only
  // performed if the remote peer has something new to offer.
  private def pull0(id: SPProgramID, client: Client): VcsAction[MergeEval] = {
    def evaluate(p: ISPProgram): VcsAction[MergeEval] =
      for {
        diffs <- client.fetchDiffs(id, DiffState(p))
        mc     = MergeContext(p, diffs)
        prelim = PreliminaryMerge.merge(mc)
        plan  <- ObsNumberCorrection(mc).apply(prelim).liftVcs
      } yield MergeEval(plan, p, mc.remote.vm)

    // Only do the merge if the merge plan has something new to offer.
    def filter(eval: MergeEval): Boolean = eval.localUpdate

    def update(f: ISPFactory, p: ISPProgram, eval: MergeEval): VcsAction[Unit] =
      eval.plan.merge(f, p)

    user >>= { u => server.write[MergeEval](id, u, evaluate, filter, update) }
  }

  /** Provides an action that will pull changes from the indicated remote peer
    * and merge them with the local program if necessary.  The action returns
    * `true` if it results in an updated local program; `false` otherwise.
    */
  def pull(id: SPProgramID, peer: Peer): VcsAction[Boolean] =
    pull0(id, Client(peer)).map(_.localUpdate)

  /** Provides an action that pushes local changes to the remote peer, merging
    * them with the remote version of the program if necessary.  When performed,
    * the action returns `true` if it results in an update remote program.
    *
    * Note that the push will only be possible if the local version is strictly
    * newer than the remote version. Otherwise it fails with a `NeedsUpdate`
    * `VcsFailure`. */
  def push(id: SPProgramID, peer: Peer): VcsAction[Boolean] = {
    val client = Client(peer)
    for {
      diffState <- client.diffState(id)
      u         <- user
      diff      <- server.read(id, u) { ProgramDiff.compare(_, diffState) }
      res       <- diff.compare(diffState.vm) match {
        case Newer => client.storeDiffs(id, diff)
        case Same  => VcsAction(false)
        case _     => VcsAction.fail(NeedsUpdate)
      }
    } yield res
  }

  /**
   * Provides an action that synchronizes changes in the local program with the
   * remote peer, merging updates from the remote program with local changes
   * and then sending the resulting merged program to the peer.  When performed,
   * the action returns a `ProgramLocationSet` which describes whether either
   * or both of the local and remote versions of the program were updated. */
  def sync(id: SPProgramID, peer: Peer): VcsAction[ProgramLocationSet] = {
    val client = Client(peer)
    for {
      eval <- pull0(id, client)
      s0    = if (eval.localUpdate) LocalOnly else Neither
      res  <- eval match {
        case MergeEval(_,     _, false) =>
          VcsAction(s0)

        case MergeEval(diffs, _, true)  =>
          client.storeDiffs(id, diffs).map { updated =>
            if (updated) s0 + Remote else s0
          }
      }
    } yield res
  }

  /** Returns a `VcsAction` that will sync the program with the remote peer,
    * retrying if it fails because the program was updated remotely while
    * performing the merge locally.  Retry up to `retryCount` times if
    * necessary. */
  def retrySync(id: SPProgramID, peer: Peer, retryCount: Int): VcsAction[Unit] = {
    def retryIfNeedsUpdate(f: VcsFailure): EitherT[Task, Unit, VcsFailure] = f match {
      case NeedsUpdate  => if (retryCount <= 0) EitherT.right(Task.delay(NeedsUpdate))
                           else retrySync(id, peer, retryCount - 1).swap
      case otherFailure => EitherT.right(Task.delay(otherFailure))
    }

    (sync(id, peer).as(()).swap >>= retryIfNeedsUpdate).swap
  }

  /** Provides access to (a chunk of) the VCS log. */
  def log(id: SPProgramID, peer: Peer, offset: Int, length: Int): VcsAction[(List[VcsEventSet], Boolean)] =
    Client(peer).log(id, offset, length)

  case class Client(peer: Peer) {
    val s = service(peer)

    def version(id: SPProgramID): VcsAction[VersionMap]  = s.version(id).liftVcs
    def add(p: ISPProgram): VcsAction[Unit]              = s.add(p).liftVcs
    def checkout(id: SPProgramID): VcsAction[ISPProgram] = s.checkout(id).liftVcs
    def diffState(id: SPProgramID): VcsAction[DiffState] = s.diffState(id).liftVcs

    def fetchDiffs(id: SPProgramID, vs: DiffState): VcsAction[MergePlan] =
      s.fetchDiffs(id, vs).map(_.decode).liftVcs

    def storeDiffs(id: SPProgramID, mp: MergePlan): VcsAction[Boolean] =
      s.storeDiffs(id, mp.encode).liftVcs

    def log(id: SPProgramID, offset: Int, length: Int): VcsAction[(List[VcsEventSet], Boolean)] =
      s.log(id, offset, length).liftVcs
  }
}

object Vcs {

  def apply(kc: KeyChain, server: VcsServer): Vcs = {
    new Vcs(VcsAction(kc.subject.getPrincipals.asScala.toSet), server, VcsService.client(_, kc))
  }

  /** Evaluation of the merge state, which includes whether local and/or remote
    * updates are needed.  We can skip merging locally or remotely if nothing
    * would be changed anyway. */
  private case class MergeEval(plan: MergePlan, localUpdate: Boolean, remoteUpdate: Boolean)

  private object MergeEval {
    def apply(plan: MergePlan, p: ISPProgram, remoteVm: VersionMap): MergeEval =
      MergeEval(plan, plan.compare(p.getVersions) === Newer,
                      plan.compare(remoteVm)      === Newer)
  }
}
