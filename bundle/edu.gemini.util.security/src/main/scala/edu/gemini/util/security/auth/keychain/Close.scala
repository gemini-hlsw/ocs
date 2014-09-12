package edu.gemini.util.security.auth.keychain

import edu.gemini.spModel.core.Peer
import edu.gemini.spModel.core.Site
import java.io._
import java.security.Principal
import scalaz._
import Scalaz._
import scalaz.effect.IO

// Typeclass for things that can be closed.

trait Close[A] {
  def close(a: A): IO[Unit]
}

object Close {

  def apply[A](implicit A: Close[A]): Close[A] = A

  def close[A, C : Close](f: C => IO[A])(s: C): IO[A] =
    f(s) ensuring Close[C].close(s)

  implicit def CloseInputStream[A <: InputStream]: Close[A] =
    new Close[A] {
      def close(o: A) = IO(o.close)
    }

  implicit def CloseOutputStream[A <: OutputStream]: Close[A] =
    new Close[A] {
      def close(o: A) = IO(o.close)
    }

}

