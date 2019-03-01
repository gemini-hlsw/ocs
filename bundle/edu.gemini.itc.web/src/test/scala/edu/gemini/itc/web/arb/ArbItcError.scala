package edu.gemini.itc.web.arb

import edu.gemini.itc.shared._
import org.scalacheck._
import org.scalacheck.Arbitrary._
import scalaz.\/

trait ArbItcError {

  val genItcError: Gen[ItcError] =
    arbitrary[String].map(ItcError)

  implicit val arbItcError: Arbitrary[ItcError] =
    Arbitrary(genItcError)

}

object itcerror extends ArbItcError