package edu.gemini.spModel.core

import scalaz.{Order, Monoid, Show}

/**
 * Offset in Q.
 */
case class OffsetQ(toAngle: Angle) extends AnyVal

object OffsetQ {
  val Zero = OffsetQ(Angle.zero)

  implicit val IsoAngleQ = new IsoAngle[OffsetQ] {
    override def toDegrees(q: OffsetQ): Double   = Angle.signedDegrees(q.toAngle.toDegrees)
    override def fromDegrees(d: Double): OffsetQ = OffsetQ(Angle.fromDegrees(d))
  }

  import AngleSyntax._

  implicit val ShowQ: Show[OffsetQ] =
    Show.shows(q => f"${q.arcsecs}%4.03f arcsecs")

  implicit val MonoidQ: Monoid[OffsetQ] =
    new Monoid[OffsetQ] {
      val zero = Zero
      def append(a: OffsetQ, b: => OffsetQ): OffsetQ = IsoAngleQ.add(a, b)
    }

  implicit val OrderQ: Order[OffsetQ] =
    Order.orderBy(_.degrees)
}
