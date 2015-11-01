package edu.gemini.dataman.query

import edu.gemini.dataman.query.GsaQueryError._

import java.io.IOException

/** Types of errors that may occur while processing a GSA query. */
sealed trait GsaQueryError {
  def explain: String = {
    val explanation = this match {
      case InvalidResponse(m)    => s"GSA server returned invalid results: $m."
      case RequestError(code, m) => s"GSA server rejected the request (http code=$code, message=$m)."
      case IoException(_)        => s"A network issue prevented executing the query."
      case Unexpected(_)         => s"Unexpected exception while working with the GSA server."
    }

    val exMessage = for {
      t <- this.exception
      m <- Option(t.getMessage) if m.trim != ""
    } yield m

    exMessage.fold(explanation)(em => s"$explanation ($em)")
  }

  def exception: Option[Throwable] = this match {
    case IoException(ex) => Some(ex)
    case Unexpected(t)   => Some(t)
    case _               => None
  }
}

object GsaQueryError {
  /** GSA server returned an invalid response.. */
  final case class InvalidResponse(msg: String)         extends GsaQueryError

  /** GSA server rejected the query request for some reason. */
  final case class RequestError(code: Int, msg: String) extends GsaQueryError

  /** An IO exception while contacting the GSA server. */
  final case class IoException(ex: IOException)         extends GsaQueryError

  /** Some random unexpected exception. */
  final case class Unexpected(t: Throwable)             extends GsaQueryError
}