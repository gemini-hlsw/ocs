package edu.gemini.dbTools.ephemeris

import scalaz._, Scalaz._
import scalaz.effect.IO

/** Convenience methods for working with TryExport. */
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
