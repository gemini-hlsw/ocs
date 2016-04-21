package edu.gemini.dbTools.ephemeris

import edu.gemini.skycalc.{Night, TwilightBoundedNight}
import edu.gemini.skycalc.TwilightBoundType.NAUTICAL
import edu.gemini.spModel.core.HorizonsDesignation.MajorBody
import edu.gemini.spModel.core.{Coordinates, HorizonsDesignation, Site}
import org.scalacheck.Prop

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import java.nio.file.{Path, Files}
import java.time.Instant
import java.util.logging.{Level, Logger}

import scala.collection.JavaConverters._
import scala.collection.immutable.NumericRange
import scalaz._, Scalaz._

object TcsEphemerisExportTest extends Specification with ScalaCheck with Arbitraries {
  val log   = Logger.getLogger(TcsEphemerisExport.getClass.getName)
  val site  = Site.GS
  val night = TwilightBoundedNight.forTime(NAUTICAL, Instant.now.toEpochMilli, site)

  import TcsEphemerisExport.{Expired, Extra, Missing, UpToDate, FilePartition}

  def hid(i: Int): HorizonsDesignation =
    MajorBody(i)

  def hidSet(ids: HorizonsDesignation*): ISet[HorizonsDesignation] =
    ISet.fromList(ids.toList)

  val hid0 = hid(0)
  val hid1 = hid(1)

  def delete(f: Path): Unit = {
    if (Files.isDirectory(f)) {
      Files.list(f).iterator.asScala.foreach(delete)
    }
    Files.delete(f)
  }

  def runTest(hids: ISet[HorizonsDesignation], ems: HorizonsDesignation ==>> EphemerisMap, expected: FilePartition): Boolean = {
    val dir = Files.createTempDirectory("TcsEphemeris")

    val ex  = TcsEphemerisExport(dir, night, site)
    val ef  = EphemerisFiles(dir)

    try {
      val action = for {
        _ <- ems.toList.traverseU { case (hid, em) => ef.write(hid, em) }
        p <- ex.partition(hids)
      } yield p

      action.run.unsafePerformIO() match {
        case -\/(err) =>
          val (msg, ex) = err.report
          log.log(Level.WARNING, msg, ex.orNull)
          err.log(log)
          throw ex.getOrElse(new RuntimeException())

        case \/-(actual) =>
          actual == expected
      }
    } finally {
      delete(dir)
    }
  }


  "TcsEphemerisExport" should {
    "partition ephemeris files" in {
      def ephemerisMap(n: Night, mins: Int): EphemerisMap =
        (==>>.empty[Instant, EphemerisElement]/:NumericRange.inclusive(n.getStartTime, n.getEndTime, mins * 60000l)) { (m, t) =>
          m.insert(Instant.ofEpochMilli(t), (Coordinates.zero, 0.0, 0.0))
        }

      val tonightMap   = ephemerisMap(night, 1)
      val yesterdayMap = ephemerisMap(night.previous(), 1)
      val sparseMap    = ephemerisMap(night, 30)

      def count(i: Int): Int = Math.abs(i % 5)

      Prop.forAll { (i0: Int, i1: Int, i2: Int, i3: Int) =>
        val ranges = List(i0, i1, i2, i3).scanLeft(0)((a,b) => a + count(b)).sliding(2)
        val hids   = ranges.map { case List(start, end) =>
                       (start until end).map(i => MajorBody(i): HorizonsDesignation).toList
                     }.toList

        val List(expired, extra, missing, upToDate) = hids

        // Create ephemeris maps corresponding to the various categories.
        val expiredMaps  = ==>>.fromList(expired.zipWithIndex.map { case (hid, i) => (hid, (i%2 == 0) ? yesterdayMap | sparseMap) })
        val extraMaps    = ==>>.fromList(extra.map    { hid => (hid, tonightMap) })
        val upToDateMaps = ==>>.fromList(upToDate.map { hid => (hid, tonightMap )})

        val expiredHids  = ISet.fromList(expired)
        val extraHids    = ISet.fromList(extra)
        val missingHids  = ISet.fromList(missing)
        val upToDateHids = ISet.fromList(upToDate)

        runTest(expiredHids.union(missingHids).union(upToDateHids),  // none of the "extra" ids
                expiredMaps.union(extraMaps).union(upToDateMaps),    // none of the "missing" ids
                FilePartition(Expired(expiredHids), Extra(extraHids), Missing(missingHids), UpToDate(upToDateHids)))
      }
    }
  }
}
