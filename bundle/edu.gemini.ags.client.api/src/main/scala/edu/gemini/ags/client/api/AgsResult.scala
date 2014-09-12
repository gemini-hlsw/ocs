package edu.gemini.ags.client.api

sealed trait AgsResult

sealed trait AgsFailure extends AgsResult

/**
 * The servlet received and processed the request but could not fulfill it
 * for some reason
 */
sealed trait ServletErrorResponse extends AgsFailure {
  /** HTTP response code. */
  def code: Int

  /** Response message. */
  def msg: String
}

object AgsResult {
  /** Successful AGS estimation. */
  case class Success(perc: Double) extends AgsResult {
    require((perc >= 0) && (perc <= 1), "Invalid percentage")
  }


  /** AGS service offline or unreachable. */
  case object Offline extends AgsFailure

  /**
   * Missing information in the Observation whose estimate is sought.  For example
   * if there is no target definition. This is a failure due to the client not
   * setting up the request properly.
   */
  case class Incomplete(msg: String) extends AgsFailure


  /**
   * The remote service is incompatible with this client.  This result is created
   * for HTTP client error responses (4xx codes).  This is assumed to be an issue
   * of version incompatibilities.
   */
  case class Incompatible(code: Int, msg: String) extends ServletErrorResponse

  /**
   * An unexpected problem in the AGS service.  This result is created for HTTP
   * server error responses (5xx codes).  This is either a bug in the remote
   * service or a problem reaching the catalog server.
   */
  case class ServiceError(code: Int, msg: String) extends ServletErrorResponse

  /** Unknown error. */
  case class Error(t: Throwable) extends AgsFailure

  def apply(res: String): AgsResult = {
    try {
      val d = res.toDouble
      if ((d < 0) || (d > 1))
        Incompatible(400, "Unexpected result: %f must be in the range 0..1")
      else
        Success(d)
    } catch {
      case ex: NumberFormatException => Incompatible(400, "Expected a number from 0 to 1, not '%s'".format(res))
    }
  }
}

