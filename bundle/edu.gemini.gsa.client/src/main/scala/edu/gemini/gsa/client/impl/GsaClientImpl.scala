package edu.gemini.gsa.client.impl

import java.net.URL

import edu.gemini.gsa.client.api._
import edu.gemini.gsa.query.{GsaFileListQuery, GsaHost}
import edu.gemini.spModel.core.Site

/**
 * Implements the GsaClient trait to send queries to the GSA for each request.
 */
object GsaClientImpl extends GsaClient {
  override def query(params: GsaParams, timeout: Int = GSA_TIMEOUT): GsaResult = {
    params match {
      case s: GsaSiderealParams =>
        val gsaQuery = GsaFileListQuery(GsaHost.Archive("archive.gemini.edu"), Site.GN)
        GsaResult.Success(gsaQuery.url(s.coords, params.instrument.name), gsaQuery.files(s.coords, params.instrument.name).toList.flatten)
      case _                    => GsaResult.Error(new URL(""), "")
    }
  }
}