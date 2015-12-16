package edu.gemini.gsa.query

import JsonCodecs._

import java.net.URL

import edu.gemini.spModel.core.{Coordinates, Site}

/** A synchronous GSA query that obtains listings of GSA file entries. */
sealed trait GsaFileListQuery {

  /** `GsaFile`s corresponding to the given coordinates. */
  def files(c: Coordinates): GsaResponse[List[GsaFile]]
}

object GsaFileListQuery {

  def apply(host: GsaHost, site: Site, instrumentName: String): GsaFileListQuery =
    new GsaFileListQuery {
      val prefix = s"${host.protocol}://${host.host}/jsonfilelist/notengineering/science/NotFail/OBJECT/$instrumentName"

      def url(filter: String): URL = new URL(s"$prefix/$filter")

      override def files(c: Coordinates): GsaResponse[List[GsaFile]] =
        GsaQuery.get(url(s"ra=${c.ra.toAngle.toDegrees}/dec=${c.dec.toDegrees}/sr=60"))
    }
}
