package edu.gemini.spModel.ictd

import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

import scalaz._
import Scalaz._

trait Arbitraries {

  implicit val arbAvailability: Arbitrary[Availability] =
    Arbitrary(Gen.oneOf(Availability.values))

  // Scalaz arbitraries are in scalaz.scalacheck but i don't know how to get
  // access to them.

  implicit def arbIctdTracking: Arbitrary[IctdTracking] =
    Arbitrary(Gen.oneOf(
      arbitrary[(Availability, String, List[String])].map { case (a, s, ss) => \&/.Both(a, NonEmptyList(s, ss: _*)) },
      arbitrary[Availability].map(\&/.This(_)),
      arbitrary[(String, List[String])].map { case (s, ss) => \&/.That(NonEmptyList(s, ss: _*)) }
    ).map(IctdTracking(_)))

}
