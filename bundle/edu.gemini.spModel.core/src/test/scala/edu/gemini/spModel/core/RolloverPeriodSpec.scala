// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.core

import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object RolloverPeriodSpec extends Specification with ScalaCheck with Arbitraries with Helpers {

  "RolloverPeriod" should {

    "support serialization" !
      forAll { (p: RolloverPeriod) =>
        canSerialize(p)
      }

    "always have contiguous semesters" !
      forAll { (p: RolloverPeriod) =>
        p.semesters.toList.zip(p.semesters.tail).forall { case (a, b) =>
          a.next == b
        }
      }

    "have semester count according to start semester" !
      forAll { (p: RolloverPeriod) =>
        val expected = if (p.startSemester.compareTo(RolloverPeriod.s2018B) < 0) 3 else 2
        p.semesters.size == expected
      }
  }
}
