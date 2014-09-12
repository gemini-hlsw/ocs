package edu.gemini.phase2.template.servlet

import javax.servlet.http.HttpServletResponse._

sealed case class Failure(code: Int, error: String)

object Failure {
  def badRequest(msg: String) = Failure(SC_BAD_REQUEST, msg)
  def error(msg: String)      = Failure(SC_INTERNAL_SERVER_ERROR, msg)
}