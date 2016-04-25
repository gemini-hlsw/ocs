package edu.gemini.dbTools.ephemeris

import edu.gemini.horizons.server.backend.HorizonsService2
import edu.gemini.spModel.core.HorizonsDesignation

/** Errors that may occur while exporting ephemeris data. */
trait ExportError {
  import ExportError._

  /** A user-oriented message describing the error. */
  def message: String = {
    val m = exception.flatMap(e => Option(e.getMessage)).getOrElse("")
    this match {
      case OdbError(e)                                           =>
        s"Error looking up nonsidereal observations in the database: $m"
      case HorizonsError(_, HorizonsService2.HorizonsError(e))   =>
        s"Error communicating with horizons service: $m"
      case HorizonsError(_, HorizonsService2.ParseError(_, msg)) =>
        s"Could not parse response from horizons service: $msg"
      case HorizonsError(_, HorizonsService2.EphemerisEmpty)     =>
        "No response from horizons"
      case FileError(msg, _, Some(e))                            =>
        s"$msg: $m"
      case FileError(msg, _, None)                               =>
        msg
    }
  }

  /** The exception corresponding to this error, if any. */
  def exception: Option[Throwable] =
    this match {
      case OdbError(e)                                         => Some(e)
      case HorizonsError(_, HorizonsService2.HorizonsError(e)) => Some(e)
      case FileError(_, _, e)                                  => e
      case _                                                   => None
    }
}

object ExportError {

  /** An error that happens when working with the ODB to extract the non-
    * sidereal observation references with their horizons ids.
    */
  case class OdbError(ex: Throwable) extends ExportError

  /** An error that happens while working with the horizons service itself. */
  case class HorizonsError(hid: HorizonsDesignation, e: HorizonsService2.HS2Error) extends ExportError

  /** Errors that occur while working with ephemeris files. */
  case class FileError(msg: String, hid: Option[HorizonsDesignation], ex: Option[Throwable]) extends ExportError
}
