package edu.gemini.dbTools

import edu.gemini.spModel.core.{HorizonsDesignation, Coordinates}

import java.time.Instant

import scalaz.effect.IO
import scalaz._, Scalaz._

package object ephemeris {
  type EphemerisElement = (Coordinates, Double, Double)
  type EphemerisMap     = Instant ==>> EphemerisElement

  type TryExport[A] = EitherT[IO, ExportError, A]

  object TryExport {
    def liftExport[A](a: IO[ExportError \/ A]): TryExport[A] =
      EitherT[IO, ExportError, A](a)

    def fromDisjunction[A](a: => ExportError \/ A): TryExport[A] =
      liftExport(IO(a))

    def apply[A](a: => A): TryExport[A] =
      fromDisjunction(a.right[ExportError])

    def unit: TryExport[Unit] =
      apply(())

    def fail[A](e: ExportError): TryExport[A] =
      fromDisjunction(e.left[A])

    def fromTryCatch[A](e: Throwable => ExportError)(a: => A): TryExport[A] =
      fromDisjunction {
        \/.fromTryCatchNonFatal(a).leftMap(e)
      }
  }

  // Need Order to use HorizonsDesignation as a ==>> key
  private[ephemeris] implicit val OrderHorizonsDesignation: Order[HorizonsDesignation] =
    Order.orderBy(_.toString)

  private[ephemeris] implicit val OrderInstant: Order[Instant] =
    Order.orderBy(_.toEpochMilli)
}
