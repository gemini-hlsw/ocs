package edu.gemini.spModel.dataset

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification


class DatasetMd5Spec extends Specification with ScalaCheck {
  "DatasetMd5" should {
    "work with any byte array and round-trip to/from String correctly" in {
      forAll { (a: Array[Byte]) =>
        val md0 = new DatasetMd5(a)
        DatasetMd5.parse(md0.hexString).exists { md1 =>
          md0 == md1 && md0.hashCode == md1.hashCode
        }
      }
    }

    "fail cleanly for invalid input" in {
      // the hex binary spec parses pairs of hex digits into bytes
      DatasetMd5.parse("abc") must_== None
    }
  }
}
