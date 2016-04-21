package edu.gemini.dbTools.ephemeris

import edu.gemini.horizons.api._
import edu.gemini.horizons.server.backend.HorizonsService2
import edu.gemini.skycalc.Night
import edu.gemini.spModel.core._

import java.nio.file.Path
import java.time.{Duration, Instant}
import java.util.Date

import scalaz._
import Scalaz._


import TcsEphemerisExport.FilePartition

/** TCS ephemeris directory maintenance. */
sealed trait TcsEphemerisExport {

  /** Gets an action which will partition the given horizons ids into
    * categories: expired, extra, missing, and up-to-date.  Any existing
    * ephemeris file that is still of interest must have minimum coverage of
    * the night to be considered valid.  Namely, if there is a gap between
    * consecutive ephemeris elements of longer than two minutes for the night,
    * the file is considered "expired".
    *
    * @param hids horizons ids for all non-sidereal observations of interest
    *
    * @return categorization of horizons ids according to the state of the
    *         corresponding ephemeris file on disk
    */
  def partition(hids: ISet[HorizonsDesignation]): TryExport[FilePartition]

  /** Gets an action that will update the directory of ephemeris files as
    * necessary so that it contains an entry for each observation of interest
    * and removes all that are no longer of interest.
    *
    * @param hids horizons ids for all non-sidereal observations of interest
    *
    * @return map of horizons ids to either an error or the (possibly) updated
    *         file
    */
  def update(hids: ISet[HorizonsDesignation]): TryExport[HorizonsDesignation ==>> (ExportError \/ Path)]

  /** Gets an action that will write ephemeris files for each of the given
    * horizons ids. Note this action doesn't delete out-of-date files or skip
    * up-to-date files.  See `update` for that.
    *
    * @param hids horizons ids for all non-sidereal observations of interest
    *
    * @return map of horizons ids to either an error or the updated file
    */
  def write(hids: ISet[HorizonsDesignation]): TryExport[HorizonsDesignation ==>> (ExportError \/ Path)]
}


/** TCS ephemeris export cron job.
  */
object TcsEphemerisExport {

  // Maximum time between two consecutive elements in the ephemeris file.  If a
  // larger time gap is found, the file is fetched from horizons and updated.
  val MaxGap = Duration.ofMinutes(2)

  // We will request this many elements from horizons, though the actual number
  // provided may differ.  The TCS maximum is 1440, but horizons may return a
  // few more than requested.
  val ElementCount  = 1430

  sealed trait FileCategory {
    def hids: ISet[HorizonsDesignation]
  }

  /** Horizons ids for which the corresponding ephemeris file is out of date or
    * contains insufficient coverage for the night in general.
    */
  final case class Expired(hids: ISet[HorizonsDesignation]) extends FileCategory

  /** Horizons ids for ephemeris files corresponding to observations that are
    * no longer of interest (for example because they have been observed).
    */
  final case class Extra(hids: ISet[HorizonsDesignation]) extends FileCategory

  /** Horizons ids for missing ephemeris files.
    */
  final case class Missing(hids: ISet[HorizonsDesignation]) extends FileCategory

  /** Horizons ids that correspond to ephemeris files that do not need to be
    * updated because they have sufficient converage for the night already.
    */
  final case class UpToDate(hids: ISet[HorizonsDesignation]) extends FileCategory

  /** Categorization of horizons ids according to the disposition of the
    * corresponding ephemeris file in the directory.
    */
  final case class FilePartition(expired: Expired, extra: Extra, missing: Missing, upToDate: UpToDate)

  def apply(dir: Path, night: Night, site: Site): TcsEphemerisExport =
    new TcsEphemerisExport {

      import ExportError._

      val files  = EphemerisFiles(dir)
      val start  = Instant.ofEpochMilli(night.getStartTime)
      val end    = Instant.ofEpochMilli(night.getEndTime)

      def partition(hids: ISet[HorizonsDesignation]): TryExport[FilePartition] = {
        def upToDate(relevant: ISet[HorizonsDesignation]): TryExport[ISet[HorizonsDesignation]] = {
          // ISet[HorizonsDesignation] => TryExport[ List[(hid, ISet[Instant])] ]
          val allPairs = relevant.toList.traverseU { hid =>
            files.parseTimes(hid).strengthL(hid)
          }

          allPairs.map { lst =>
            val upToDatePairs = lst.filter { case (hid, times) =>
              val validTimes = times.filterGt(Some(start)).filterLt(Some(end)).insert(start).insert(end).toList
              validTimes.zip(validTimes.tail).forall {
                case (s, e) =>
                  val gap = Duration.ofMillis(e.toEpochMilli - s.toEpochMilli)
                  gap.compareTo(MaxGap) <= 0
                case _          =>
                  false
              }
            }

            ISet.fromList(upToDatePairs.unzip._1)
          }
        }

        for {
          all     <- files.list
          relevant = all.intersection(hids)
          up      <- upToDate(relevant)
          expired  = relevant.difference(up)
        } yield FilePartition(
                  Expired(expired),
                  Extra(all.difference(relevant)),
                  Missing(hids.difference(all)),
                  UpToDate(up))
      }

      def update(hids: ISet[HorizonsDesignation]): TryExport[HorizonsDesignation ==>> (ExportError \/ Path)] =
        for {
          part      <- partition(hids)
          _         <- files.deleteAll(part.extra.hids.union(part.expired.hids))
          upToDate   = ==>>.fromList(part.upToDate.hids.toList.fproduct(files.path(_).right[ExportError]))
          refreshed <- write(part.expired.hids.union(part.missing.hids))
        } yield upToDate.union(refreshed)

      def write(hids: ISet[HorizonsDesignation]): TryExport[HorizonsDesignation ==>> (ExportError \/ Path)] = {
        // HorizonsDesignation ==>> TryExport[File]
        val exportMap = ==>>.fromList(hids.toList.fproduct(writeOne))

        // IO[HorizonsDesignation ==>> (ExportError \/ Path)]
        val exportOp = exportMap.traverse(_.run)

        // TryExport[HorizonsDesignation ==> (ExportError \/ Path)]
        EitherT(exportOp.map(_.right[ExportError]))
      }

      def writeOne(hid: HorizonsDesignation): TryExport[Path] =
        for {
          em <- lookupEphemeris(hid)
          p  <- files.write(hid, em)
        } yield p

      def lookupEphemeris(hid: HorizonsDesignation): TryExport[EphemerisMap] =
        HorizonsService2.lookupEphemerisE[EphemerisElement](hid, site, new Date(start.toEpochMilli), new Date(end.toEpochMilli), ElementCount) {
          (ee: EphemerisEntry) => ee.coords.map((_, ee.getRATrack, ee.getDecTrack))
        }.leftMap(e => HorizonsError(hid, e): ExportError).map {
          _.mapKeys(Instant.ofEpochMilli)
        }
    }
}
