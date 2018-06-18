package edu.gemini.dbTools

import scalaz._
import Scalaz._
import scalaz.effect.IO

package object maskcheck {

  type Action[A] = EitherT[IO, Throwable, A]

  object Action {

    def unit: Action[Unit] =
      EitherT.right(IO.ioUnit)

    def fromNullableOp[A](failMessage: => String)(a: => A): Action[A] =
      fromOption(failMessage)(Option(a))

    def fromOption[A](failMessage: => String)(oa: => Option[A]): Action[A] =
      EitherT.fromDisjunction[IO](oa \/> new RuntimeException(failMessage))

    def catchLeft[A](a: => A): Action[A] =
      EitherT(IO(a).catchLeft)

    def delay[A](a: => A): Action[A] =
      EitherT.right(IO(a))

    def fail[A](failMessage: => String): Action[A] =
      EitherT.left(IO(new RuntimeException(failMessage)))

  }


}
