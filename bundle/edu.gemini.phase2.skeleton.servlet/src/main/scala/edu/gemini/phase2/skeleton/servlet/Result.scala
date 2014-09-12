package edu.gemini.phase2.skeleton.servlet

import javax.servlet.http.HttpServletResponse._

/**
 * A type for skeleton servlet results.
 */
sealed trait Result {
  def code: Int

  def display: String

  def msg: Option[String] = None
}


/**
 * Defines the potential results from calling the skeleton servlet.
 */
object Result {
  case class Success(code: Int, display: String) extends Result
  case class Failure(code: Int, display: String, override val msg: Option[String]) extends Result

  object Success {
    /** A Success that indicates a new program was created in the database. */
    val CREATED  = Success(SC_CREATED, "created")
    val OK       = Success(SC_OK,      "ok")
  }

  object Failure {
    def apply(code: Int, display: String, msg: String): Failure =
      Failure(code, display, Some(msg))

    /**
     * A failure for a GET request for a program id that cannot be found.
     */
    def notFound: Failure = Failure(SC_NOT_FOUND, "not found", None)

    /**
     * Creates a Failure that indicates that the proposal wasn't posted to the
     * server, wasn't parseable, didn't have all the required information, etc.
     * See the <code>msg</code> field for more information.
     */
    def badRequest(msg: String): Failure =
      Failure(SC_BAD_REQUEST, "bad request", msg)

    /**
     * Creates a Failure that indicates an existing program was present but
     * could not be replaced because it has been edited.
     */
    def rejected: Failure =
      Failure(SC_CONFLICT, "rejected", None)

    /**
     * Creates a Failure that indicates that the update failed for some reason
     * such as a problem communicating with the ODB.  See the <code>msg</code>
     * field for more information.
     */
    def error(msg: String): Failure =
      Failure(SC_INTERNAL_SERVER_ERROR, "failed", msg)

    def error(ex: Exception): Failure =
      error(Option(ex.getMessage).getOrElse(""))
  }
}

