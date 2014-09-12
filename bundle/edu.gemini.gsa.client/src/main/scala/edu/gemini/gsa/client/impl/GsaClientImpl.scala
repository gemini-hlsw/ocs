package edu.gemini.gsa.client.impl

import edu.gemini.gsa.client.api._
import java.io.IOException
import java.net.URL
import edu.gemini.gsa.client.api.GsaResult._

/**
 * Implements the GsaClient trait to send queries to the GSA for each request.
 */
object GsaClientImpl extends GsaClient {
  def query(params: GsaParams, timeout: Int = GSA_TIMEOUT): GsaResult =
    query(GsaUrl(params), timeout) match {
      case Left(f)  => f
      case Right(s) => s
    }

  private def query(url: URL, timeout: Int): Either[Failure, Success] =
    for {
      m <- scrape(url, timeout).right
      t <- GsaTable.fromMap(m).left.map(msg => Error(url, msg)).right
    } yield Success(url, validDatasets(t))

  // Scrapes the page information into a GsaMap, if possible.
  private def scrape(url: URL, timeout: Int): Either[Failure, GsaMap] =
    try {
      GsaPageScraper.scrape(url, timeout).left.map(msg => Error(url, msg))
    } catch {
      case ex: IOException => Left(Offline(url))
      case ex: Exception   => Left(Other(url, ex))
    }

  // Any bad table rows are ignored.  The service sometimes returns bad data
  // for some cells, which we interpret as ignorable.
  private def validDatasets(t: GsaTable): List[GsaDataset] =
    for { Right(d) <- t.datasets } yield d
}