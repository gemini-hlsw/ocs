package edu.gemini.gsa.query

import edu.gemini.gsa.query.JsonCodecs._
import edu.gemini.pot.sp.SPObservationID
import edu.gemini.skycalc.ObservingNight
import edu.gemini.spModel.core.{SPProgramID, Site}
import edu.gemini.spModel.dataset.DatasetLabel

import edu.gemini.gsa.core.GsaRecord

import java.net.URL
import java.util.{Calendar, GregorianCalendar}

import scalaz.Scalaz._

/** A synchronous GSA query that obtains listings of GSA file entries. */
sealed trait GsaRecordQuery {

  /** `GsaFile`s corresponding to the given program id, if any. */
  def program(pid: SPProgramID): GsaResponse[List[GsaRecord]]

  /** `GsaFile`s corresponding to the given observation id, if any. */
  def observation(oid: SPObservationID): GsaResponse[List[GsaRecord]]

  /** The `GsaFile` corresponding to the given dataset label, if any. */
  def dataset(label: DatasetLabel): GsaResponse[Option[GsaRecord]]

  /** All `GsaFile`s from the current observing night. */
  def tonight: GsaResponse[List[GsaRecord]]

  /** All `GsaFile`s from the last 7 observing nights (including the current
    * observing night). */
  def thisWeek: GsaResponse[List[GsaRecord]]
}

object GsaRecordQuery {
  private def gsaSite(site: Site): String = site match {
    case Site.GN => "Gemini-North"
    case Site.GS => "Gemini-South"
  }

  def apply(host: GsaHost, site: Site): GsaRecordQuery =
    new GsaRecordQuery {
      val prefix = s"${host.protocol}://${host.host}/jsonqastate/${gsaSite(site)}/present"

      def url(filter: String): URL = new URL(s"$prefix/RAW/$filter")

      override def program(pid: SPProgramID): GsaResponse[List[GsaRecord]] =
        GsaQuery.get(url(s"progid=${pid.stringValue}"))

      override def observation(oid: SPObservationID): GsaResponse[List[GsaRecord]] =
        GsaQuery.get(url(s"obsid=${oid.stringValue}"))

      override def dataset(label: DatasetLabel): GsaResponse[Option[GsaRecord]] =
        GsaQuery.get[List[GsaRecord]](url(label.toString)).map(_.headOption)

      override def tonight: GsaResponse[List[GsaRecord]] = {
        // Use the filename prefix option instead of the UT date option because
        // observing nights span UT nights in Chile but the filename prefix
        // for a single observing night doesn't change.
        val night = new ObservingNight(site)
        val sitePrefix = site match {
          case Site.GN => "N"
          case Site.GS => "S"
        }
        GsaQuery.get(url(s"$sitePrefix${night.getNightString}"))
      }

      override def thisWeek: GsaResponse[List[GsaRecord]] = {
        val endNight = new ObservingNight(site)
        val c = new GregorianCalendar(site.timezone()) <|
          (_.setTimeInMillis(endNight.getStartTime))   <|
          (_.add(Calendar.DAY_OF_YEAR, -6))
        val startNight = new ObservingNight(site, c.getTimeInMillis)

        GsaQuery.get(url(s"${startNight.getNightString}-${endNight.getNightString}"))
      }
    }
}
