package edu.gemini.gsa.query

import JsonCodecs._

import java.net.URL
import java.net.URLEncoder

import edu.gemini.spModel.core.{Coordinates, Site}
import java.nio.charset.StandardCharsets

/** A synchronous GSA query that obtains listings of GSA file entries. */
sealed trait GsaFileListQuery {

  /** `GsaFile`s corresponding to the given coordinates. */
  def files(c: Coordinates, instrumentName: String): GsaResponse[List[GsaFile]]

  /** Builds the URL to be searched for the given coordinates */
  def url(c: Coordinates, instrumentName: String): URL

  /** `GsaFile`s corresponding to the given name. */
  def files(n: String, instrumentName: String): GsaResponse[List[GsaFile]]

  /** Builds the URL to be searched for the given coordinates */
  def url(n: String, instrumentName: String): URL
}

object GsaFileListQuery {

  def apply(host: GsaHost, site: Site): GsaFileListQuery =
    new GsaFileListQuery {
      val siderealPrefix = s"${host.baseUrl}/jsonfilelist/notengineering/NotFail/OBJECT"
      val nonSiderealPrefix = s"${host.baseUrl}/jsonfilelist/summary"

      private def siderealUrl(filter: String, instrumentName: String): URL = new URL(s"$siderealPrefix/$instrumentName/$filter")
      private def nonSiderealUrl(filter: String, instrumentName: String): URL = new URL(s"$nonSiderealPrefix/$instrumentName/$filter")

      override def files(c: Coordinates, instrumentName: String): GsaResponse[List[GsaFile]] =
        GsaQuery.get(url(c, instrumentName))

      override def files(n: String, instrumentName: String): GsaResponse[List[GsaFile]] =
        GsaQuery.get(url(n, instrumentName))

      override def url(c: Coordinates, instrumentName: String): URL = siderealUrl(s"ra=${c.ra.toAngle.toDegrees}/dec=${c.dec.toDegrees}/sr=60", instrumentName)

      override def url(n: String, instrumentName: String): URL = nonSiderealUrl(s"object=${URLEncoder.encode(n, StandardCharsets.UTF_8.toString)}/sr=60", instrumentName)
    }
}
