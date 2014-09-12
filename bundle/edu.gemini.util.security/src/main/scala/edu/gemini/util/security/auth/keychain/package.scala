package edu.gemini.util.security.auth

import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.core.Site
import scalaz._
import Scalaz._
import scalaz.effect._
import scalaz.effect.IO._
import java.security.Principal
import java.security.PrivateKey
import edu.gemini.util.security.principal._

package object keychain {

  type Signed[A] = edu.gemini.util.security.auth.Signed[A]
  val Signed = edu.gemini.util.security.auth.Signed

  type Sealed[A] = edu.gemini.util.security.auth.Sealed[A]
  val Sealed = edu.gemini.util.security.auth.Sealed


  type UserPrincipal = edu.gemini.util.security.principal.UserPrincipal
  type KeyVersion = Int
  type Action[+A] = EitherT[IO, KeyFailure, A]

  implicit object ActionMonadIO extends MonadIO[Action] {
    def point[A](a: => A): Action[A] = Action(a)
    def bind[A, B](fa: Action[A])(f: A => Action[B]): Action[B] = fa.flatMap(f)
    def liftIO[A](ioa: IO[A]): Action[A] = EitherT(ioa.map(_.right))
  }

  object Action {

    def apply[A](a: => A): Action[A] = EitherT(IO(a.right))
    def fail(kf: => KeyFailure): Action[Nothing] = EitherT(IO(kf.left))

    implicit class ActionOps[A](a: Action[A]) {

      /**
       * Execute this action, performing any side-effects and promoting KeyFailure to KeyException.
       */
      def unsafeRun: Throwable \/ A =
        a.run.catchLeft.unsafePerformIO.fold(_.left, _.leftMap(_.toException))

      def unsafeRunAndThrow: A =
        unsafeRun.fold(throw _, identity)

    }

  }

  type Key = Signed[(GeminiPrincipal, KeyVersion)]

  object Key {
    def sign(pk: PrivateKey, p: GeminiPrincipal, v: KeyVersion): Action[Key] = 
      Signed.sign((p, v), pk).fold(e => Action.fail(KeyFailure.InvalidSignature(e)), Action.apply(_))
  }

  implicit class KeyOps(k: Key) {
    def principal: GeminiPrincipal = k.get._1
    def version: KeyVersion = k.get._2
  }

  implicit val siteEqual: Equal[Site] =
    Equal.equalA



  type MD5 = String

  object MD5 {
    def apply(s:String): MD5 = s
    implicit val eq: Equal[MD5] = Equal.equalA
  }

}
