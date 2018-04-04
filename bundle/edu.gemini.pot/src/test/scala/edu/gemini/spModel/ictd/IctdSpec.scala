package edu.gemini.spModel.ictd

import org.scalacheck.{ Properties, Gen, Arbitrary }
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._

object IctdSpec extends Specification with ScalaCheck with Arbitraries {

  "Availability monoid" should {
    "have a valid left identity" in {
      forAll { (a: Availability) =>
        (Availability.Zero |+| a) === a
      }
    }

    "have a valid right identity" in {
      forAll { (a: Availability) =>
        (a |+| Availability.Zero) === a
      }
    }

    "be associative" in {
      forAll { (a: Availability, b: Availability, c: Availability) =>
        ((a |+| b) |+| c) === (a |+| (b |+| c))
      }
    }

    // Not necessary to be a valid Monoid, but we need it to be true.
    "be commutative" in {
      forAll { (a: Availability, b: Availability) =>
        (a |+| b) === (b |+| a)
      }
    }
  }

  val empty: Map[String, Availability] = Map.empty

  def genLookup(t: IctdTracking): Map[String, Availability] =
    t.toThese.map(_.foldLeft(empty){ (m, s) =>
      arbitrary[Availability].sample.fold(m)(m.updated(s, _))
    }).getOrElse(empty)

  def genLookups(ts: List[IctdTracking]): Map[String, Availability] =
    ts.foldLeft(empty) { (m, t) => m ++ genLookup(t) }

  "IctdTracking" should {
    "have a valid left identity" in {
      forAll { (a: IctdTracking) =>
        (IctdTracking.zero |+| a) === a
      }
    }

    "have a valid right identity" in {
      forAll { (a: IctdTracking) =>
        (a |+| IctdTracking.zero) === a
      }
    }

    "be associative" in {
      forAll { (a: IctdTracking, b: IctdTracking, c: IctdTracking) =>
        ((a |+| b) |+| c) === (a |+| (b |+| c))
      }
    }

    "resolve(sum) == sum(resolve)" in {
      forAll { (ts: List[IctdTracking]) =>
        val lu = genLookups(ts).lift
        ts.suml.resolve(lu) === ts.foldMap(_.resolve(lu))
      }
    }

    "missing key always results in Missing availability" in {
      forAll { (t: IctdTracking) =>
        // If there is nothing to lookup, then it will resolve to whatever the
        // untracked constant is.  Otherwise we'll remove a lookup value and
        // it should be Missing.
        val a = t.toThese match {
          case \&/.This(a) => a
          case _           => Availability.Missing
        }
        t.resolve(genLookup(t).drop(1).lift) === a
      }
    }

  }

}
