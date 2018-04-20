package jsky.app.ot.vcs

import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.sp.version.VersionMap
import edu.gemini.sp.vcs.log.VcsEventSet
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.sp.vcs2._
import edu.gemini.spModel.core.{Peer, SPProgramID}
import jsky.app.ot.vcs.vm.VmStore

import java.util.concurrent.atomic.AtomicBoolean

import scala.swing.Swing

object VcsOtClient {
  private var client: Option[VcsOtClient] = None

  def ref: Option[VcsOtClient] = client

  def ref_=(c: Option[VcsOtClient]): Unit = {
    client = c
  }

  def unsafeGetRegistrar: VcsRegistrar =
    ref.fold(sys.error("OT not initialized for VCS"))(_.reg)
}

/** Provides a simplified VCS API for the OT that Wraps the raw `Vcs` API to
  * handle peer lookup from the `VcsRegistrar` and update the `VmStore` map
  * with sync results. */
case class VcsOtClient(vcs: Vcs, reg: VcsRegistrar) {

  def peer(id: SPProgramID): Option[Peer] = reg.registration(id)
  def peerOrNull(id: SPProgramID): Peer   = reg.registrationOrNull(id)

  def checkout(id: SPProgramID, peer: Peer, cancelled: AtomicBoolean): VcsAction[ISPProgram] =
    for {
      p <- vcs.checkout(id, peer, cancelled)
      _ <- VcsAction(reg.register(id, peer))
    } yield vmStore(id, p, force = false)(_.getVersions)

  def revert(id: SPProgramID, peer: Peer, cancelled: AtomicBoolean): VcsAction[ISPProgram] =
      vcs.revert(id, peer, cancelled).map { p =>
        vmStore(id, p, force = true)(_.getVersions)
      }

  def add(id: SPProgramID, peer: Peer): VcsAction[VersionMap] =
    for {
      vm <- vcs.add(id, peer)
      _  <- VcsAction(reg.register(id, peer))
    } yield vmStore(id, vm, force = false)(identity)

  private def lookupAndThen[A](id: SPProgramID)(f: (Vcs, Peer) => VcsAction[A]): VcsAction[A] =
    for {
      p <- peer(id).toTryVcs(s"Program '$id' has not been checked out of any database.").liftVcs
      a <- f(vcs, p)
    } yield a

  private def recording[A](id: SPProgramID)(f: (Vcs, Peer) => VcsAction[A])(g: A => VersionMap): VcsAction[A] =
    lookupAndThen(id)(f).map(vmStore(id, _, force = false)(g))

  def version(id: SPProgramID): VcsAction[VersionMap] =
    recording(id)(_.version(id, _))(identity)

  def pull(id: SPProgramID, cancelled: AtomicBoolean): VcsAction[(PullResult, VersionMap)] =
    recording(id)(_.pull(id, _, cancelled))(_._2)

  def sync(id: SPProgramID, cancelled: AtomicBoolean): VcsAction[(ProgramLocationSet, VersionMap)] =
    recording(id)(_.retrySync(id, _, cancelled, 10))(_._2)

  def log(id: SPProgramID, offset: Int, length: Int): VcsAction[(List[VcsEventSet], Boolean)] =
    lookupAndThen(id)(_.log(id, _, offset, length))

  // Performs the side-effect of updating the map from id to VersionMap.
  private def vmStore[A](id: SPProgramID, a: A, force: Boolean)(f: A => VersionMap): A = {
    Swing.onEDT { VmStore.update(id, f(a), force) }
    a
  }
}
