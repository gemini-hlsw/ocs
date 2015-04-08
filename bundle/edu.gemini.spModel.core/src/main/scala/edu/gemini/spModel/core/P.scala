package edu.gemini.spModel.core

import scalaz.{Order, Monoid, Show}

/**
 * Offset in P.
 */
case class P(toAngle: Angle) extends AnyVal

object P {
  implicit val IsoAngleP = new IsoAngle[P] {
    override def toDegrees(p: P): Double   = Angle.signedDegrees(p.toAngle.toDegrees)
    override def fromDegrees(d: Double): P = P(Angle.fromDegrees(d))
  }

  import AngleSyntax._

  implicit val ShowP: Show[P] =
    Show.shows(p => f"${p.arcsecs}%4.03f arcsecs")

  implicit val MonoidP: Monoid[P] =
    new Monoid[P] {
      val zero = 0.arcsecs[P]
      def append(a: P, b: => P): P = a + b
    }

  implicit val OrderP: Order[P] =
    Order.orderBy(_.degrees)
}
