package edu.gemini.dataman.core

import scalaz._
import Scalaz._

/** Provides a value class wrapper for the GSA authentication cookie value.
  * When post requests are made to update the QA state, a cookie with this value
  * is required.
  */
final class GsaAuth(val value: String) extends AnyVal

object GsaAuth {
  implicit val EqualGsaAuth: Equal[GsaAuth] = Equal.equal { (a0, a1) =>
    a0.value === a1.value
  }
}
