package edu.gemini.dbTools

import edu.gemini.spModel.core.{HorizonsDesignation, Coordinates}

import java.time.Instant

import scalaz.effect.IO
import scalaz._, Scalaz._

package object ephemeris {
  type EphemerisElement = (Coordinates, Double, Double)
  type EphemerisMap     = Instant ==>> EphemerisElement

  type TryExport[A] = EitherT[IO, ExportError, A]

  // Need Order to use HorizonsDesignation as a ==>> key
  private[ephemeris] implicit val OrderHorizonsDesignation: Order[HorizonsDesignation] =
    Order.orderBy(_.toString)

  private[ephemeris] implicit val OrderInstant: Order[Instant] =
    Order.orderBy(_.toEpochMilli)
}
