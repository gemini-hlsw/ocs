package edu.gemini.util.trpc.auth

import edu.gemini.util.security.auth.keychain._
import edu.gemini.util.security.auth.keychain.KeyChain.KeyFetcher
import edu.gemini.spModel.core.Peer
import edu.gemini.util.security.principal.GeminiPrincipal
import java.io.File
import edu.gemini.util.trpc.client.TrpcClient
import scalaz._
import scalaz.effect._
import Scalaz._

object TrpcKeyChain {

  def apply(f: File, initialPeers: List[Peer]): Action[KeyChain] =
    KeyChain.apply(f, fetcher, initialPeers)

  lazy val fetcher: KeyChain.KeyFetcher =
    new KeyFetcher {

      def checkConnection(peer: Peer): Action[Unit] =
        withKeyService(peer, _.testConnection())

      def retrieveKey(peer: Peer, principal: GeminiPrincipal, pass: Array[Char]): Action[Key] =
        withKeyService(peer, _.tryKey(principal, pass.mkString))

      def validateKey(peer: Peer, key: Key): Action[Unit] =
        withKeyService(peer, _.validateKey(key))

      def resetPasswordAndNotify(peer: Peer, u: UserPrincipal): Action[Unit] =
        withKeyService(peer, _.resetPasswordAndNotify(u))

      def withKeyService[A](peer: Peer, f: KeyService => KeyFailure \/ A): Action[A] =
        EitherT.eitherT(IO(TrpcClient(peer).withoutKeys.apply(r => f(r[KeyService])).fold(throw _, identity)))

    }

}



