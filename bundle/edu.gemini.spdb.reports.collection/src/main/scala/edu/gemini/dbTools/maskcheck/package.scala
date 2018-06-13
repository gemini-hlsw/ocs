package edu.gemini.dbTools

import scalaz._
import Scalaz._
import scalaz.effect.IO

package object maskcheck {

  type MC[A] = EitherT[IO, Throwable, A]

  object MC {

    def mcUnit: MC[Unit] =
      EitherT.right(IO.ioUnit)

    def fromNullableOp[A](failMessage: => String)(a: => A): MC[A] =
      fromOption(failMessage)(Option(a))

    def fromOption[A](failMessage: => String)(oa: => Option[A]): MC[A] =
      EitherT.fromDisjunction[IO](oa \/> new RuntimeException(failMessage))

    def catchLeft[A](a: => A): MC[A] =
      EitherT(IO(a).catchLeft)

    def delay[A](a: => A): MC[A] =
      EitherT.right(IO(a))

    def fail[A](failMessage: => String): MC[A] =
      EitherT.left(IO(new RuntimeException(failMessage)))

  }


}
