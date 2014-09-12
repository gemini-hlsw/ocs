package edu.gemini.model.p1.targetio

package object api {
  implicit class ExceptionPimp(val ex: Exception) extends AnyVal {
    def nonNullMessage = Option(ex.getMessage).getOrElse("")
  }
}