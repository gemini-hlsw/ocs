package edu.gemini.dbTools.ephemeris

import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.core.AlmostEqual
import edu.gemini.spModel.core.AlmostEqual._
import org.scalacheck.{Gen, Arbitrary}
import org.scalacheck.Arbitrary.arbitrary

import java.time.{LocalDateTime, Month, ZonedDateTime}
import java.time.ZoneOffset.UTC

import scalaz._, Scalaz._

trait Arbitraries extends edu.gemini.spModel.core.Arbitraries {
  implicit val arbEphemerisElement: Arbitrary[EphemerisElement] =
    Arbitrary {
      for {
        c <- arbitrary[Coordinates]
        r <- Gen.choose(-1000.0, 1000.0)
        d <- Gen.choose(-1000.0, 1000.0)
      } yield (c, r, d)
    }

  implicit val arbTime: Arbitrary[LocalDateTime] =
    Arbitrary {
      for {
        y <- Gen.choose(1000, 9999)
        m <- Gen.choose(1, 12).map(Month.of)
        d <- Gen.choose(1, 28)
        h <- Gen.choose(0, 23)
        n <- Gen.choose(0, 59)
      } yield LocalDateTime.of(y, m, d, h, n, 0)
    }

  implicit val arbEphemerisMap: Arbitrary[EphemerisMap] =
    Arbitrary {
      for {
        es <- arbitrary[List[EphemerisElement]]
        ts <- Gen.listOfN(es.size, arbitrary[LocalDateTime].map(ldt => ZonedDateTime.of(ldt, UTC).toInstant))
      } yield ==>>.fromList(ts.zip(es))
    }

  implicit val almostEqualEphemerisElement: AlmostEqual[EphemerisElement] =
    new AlmostEqual[(Coordinates, Double, Double)] {
      override def almostEqual(a: EphemerisElement, b: EphemerisElement): Boolean =
        (a._1 ~= b._1) && (a._2 ~= b._2) && (a._3 ~= b._3)
    }

  implicit val almostEqualEphemerisMap: AlmostEqual[EphemerisMap] =
    new AlmostEqual[EphemerisMap] {
      override def almostEqual(a: EphemerisMap, b: EphemerisMap): Boolean = {
        val aList = a.toList
        val bList = b.toList

        (aList.size == bList.size) && aList.zip(bList).forall {
          case ((aTime, aEph), (bTime, bEph)) =>
            (aTime === bTime) && (aEph ~= bEph)
        }
      }
    }

}
