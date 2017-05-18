package edu.gemini.spModel.core

import squants.motion.Velocity
import squants.radio.{SpectralIrradiance, Irradiance}

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

  def apply[A](implicit ev: AlmostEqual[A]): ev.type = ev

  def by[A, B](f: B => A)(implicit ev: AlmostEqual[A]): AlmostEqual[B] =
    ev.contramap(f)

  implicit class AlmostEqualOps[A](a: A)(implicit A: AlmostEqual[A]) {
    def ~=(b: A): Boolean = A.almostEqual(a, b)

    // squants already defines ~= so here's an overload
    def almostEquals(b: A): Boolean = ~=(b)

  }

  implicit def AlmostEqualOption[A: AlmostEqual]: AlmostEqual[Option[A]] =
    new AlmostEqual[Option[A]] {
      def almostEqual(a: Option[A], b: Option[A]) =
        (a, b) match {
          case (Some(a), Some(b)) => a ~= b
          case (None, None)       => true
          case _                  => false
        }
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
  implicit val RedshiftAlmostEqual = by((_: Redshift).z)
  implicit val ParallaxAlmostEqual = by((_: Parallax).mas)
  implicit val EpochAlmostEqual = by((_: Epoch).year)
  implicit val VelocityAlmostEqual = by((_: Velocity).toKilometersPerSecond)
  implicit val IrradianceAlmostEqual = by((_: Irradiance).toErgsPerSecondPerSquareCentimeter)
  implicit val SpectralIrradianceAlmostEqual = by((_: SpectralIrradiance).toErgsPerSecondPerSquareCentimeterPerAngstrom)

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

  implicit val SpectralDistributionAlmostEqual =
    new AlmostEqual[SpectralDistribution] {
      def almostEqual(a: SpectralDistribution, b: SpectralDistribution) =
        (a, b) match {
          case (BlackBody(a), BlackBody(b))   => a ~= b
          case (PowerLaw(a), PowerLaw(b))     => a ~= b
          case (EmissionLine(a, b, c, d),
                EmissionLine(a0, b0, c0, d0)) => (a ~= a0) && (b almostEquals b0) &&
                                                              (c almostEquals c0) &&
                                                              (d almostEquals d0)
          case (a, b)                         => a == b // others are comparable directly
        }
    }

  implicit val SpatialProfileAlmostEqual =
    new AlmostEqual[SpatialProfile] {
      def almostEqual(a: SpatialProfile, b: SpatialProfile) =
        (a, b) match {
          case (GaussianSource(a), GaussianSource(b)) => a ~= b
          case (a, b) => a == b // others are comparable directly
        }
    }

  implicit val SiderealTargetAlmostEqual =
    new AlmostEqual[SiderealTarget] {
      def almostEqual(a: SiderealTarget, b: SiderealTarget) =
        (a.name                 == b.name)                 &&
        (a.coordinates          ~= b.coordinates)          &&
        (a.properMotion         ~= b.properMotion)         &&
        (a.redshift             ~= b.redshift)             &&
        (a.parallax             ~= b.parallax)             &&
        (a.magnitudes           ~= b.magnitudes)           &&
        (a.spectralDistribution ~= b.spectralDistribution) &&
        (a.spatialProfile       ~= b.spatialProfile)
    }

  implicit val EphemerisAlmostEqual =
    new AlmostEqual[Ephemeris] {
      def almostEqual(a: Ephemeris, b: Ephemeris) =
        (a.site   == b.site)   &&
        (a.toList ~= b.toList)
    }

  implicit val NonSiderealTargetAlmostEqual =
    new AlmostEqual[NonSiderealTarget] {
      def almostEqual(a: NonSiderealTarget, b: NonSiderealTarget) =
        (a.name                 == b.name)                 &&
        (a.ephemeris            ~= b.ephemeris)            &&
        (a.horizonsDesignation  == b.horizonsDesignation)  &&
        (a.magnitudes           ~= b.magnitudes)           &&
        (a.spectralDistribution ~= b.spectralDistribution) &&
        (a.spatialProfile       ~= b.spatialProfile)
    }

  implicit val TargetAlmostEqual =
    new AlmostEqual[Target] {
      def almostEqual(a: Target, b: Target) =
        (a, b) match {
          case (a: TooTarget, b: TooTarget) => a == b
          case (a: SiderealTarget, b: SiderealTarget) => a ~= b
          case (a: NonSiderealTarget, b: NonSiderealTarget) => a ~= b
          case _ => false
        }
    }

}
