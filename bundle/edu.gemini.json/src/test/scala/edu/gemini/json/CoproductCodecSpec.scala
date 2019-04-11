package edu.gemini.json

import argonaut._, Argonaut._
import org.scalacheck.{ Gen, Arbitrary }
import org.scalacheck.Arbitrary.arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.specs2.scalacheck.ScalaCheckParameters

object CoproductCodecSpec extends Specification with ScalaCheck {
  import coproduct._

  sealed trait Thing
  case class One(n: Int) extends Thing
  case class Two(b: Boolean, s: Short) extends Thing

  implicit val arbThing: Arbitrary[Thing] =
    Arbitrary(Gen.oneOf(
      arbitrary[Int].map(One),
      arbitrary[(Boolean, Short)].map((Two.apply _).tupled)
    ))

  val codecOne: CodecJson[One] = casecodec1(One.apply, One.unapply)("n")
  val codecTwo: CodecJson[Two] = casecodec2(Two.apply, Two.unapply)("b", "s")

  implicit val codecThing: CodecJson[Thing] =
    CoproductCodec[Thing]
      .withCase("One", codecOne) { case a: One => a }
      .withCase("Two", codecTwo) { case a: Two => a}
      .asCodecJson

  "CoproductCodec" should {
    "be invertible" ! prop { (t: Thing) =>
      Parse.decodeOption[Thing](t.asJson.spaces2) should_== Some(t)
    }
  }

}
