package edu.gemini.lchquery.servlet

import scala.util.matching.Regex

import scalaz._
import Scalaz._

sealed class ValueMatcher[A](expression: String, extractor: A => String) {
  import ValueMatcher.ToRegex
  def matcher(x: A): Boolean = (for {
    r <- Option(expression).map(_.toRegex)
    v <- Option(x)
    m <- Option(extractor(v))
  } yield r.findFirstMatchIn(m).isDefined).getOrElse(false)
}

object ValueMatcher {
  private[servlet] implicit class ToRegex(val expression: String) extends AnyVal {
    def toRegex: Regex = ("^" +
      (expression.contains("|") ? s"($expression)" | expression).
        replaceAllLiterally("*", ".*").
        replaceAllLiterally("%", ".*").
        replaceAllLiterally("?", ".")
        + "$").r
  }
}
