package edu.gemini.sp.vcs

import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.pot.sp.ISPProgram
import edu.gemini.pot.sp.version._
import edu.gemini.sp.vcs.VcsFailure._

import scalaz._
import edu.gemini.sp.vcs.log.VcsEventSet
import edu.gemini.util.security.auth.keychain.KeyChain

/**
 *
 */
case class TrpcVcsServer(kc: KeyChain, host: String, port: Int) extends VcsServer {
  import edu.gemini.util.trpc.client.TrpcClient

  private def mergeLefts[F>:VcsException,T](v: \/[Exception, F \/ T]): F \/ T =
    v.fold(ex => -\/(VcsException(ex)), identity)

  private def call[F>:VcsException,T](op: VcsServer => F \/ T): F \/ T = {
    val vcsServer = TrpcClient(host, port).withKeyChain(kc)
    mergeLefts(vcsServer { remote => op(remote[VcsServer]) })
  }

  def version(id: SPProgramID): TryVcs[VersionMap] =
    call(_.version(id))

  def fetch(id: SPProgramID): TryVcs[ISPProgram] =
    call(_.fetch(id))

  def store(p: ISPProgram): TryVcs[VersionMap] =
    call(_.store(p))

  def log(p: SPProgramID, offset: Int, length: Int): VcsFailure.TryVcs[(List[VcsEventSet], Boolean)] =
    call(_.log(p, offset, length))

}

object TrpcVcsServer {
  def apply(kc: KeyChain, location: Peer): TrpcVcsServer =
    TrpcVcsServer(kc, location.host, location.port)
}
