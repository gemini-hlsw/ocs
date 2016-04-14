package edu.gemini.dbTools.ephemeris

import edu.gemini.horizons.server.backend.HorizonsService2
import edu.gemini.spModel.core.HorizonsDesignation

import java.util.logging.{Level, Logger}

/** Errors that may occur while exporting ephemeris data. */
trait ExportError {
  import ExportError._

  /** Extracts a human readable explanation of the error and the associated
    * `Throwable`, if any.
    */
  def report: (String, Option[Throwable]) = this match {
    case OdbError(ex)           =>
      ("Error looking up nonsidereal observations in the database", Some(ex))

    case HorizonsError(hid, h2) =>
      h2 match {
        case HorizonsService2.HorizonsError(e)   =>
          (s"$hid: Error communicating with horizons service", Some(e))

        case HorizonsService2.ParseError(_, msg) =>
          (s"$hid: Could not parse response from horizons service: $msg", None)

        case HorizonsService2.EphemerisEmpty     =>
          (s"$hid: No response from horizons", None)
      }

    case FileError(msg, hid, ex)   =>
      val prefix = hid.fold("") { h => s"$h: " }
      (s"$prefix $msg", ex)
  }

  def log(logger: Logger, prefix: String = ""): Unit = {
    val (msg, ex) = report
    logger.log(Level.WARNING, s"$prefix$msg", ex.orNull)
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
