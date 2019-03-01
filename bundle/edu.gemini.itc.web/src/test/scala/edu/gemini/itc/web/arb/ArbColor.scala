package edu.gemini.itc.web.arb

import java.awt.Color
import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ArbColor {

  val genComponent: Gen[Int] =
    Gen.chooseNum(0, 255)

  val genColor: Gen[Color] =
    for {
      r <- genComponent
      g <- genComponent
      b <- genComponent
      a <- genComponent
    } yield new Color(r, g, b, a)

  implicit val arbColor: Arbitrary[Color] =
    Arbitrary(genColor)

}

object color extends ArbColor