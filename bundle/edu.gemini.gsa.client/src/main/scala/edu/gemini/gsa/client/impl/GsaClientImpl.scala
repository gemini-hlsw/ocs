package edu.gemini.gsa.client.impl

import java.net.URL

import edu.gemini.gsa.client.api._
import edu.gemini.gsa.query.GsaFileListQuery
import edu.gemini.spModel.core.Site

import scalaz._
import Scalaz._

/**
 * Implements the GsaClient trait to send queries to the GSA for each request.
 */
object GsaClientImpl extends GsaClient {
  val gsaQuery = GsaFileListQuery(GsaUrl.ROOT, Site.GN)

  override def query(params: GsaParams): GsaResult =
    params match {
      case s: GsaSiderealParams    =>
        gsaQuery.files(s.coords, params.instrument.name) match {
          case \/-(r) => GsaResult.Success(gsaQuery.url(s.coords, params.instrument.name), r)
          case -\/(e) => GsaResult.Error(gsaQuery.url(s.coords, params.instrument.name), e.explain)
        }
      case n: GsaNonSiderealParams =>
        gsaQuery.files(n.targetName, params.instrument.name) match {
          case \/-(r) => GsaResult.Success(gsaQuery.url(n.targetName, params.instrument.name), r)
          case -\/(e) => GsaResult.Error(gsaQuery.url(n.targetName, params.instrument.name), e.explain)
        }
    }
}