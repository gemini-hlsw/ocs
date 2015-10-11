package edu.gemini.dataman.gsa.query

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.skycalc.ObservingNight
import edu.gemini.spModel.core.{SPProgramID, Site}
import edu.gemini.spModel.dataset.DatasetLabel

import java.net.URL
import java.util.{Calendar, GregorianCalendar}

import argonaut._
import Argonaut._

import scalaz._
import Scalaz._

/** A synchronous GSA query that obtains listings of GSA file entries. */
sealed trait GsaFileQuery {

  /** `GsaFile`s corresponding to the given program id, if any. */
  def program(pid: SPProgramID): GsaResponse[List[GsaFile]]

  /** `GsaFile`s corresponding to the given observation id, if any. */
  def observation(oid: SPObservationID): GsaResponse[List[GsaFile]]

  /** The `GsaFile` corresponding to the given dataset label, if any. */
  def dataset(label: DatasetLabel): GsaResponse[Option[GsaFile]]

  /** All `GsaFile`s from the current observing night. */
  def tonight: GsaResponse[List[GsaFile]]

  /** All `GsaFile`s from the last 7 observing nights (including the current
    * observing night). */
  def thisWeek: GsaResponse[List[GsaFile]]
}

object GsaFileQuery {
  private def gsaSite(site: Site): String = site match {
    case Site.GN => "Gemini-North"
    case Site.GS => "Gemini-South"
  }

  def apply(host: String, site: Site): GsaFileQuery =
    new GsaFileQuery {
      val prefix = s"http://$host/jsonqastate/${gsaSite(site)}/present"

      def url(filter: String): URL = new URL(s"$prefix/$filter")

      override def program(pid: SPProgramID): GsaResponse[List[GsaFile]] =
        GsaQuery.get(url(pid.stringValue))

      override def observation(oid: SPObservationID): GsaResponse[List[GsaFile]] =
        GsaQuery.get(url(oid.stringValue))

      override def dataset(label: DatasetLabel): GsaResponse[Option[GsaFile]] =
        GsaQuery.get[List[GsaFile]](url(label.toString)).map(_.headOption)

      override def tonight: GsaResponse[List[GsaFile]] = {
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

      override def thisWeek: GsaResponse[List[GsaFile]] = {
        val endNight = new ObservingNight(site)
        val c = new GregorianCalendar(site.timezone()) <|
          (_.setTimeInMillis(endNight.getStartTime))   <|
          (_.add(Calendar.DAY_OF_YEAR, -6))
        val startNight = new ObservingNight(site, c.getTimeInMillis)

        GsaQuery.get(url(s"${startNight.getNightString}-${endNight.getNightString}"))
      }
    }
}
