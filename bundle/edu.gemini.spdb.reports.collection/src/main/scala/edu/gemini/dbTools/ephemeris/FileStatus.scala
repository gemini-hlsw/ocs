package edu.gemini.dbTools.ephemeris

import scalaz._, Scalaz._

/** Ephemeris file status before an update. */
sealed trait FileStatus

object FileStatus {
  /** FileStatus indicating that the corresponding ephemeris file is out of
    * date or contains insufficient coverage for the night in general.
    */
  case object Expired extends FileStatus

  /** FileStatus indicating that the ephemeris file corresponds to an
    * observation that is no longer of interest (for example because it has
    * been deleted or observed).
    */
  case object Extra extends FileStatus

  /** FileStatus indicating that the ephemeris file is missing.
    */
  case object Missing extends FileStatus

  /** FileStatus indicating that the ephemeris file is up-to-date for the
    * night.
    */
  case object UpToDate extends FileStatus

  implicit val OrderFileStatus: Order[FileStatus] =
    Order.orderBy(_.toString)
}