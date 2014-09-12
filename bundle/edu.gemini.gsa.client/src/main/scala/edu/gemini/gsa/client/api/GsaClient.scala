package edu.gemini.gsa.client.api

/**
 * Provides support for querying the GSA for datasets.
 */
trait GsaClient {
  /**
   * Query the GSA for datasets that match the provided parameters, timing out
   * if there is no response within the given number of milliseconds.
   */
  def query(params: GsaParams, timeoutMs: Int): GsaResult
}