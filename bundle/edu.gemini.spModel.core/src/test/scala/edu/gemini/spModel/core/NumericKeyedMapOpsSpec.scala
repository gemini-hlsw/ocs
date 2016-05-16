package edu.gemini.spModel.core

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz.{ ==>>, IList }
import scalaz.std.anyVal._
import scalaz.syntax.foldable._

class NumericKeyedMapOpsSpec extends Specification with ScalaCheck with Arbitraries {
  val N = implicitly[Numeric[Byte]]

  "lookupClosest" should {
    "find the closest key" ! forAll { ((m: Byte ==>> Angle), k: Byte) =>
      val closest = IList.fromList(m.keys).reverse.minimumBy(k0 => N.abs(N.minus(k, k0)))
      closest.flatMap(m.lookupAssoc(_)) must_== m.lookupClosestAssoc(k)
    }
  }

}
