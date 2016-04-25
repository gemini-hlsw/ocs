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
import scalaz.effect.IO


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
  def partition(hids: ISet[HorizonsDesignation]): TryExport[HorizonsDesignation ==>> FileStatus]

  /** Gets an action that will update the directory of ephemeris files as
    * necessary so that it contains an entry for each observation of interest
    * and removes all that are no longer of interest.
    *
    * @param hids horizons ids for all non-sidereal observations of interest
    *
    * @return map of horizons ids to either an error or the (possibly) updated
    *         file
    */
  def update(hids: ISet[HorizonsDesignation]): TryExport[HorizonsDesignation ==>> (FileStatus, ExportError \/ Path)]

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

  // Single file result.  Status before, error from operation or file path
  type FileUpdate = (FileStatus, ExportError \/ Path)

  def apply(dir: Path, night: Night, site: Site): TcsEphemerisExport =
    new TcsEphemerisExport {

      import ExportError._

      val files  = EphemerisFiles(dir)
      val start  = Instant.ofEpochMilli(night.getStartTime)
      val end    = Instant.ofEpochMilli(night.getEndTime)

      def partition(hids: ISet[HorizonsDesignation]): TryExport[HorizonsDesignation ==>> FileStatus] = {

        def upToDate(relevant: ISet[HorizonsDesignation]): TryExport[ISet[HorizonsDesignation]] = {
          val allPairs: TryExport[List[(HorizonsDesignation, ISet[Instant])]] =
            relevant.toList.traverseU { hid =>
              files.parseTimes(hid).strengthL(hid)
            }

          allPairs.map { lst =>
            val upToDatePairs = lst.filter { case (hid, times) =>
              val validTimes = times.filterGt(Some(start)).filterLt(Some(end)).insert(start).insert(end).toList
              validTimes.zip(validTimes.tail).forall {
                case (s, e) => Duration.ofMillis(e.toEpochMilli - s.toEpochMilli).compareTo(MaxGap) <= 0
                case _      => false
              }
            }

            ISet.fromList(upToDatePairs.unzip._1)
          }
        }

        def statusMap(status: FileStatus, hids: ISet[HorizonsDesignation]): HorizonsDesignation ==>> FileStatus =
          ==>>.fromList(hids.toList.zip(Stream.continually(status)))

        for {
          all     <- files.list
          relevant = all.intersection(hids)
          up      <- upToDate(relevant)
        } yield statusMap(FileStatus.Expired,          relevant.difference(up)).
                  union(statusMap(FileStatus.Extra,    all.difference(relevant))).
                  union(statusMap(FileStatus.Missing,  hids.difference(all))).
                  union(statusMap(FileStatus.UpToDate, up))
      }


      def update(hids: ISet[HorizonsDesignation]): TryExport[HorizonsDesignation ==>> FileUpdate] = {

        // Map each file status to an action to take.
        def actions(m: HorizonsDesignation ==>> FileStatus): HorizonsDesignation ==>> (FileStatus, TryExport[Path]) =
          m.mapWithKey { case (hid, status) =>
            val act: TryExport[Path] = status match {
              case FileStatus.Expired  => writeOne(hid)
              case FileStatus.Extra    => files.delete(hid).as(files.path(hid))
              case FileStatus.Missing  => writeOne(hid)
              case FileStatus.UpToDate => TryExport(files.path(hid))
            }
            (status, act)
          }

        // Explicit sequencing because of the (FileStatus, TryExport[Path]) tuple.
        def seq(m: HorizonsDesignation ==>> (FileStatus, TryExport[Path])): TryExport[HorizonsDesignation ==>> FileUpdate] = {
          val mr: HorizonsDesignation ==>> (FileStatus, IO[ExportError \/ Path]) =
            m.map { _.map(_.run) }

          val empty = IO(==>>.empty[HorizonsDesignation, FileUpdate])
          val ms: IO[HorizonsDesignation ==>> FileUpdate] =
            mr.fold(empty) { case (hid, (fs, io), res) =>
              for {
                m0   <- res
                disj <- io
              } yield m0.insert(hid, (fs, disj))
            }

          EitherT.eitherT(ms.map(_.right[ExportError]))
        }

        for {
          part <- partition(hids)     // TryExport[HorizonsDesignation ==>> FileStatus]]
          acts <- seq(actions(part))  // TryExport[HorizonsDesignations ==>> (FileStatus, ExportError \/ Path)]
        } yield acts
      }


      def write(hids: ISet[HorizonsDesignation]): TryExport[HorizonsDesignation ==>> (ExportError \/ Path)] = {
        val exportMap: HorizonsDesignation ==>> TryExport[Path] =
          ==>>.fromList(hids.toList.fproduct(writeOne))

        val exportOp: IO[HorizonsDesignation ==>> (ExportError \/ Path)] =
          exportMap.traverse(_.run)

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
