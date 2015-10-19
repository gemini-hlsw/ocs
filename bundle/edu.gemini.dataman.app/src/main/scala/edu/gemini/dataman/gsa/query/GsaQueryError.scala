package edu.gemini.dataman.gsa.query

import java.io.IOException

/** Types of errors that may occur while processing a GSA query. */
sealed trait GsaQueryError

object GsaQueryError {
  /** GSA server returned an invalid response.. */
  case class InvalidResponse(msg: String)         extends GsaQueryError

  /** GSA server rejected the query request for some reason. */
  case class RequestError(code: Int, msg: String) extends GsaQueryError

  /** An IO exception while contacting the GSA server. */
  case class IoException(ex: IOException)         extends GsaQueryError

  /** Some random unexpected exception. */
  case class Unexpected(t: Throwable)             extends GsaQueryError

  def exception(e: GsaQueryError): Option[Throwable] = e match {
    case IoException(ex) => Some(ex)
    case Unexpected(t)   => Some(t)
    case _               => None
  }

  def explain(e: GsaQueryError): String = {
    val explanation = e match {
      case InvalidResponse(m)    => s"GSA server returned invalid results: $m."
      case RequestError(code, m) => s"GSA server rejected the request (http code=$code, message=$m)."
      case IoException(_)        => s"A network issue prevented executing the query."
      case Unexpected(_)         => s"Unexpected exception while working with the GSA server."
    }

    val exMessage = for {
      t <- GsaQueryError.exception(e)
      m <- Option(t.getMessage) if m.trim != ""
    } yield m

    exMessage.fold(explanation)(em => s"$explanation ($em)")
  }
}