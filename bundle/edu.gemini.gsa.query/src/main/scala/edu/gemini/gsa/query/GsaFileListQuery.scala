package edu.gemini.gsa.query

import JsonCodecs._

import java.net.URL

import edu.gemini.spModel.core.{Coordinates, Site}

/** A synchronous GSA query that obtains listings of GSA file entries. */
sealed trait GsaFileListQuery {

  /** `GsaFile`s corresponding to the given coordinates. */
  def files(c: Coordinates, instrumentName: String): GsaResponse[List[GsaFile]]

  /** Builds the URL to be searched for the given coordinates */
  def url(c: Coordinates, instrumentName: String): URL
}

object GsaFileListQuery {

  def apply(host: GsaHost, site: Site): GsaFileListQuery =
    new GsaFileListQuery {
      val prefix = s"${host.protocol}://${host.host}/jsonfilelist/notengineering/science/NotFail/OBJECT"

      private def url(filter: String, instrumentName: String): URL = new URL(s"$prefix/$instrumentName/$filter")

      override def files(c: Coordinates, instrumentName: String): GsaResponse[List[GsaFile]] =
        GsaQuery.get(url(c, instrumentName))

      override def url(c: Coordinates, instrumentName: String): URL = url(s"ra=${c.ra.toAngle.toDegrees}/dec=${c.dec.toDegrees}/sr=60", instrumentName)
    }
}
