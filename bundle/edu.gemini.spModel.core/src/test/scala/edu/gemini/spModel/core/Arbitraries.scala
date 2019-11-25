package edu.gemini.spModel.core

import java.time.{LocalDate, LocalDateTime, LocalTime, Year, ZoneId}

import edu.gemini.spModel.core.WavelengthConversions._
import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._
import squants.motion.VelocityConversions._
import squants.radio.IrradianceConversions._
import squants.radio.SpectralIrradianceConversions._
import scala.collection.JavaConverters._
import scalaz.{==>>, Order}
import scalaz.std.anyVal._

trait Arbitraries {

  implicit val arbAngle: Arbitrary[Angle] =
    Arbitrary(arbitrary[Short].map(n => n / 10.0).map(Angle.fromDegrees))

  implicit val arbP: Arbitrary[OffsetP] = Arbitrary { arbitrary[Angle].map(OffsetP(_)) }
  implicit val arbQ: Arbitrary[OffsetQ] = Arbitrary { arbitrary[Angle].map(OffsetQ(_)) }

  implicit val arbOffset: Arbitrary[Offset] =
    Arbitrary {
      for {
        p <- arbitrary[OffsetP]
        q <- arbitrary[OffsetQ]
      } yield Offset(p, q)
    }

  implicit val arbRA: Arbitrary[RightAscension] =
    Arbitrary(arbitrary[Angle].map(RightAscension.fromAngle))

  implicit val arbDec: Arbitrary[Declination] =
    Arbitrary(arbitrary[Angle].map(Declination.zero.offset(_)._1))

  implicit val arbRAVelocity: Arbitrary[RightAscensionAngularVelocity] =
    // Velocity can be any number but it makes sense to restrict to work with the legacy model
    Arbitrary(arbitrary[Double].map(v => RightAscensionAngularVelocity(AngularVelocity(v % AngularVelocity.MilliArcSecsInADegree))))

  implicit val arbDecVelocity: Arbitrary[DeclinationAngularVelocity] =
    // Velocity can be any number but it makes sense to restrict to work with the legacy model
    Arbitrary(arbitrary[Double].map(v => DeclinationAngularVelocity(AngularVelocity(v % AngularVelocity.MilliArcSecsInADegree))))

  implicit val arbCoords: Arbitrary[Coordinates] =
    Arbitrary {
      for {
        ra  <- arbitrary[RightAscension]
        dec <- arbitrary[Declination]
      } yield Coordinates(ra, dec)
    }

  implicit val angleChooser: Gen.Choose[Angle] = new Choose[Angle] {
    override def choose(min: Angle, max: Angle): Gen[Angle] =
      Gen.choose(min.toDegrees, max.toDegrees).map(Angle.fromDegrees)
  }

  // A way of picking coordinates within an offset distance in arcseconds from a given coordinate.
  def genCoordsWithinDistance(c: Coordinates, distLower: Angle, distUpper: Angle): Gen[Coordinates] =
    for {
      bearing  <- arbitrary[Angle]
      distance <- choose(distLower, distUpper)
    } yield c.angularOffset(bearing, distance)


  implicit val arbMagnitudeBand: Arbitrary[MagnitudeBand] =
    Arbitrary(oneOf(MagnitudeBand.all))

  implicit val arbMagnitudeSystem: Arbitrary[MagnitudeSystem] =
    Arbitrary(oneOf(MagnitudeSystem.allForOT))

  implicit val arbEpoch: Arbitrary[Epoch] =
    Arbitrary {
      for {
        d <- arbitrary[Short].map(_ / 100.0)
      } yield Epoch(d)
    }

  implicit val arbProperMotion: Arbitrary[ProperMotion] =
    Arbitrary {
      for {
        deltaRA  <- arbitrary[RightAscensionAngularVelocity]
        deltaDec <- arbitrary[DeclinationAngularVelocity]
        epoch    <- arbitrary[Epoch]
      } yield ProperMotion(deltaRA, deltaDec, epoch)
    }

  implicit val arbRedshift: Arbitrary[Redshift] =
    Arbitrary(Gen.choose[Double](-1, 50.0).suchThat(_ > -1).map(v => Redshift(v))) // Redshift must be more than -1 and usually never goes above 20

  implicit val arbParallax: Arbitrary[Parallax] =
    Arbitrary(arbitrary[Double].map(Parallax.apply))

  implicit val arbMagnitude: Arbitrary[Magnitude] =
    Arbitrary {
      for {
        value  <- arbitrary[Short].map(_ / 100.0)
        band   <- arbitrary[MagnitudeBand]
        error  <- arbitrary[Option[Double]]
        system <- arbitrary[MagnitudeSystem]
      } yield Magnitude(value, band, error, system)
    }

  implicit val arbDistribution: Arbitrary[SpectralDistribution] = Arbitrary[SpectralDistribution] {
    Gen.oneOf(
      BlackBody(8000),
      BlackBody(10000),
      PowerLaw(0),
      PowerLaw(1),
      EmissionLine(450.nm, 150.kps, 13.ergsPerSecondPerSquareCentimeter, 22.wattsPerSquareMeterPerMicron),
      EmissionLine(550.nm, 400.kps, 23.wattsPerSquareMeter, 42.ergsPerSecondPerSquareCentimeterPerAngstrom),
      LibraryStar.A0V,
      LibraryStar.A5III,
      LibraryNonStar.NGC2023,
      LibraryNonStar.GammaDra
    )
  }
  implicit val arbProfile: Arbitrary[SpatialProfile] = Arbitrary[SpatialProfile] {
    Gen.oneOf(
      PointSource,
      UniformSource,
      GaussianSource(0.5),
      GaussianSource(0.75)
    )
  }

  implicit val arbTooTarget: Arbitrary[TooTarget] =
    Arbitrary(arbitrary[String].map(TooTarget(_)))

  implicit val arbHorizonsDesignation: Arbitrary[HorizonsDesignation] =
    Arbitrary {
      for {
        obj <- arbitrary[Int]
        des <- oneOf(
          HorizonsDesignation.AsteroidNewStyle(obj.toString),
          HorizonsDesignation.AsteroidOldStyle(obj),
          HorizonsDesignation.Comet(obj.toString),
          HorizonsDesignation.MajorBody(obj)
        )
      } yield des
    }

  implicit val arbSiderealTarget: Arbitrary[SiderealTarget] =
    Arbitrary {
      for {
          name           <- arbitrary[String]
          coordinates    <- arbitrary[Coordinates]
          properMotion   <- arbitrary[Option[ProperMotion]]
          redshift       <- arbitrary[Option[Redshift]]
          parallax       <- arbitrary[Option[Parallax]]
          magnitudes     <- arbitrary[List[Magnitude]]
          spectralDistr  <- arbitrary[Option[SpectralDistribution]]
          spatialProfile <- arbitrary[Option[SpatialProfile]]
      } yield SiderealTarget(name, coordinates, properMotion, redshift, parallax, magnitudes, spectralDistr, spatialProfile)
    }

  implicit val arbEphemeris: Arbitrary[Ephemeris] =
    Arbitrary {
      for {
        site <- arbitrary[Site]
        data <- arbitrary[List[(Long, Coordinates)]]
      } yield Ephemeris(site, ==>>.fromList(data))
    }

  implicit val arbNonSiderealTarget: Arbitrary[NonSiderealTarget] =
    Arbitrary {
      for {
         name           <- arbitrary[String]
         ephemeris      <- arbitrary[Ephemeris]
         horizonsDes    <- arbitrary[Option[HorizonsDesignation]]
         magnitudes     <- arbitrary[List[Magnitude]]
         spectralDistr  <- arbitrary[Option[SpectralDistribution]]
         spatialProfile <- arbitrary[Option[SpatialProfile]]
      } yield NonSiderealTarget(name, ephemeris, horizonsDes, magnitudes, spectralDistr, spatialProfile)
    }

  implicit val arbTarget: Arbitrary[Target] =
    Arbitrary(oneOf(
      arbitrary[TooTarget],
      arbitrary[SiderealTarget],
      arbitrary[NonSiderealTarget]))

  implicit val arbWavelength: Arbitrary[Wavelength] =
    Arbitrary(arbitrary[Short].map(n => Math.abs(n).nm))

  implicit def arbMap[K: Arbitrary: Order, V: Arbitrary]: Arbitrary[K ==>> V] =
    Arbitrary(arbitrary[List[(K, V)]].map(==>>.fromList(_)))

  implicit val arbVersionToken: Arbitrary[VersionToken] =
    Arbitrary(nonEmptyListOf(posNum[Int]).map {
      case i :: is => VersionToken.unsafeFromIntegers(i, is: _*)
      case _       => sys.error("generated empty list!?")
    })
}

// Arbitrary but reasonable dates and times.
trait ArbTime {

  implicit val arbZoneId: Arbitrary[ZoneId] =
    Arbitrary {
      oneOf(ZoneId.getAvailableZoneIds.asScala.toSeq).map(ZoneId.of)
    }

  implicit val arbYear: Arbitrary[Year] =
    Arbitrary {
      choose(2000, 2020).map(Year.of)
    }

  implicit val arbLocalDate: Arbitrary[LocalDate] =
    Arbitrary {
      for {
        y <- arbitrary[Year]
        d <- choose(1, y.length)
      } yield LocalDate.ofYearDay(y.getValue, d)
    }

  implicit val arbLocalTime: Arbitrary[LocalTime] =
    Arbitrary {
      for {
        h <- choose(0, 23)
        m <- choose(0, 59)
        s <- choose(0, 59)
        n <- choose(0, 999999999)
      } yield LocalTime.of(h, m, s, n)
    }

  implicit val arbLocalDateTime: Arbitrary[LocalDateTime] =
    Arbitrary {
      for {
        d <- arbitrary[LocalDate]
        t <- arbitrary[LocalTime]
      } yield LocalDateTime.of(d, t)
    }

  implicit val cogLocalDate: Cogen[LocalDate] =
    Cogen[(Int, Int)].contramap(d => (d.getYear, d.getDayOfYear))
}

object ArbTime extends ArbTime
