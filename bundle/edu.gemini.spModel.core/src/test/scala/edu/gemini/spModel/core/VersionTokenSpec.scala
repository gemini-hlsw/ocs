package edu.gemini.spModel.core

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object VersionTokenSpec extends Specification with Arbitraries with ScalaCheck {

  "VersionToken" should {

    "correspond to the format" in
      forAll { (v: VersionToken) =>
        v.format.split('.').toList.map(_.toInt) == v.toNel.list.toList
      }

    "roundtrip format => parse" in
      forAll { (v: VersionToken) =>
        VersionToken.parse(v.format) == Some(v)
      }

    "rountrip toNel => fromNel" in
      forAll { (v: VersionToken) =>
        VersionToken.fromNel(v.toNel) == Some(v)
      }

    "order properly" in
      forAll { (v0: VersionToken, v1: VersionToken) =>

        def zip(a: VersionToken, b: VersionToken): List[(Int, Int)] =
          a.toNel.list.toList.zipAll(b.toNel.list.toList, 0, 0)

        def isLt(a: VersionToken, b: VersionToken): Boolean =
          zip(a, b).dropWhile {
            case (n0, n1) => n0 == n1
          }.headOption.exists {
            case (n0, n1) => n0 < n1
          }

        (scalaz.Order[VersionToken]).apply(v0, v1) match {
          case scalaz.Ordering.LT => isLt(v0, v1)
          case scalaz.Ordering.GT => isLt(v1, v0)
          case _                  => zip(v0, v1).forall { case (n0, n1) => n0 == n1 }
        }
      }

  }

}
