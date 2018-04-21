package edu.gemini.ictd.dao

import edu.gemini.ictd._
import edu.gemini.pot.sp.Instrument
import edu.gemini.pot.sp.Instrument._
import edu.gemini.spModel.core.{ProgramId, Site}
import edu.gemini.spModel.ictd.Availability

import doobie.imports._

import scala.collection.immutable.TreeMap

import scalaz._
import Scalaz._
import scalaz.effect.IO

/** DAO for reading custom mask availability data. */
object CustomMaskDao {

  /** MOS table entries.  Note that the table splits up the program id in such a
    * way that only ProgramId.Science ids are permitted.
    */
  final case class Mos(
    pid:        ProgramId.Science,
    name:       String,
    location:   Location
  ) {

    /** The corresponding CustomMaskKey.  The MOS table stores the full custom
      * mask ODF file name, but in the Science Program model, we keep just the
      * mask name itself without the _ODF.fits suffix.
      */
    def toKey: CustomMaskKey =
      CustomMaskKey(pid, name.stripSuffix("_ODF.fits").stripSuffix(".fits"))

    def toMapEntry: (CustomMaskKey, Availability) =
      (toKey, location.availability)

  }

  def select(s: Site): ConnectionIO[TreeMap[CustomMaskKey, Availability]] =
    Statements.select(s).list.map { ms =>
      TreeMap(ms.map(_.toMapEntry): _*)
    }

  object Statements {

    import ProgramIdComposite._
    import SemesterComposite._

    import LocationMeta._
    import SiteMeta._

    def select(s: Site): Query0[Mos] = {

      val instrument: Location =
        Location.Instrument

      val summitCabinet: Location =
        if (s == Site.GN) Location.MkoCabinet else Location.CpoCabinet

      sql"""
        SELECT m.Site,
               m.Year,
               m.Semester,
               m.ProgramType,
               m.ProgramNo,
               m.ODF,
               c.Location
          FROM MOS m
               LEFT OUTER JOIN Component c
                 ON c.ComponentID = m.ComponentID
         WHERE m.Site = $s
           AND (c.Location = $instrument OR c.Location = $summitCabinet)
      """.query[Mos]

    }

  }

}
