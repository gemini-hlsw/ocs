package edu.gemini.itc.web.arb

import java.awt.Color
import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ArbSmall{

  // Add some generators for small lists, to keep result generation from being quite so explosively
  // complex. This brings test execution down from hours to a few seconds but still generates
  // responses with fairly large JSON representations (up to 500k or so).
  implicit class GenOps(g: Gen.type) {

    def smallListOf[A](ga: Gen[A]): Gen[List[A]] =
      for {
        n  <- Gen.choose(0, 3)
        as <- Gen.listOfN(n, ga)
      } yield as

    def smallNonEmptyListOf[A](ga: Gen[A]): Gen[List[A]] =
      for {
        n  <- Gen.choose(1, 4)
        as <- Gen.listOfN(n, ga)
      } yield as

    }

}

object small extends ArbSmall