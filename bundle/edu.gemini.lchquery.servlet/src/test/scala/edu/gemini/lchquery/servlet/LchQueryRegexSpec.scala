package edu.gemini.lchquery.servlet

import jsky.util.StringUtil
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.scalacheck.Prop._
import org.specs2.mutable.Specification

import scala.util.Random
import scala.util.matching.Regex

import scalaz._
import Scalaz._

class LchQueryRegexSpec extends Specification with ScalaCheck {
  import LchQueryRegexSpec._

  "toRegEx and StringUtil.match" should {
    "always match exact strings the same" in {
      forAll (queryGen) { s => s.matchesAgree(s) should beTrue }
    }

    "always match case insensitively the same" in {
      forAll (queryGen) { s => s.matchesAgree(s.randomizeCase) should beTrue }
    }

    "always match everything in an OR list the same" in {
      forAll (Gen.nonEmptyListOf(queryGen)) {
        lst => {
          val expr = lst.mkString("|")
          lst.forall(s => expr.matchesAgree(s.randomizeCase)) should beTrue
        }
      }
    }

    "always match the ? wildcard the same" in {
      forAll (queryGenCanBeEmpty, queryGenCanBeEmpty, Gen.alphaNumChar, queryGenCanBeEmpty, queryGenCanBeEmpty) {
        (pre, prem, mid, postm, post) => {
          val expr = s"$pre$prem?$postm$post"
          val query = s"$prem$mid$postm"
          expr.matchesAgree(query.randomizeCase) should beTrue
        }
      }
    }

    "always match the * wildcard the same" in {
      forAll (queryGenCanBeEmpty, queryGenCanBeEmpty, queryGenCanBeEmpty, queryGenCanBeEmpty, queryGenCanBeEmpty) {
        (pre, prem, mid, postm, post) => {
          val expr = s"$pre$prem*$postm$post"
          val query = s"$prem$mid$postm"
          expr.matchesAgree(query.randomizeCase) should beTrue
        }
      }
    }
  }

  "toRegEx" should {
    "not match something not in an OR list" in {
      forAll (Gen.nonEmptyListOf(queryGen), queryGen) {
        (lst, other) => !lst.exists(_.matchesLCH(other)) ==> (lst.mkString("|").matchesLCH(other) should beFalse)
      }
    }
  }
}

object LchQueryRegexSpec {
  import ValueMatcher.ToRegex

  val random = Random
  val queryGenCanBeEmpty = Gen.listOf(Gen.alphaNumChar).map(_.mkString)
  val queryGen = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)

  implicit class RegexMatcher(val r: Regex) extends AnyVal {
    def matches(s: String): Boolean = {
      r.findFirstMatchIn(s).isDefined
    }
  }

  implicit class StringFuncs(val r: String) extends AnyVal {
    def matchesSU(s: String): Boolean =
      StringUtil.`match`(r, s)

    def matchesLCH(s: String): Boolean =
      r.toRegex.matches(s)

    def matchesAgree(s: String): Boolean =
      r.matchesLCH(s) == r.matchesSU(s)

    def randomizeCase: String = {
      def randomizeCharCase(c: Char): Char =
        if (Random.nextInt % 2 == 0) c.toLower else c.toUpper
      r.map(randomizeCharCase)
    }
  }
}