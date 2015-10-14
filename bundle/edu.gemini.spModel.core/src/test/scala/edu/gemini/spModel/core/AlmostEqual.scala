package edu.gemini.spModel.core

import scalaz._, Scalaz._

trait AlmostEqual[A] { outer =>
  def almostEqual(a: A, b: A): Boolean
  def contramap[B](f: B => A): AlmostEqual[B] =
    new AlmostEqual[B] {
      def almostEqual(a: B, b: B) =
        outer.almostEqual(f(a), f(b))
    }
}

object AlmostEqual {

  def by[A, B](f: B => A)(implicit ev: AlmostEqual[A]): AlmostEqual[B] =
    ev.contramap(f)

  implicit class AlmostEqualOps[A](a: A)(implicit A: AlmostEqual[A]) {
    def ~=(b: A): Boolean = A.almostEqual(a, b)
  }

  implicit def AlmostEqualOption[A: AlmostEqual]: AlmostEqual[Option[A]] =
    new AlmostEqual[Option[A]] {
      def almostEqual(a: Option[A], b: Option[A]) =
        (a |@| b)(_ ~= _).getOrElse(true)
    }

  implicit def AlmostEqualList[A: AlmostEqual]: AlmostEqual[List[A]] =
    new AlmostEqual[List[A]] {
      def almostEqual(a: List[A], b: List[A]) =
        a.corresponds(b)(_ ~= _)
    }

  implicit val DoubleAlmostEqual =
    new AlmostEqual[Double] {
      def almostEqual(a: Double, b: Double) =
        (a - b).abs < 0.00001
    }

  implicit val AngleAlmostEqual = by((_: Angle).toDegrees)
  implicit val RightAscensionAngularVelocityAlmostEqual = by((_: RightAscensionAngularVelocity).velocity.masPerYear)
  implicit val DeclinationAngularVelocityAlmostEqual = by((_: DeclinationAngularVelocity).velocity.masPerYear)
  implicit val RightAscensionAlmostEqual = by((_: RightAscension).toAngle)
  implicit val DeclinationAlmostEqual = by((_: Declination).toAngle)
  implicit val WavelengthAlmostEqual = by((_: Wavelength).toNanometers)
  implicit val RedshiftAlmostEqual = by((_: Redshift).redshift)
  implicit val ParallaxAlmostEqual = by((_: Parallax).angle)
  implicit val RadialVelocityAlmostEqual = by((_: RadialVelocity).velocity.toKilometersPerSecond)
  implicit val EpochAlmostEqual = by((_: Epoch).year)

  implicit val CoordinatesAlmostEqual =
    new AlmostEqual[Coordinates] {
      def almostEqual(a: Coordinates, b: Coordinates) =
        (a.ra ~= b.ra) && (a.dec ~= b.dec)
    }

  implicit val ProperMotionAlmostEqual =
    new AlmostEqual[ProperMotion] {
      def almostEqual(a: ProperMotion, b: ProperMotion) =
        (a.deltaRA ~=  b.deltaRA) && (a.deltaDec ~= b.deltaDec)
    }

  implicit val MagnitudeAlmostEqual = 
    new AlmostEqual[Magnitude] {
      def almostEqual(a: Magnitude, b: Magnitude) =
        (a.band   == b.band)   && 
        (a.system == b.system) &&
        (a.value  ~= b.value)  &&
        (a.error  ~= b.error)
    }

  implicit val EphemerisElementAlmostEqual =
    new AlmostEqual[(Long, Coordinates)] {
      def almostEqual(a: (Long, Coordinates), b: (Long, Coordinates)) =
        (a._1 == b._1) && (a._2 ~= b._2)
    }

  implicit val SiderealTargetAlmostEqual =
    new AlmostEqual[Target.SiderealTarget] {
      def almostEqual(a: Target.SiderealTarget, b: Target.SiderealTarget) =
        (a.name           == b.name)           &&
        (a.coordinates    ~= b.coordinates)    &&
        (a.properMotion   ~= b.properMotion)   &&
        (a.radialVelocity ~= b.radialVelocity) &&
        (a.redshift       ~= b.redshift)       &&
        (a.parallax       ~= b.parallax)       &&
        (a.magnitudes     ~= b.magnitudes)
    }

  implicit val NonSiderealTargetAlmostEqual =
    new AlmostEqual[Target.NonSiderealTarget] {
      def almostEqual(a: Target.NonSiderealTarget, b: Target.NonSiderealTarget) =
        (a.name == b.name) &&
        (a.ephemeris.toList ~= b.ephemeris.toList) &&
        (a.horizonsDesignation == b.horizonsDesignation) &&
        (a.magnitudes ~= b.magnitudes)
    }

  implicit val TargetAlmostEqual =
    new AlmostEqual[Target] {
      def almostEqual(a: Target, b: Target) =
        (a, b) match {
          case (a: Target.TooTarget, b: Target.TooTarget) => a == b
          case (a: Target.SiderealTarget, b: Target.SiderealTarget) => a ~= b
          case (a: Target.NonSiderealTarget, b: Target.NonSiderealTarget) => a ~= b
          case _ => false
        }
    }

}
