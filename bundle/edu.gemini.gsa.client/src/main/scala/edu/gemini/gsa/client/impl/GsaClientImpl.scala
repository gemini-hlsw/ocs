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
      case GsaSiderealParams(coords, instrument)        =>
        gsaQuery.files(coords, instrument.name) match {
          case \/-(r) => GsaResult.Success(gsaQuery.url(coords, instrument.name), r)
          case -\/(e) => GsaResult.Error(gsaQuery.url(coords, instrument.name), e.explain)
        }
      case GsaNonSiderealParams(targetName, instrument) =>
        gsaQuery.files(targetName, instrument.name) match {
          case \/-(r) => GsaResult.Success(gsaQuery.url(targetName, instrument.name), r)
          case -\/(e) => GsaResult.Error(gsaQuery.url(targetName, instrument.name), e.explain)
        }
      case GsaUnsupportedParams                         =>
        GsaResult.Success(new URL(s"http://${GsaUrl.ROOT.host}"), Nil)
    }
}