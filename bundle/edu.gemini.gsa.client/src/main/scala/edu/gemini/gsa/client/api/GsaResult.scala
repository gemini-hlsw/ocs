package edu.gemini.gsa.client.api

import java.net.URL

import edu.gemini.gsa.query.GsaFile

/**
 * Base trait for the results of a GSA dataset search.
 */
sealed trait GsaResult {

  /**
   * The exact URL that was queried to obtain this result.
   */
  def url: URL
}

/**
 * Defines all the possible results of performing a GSA query.
 */
object GsaResult {

  /**
   * Indicates a successful query (whether or not any datasets were found).
   */
  case class Success(url: URL, datasets: List[GsaFile]) extends GsaResult

  /**
   * A trait for all failure cases.
   */
  sealed trait Failure extends GsaResult

  /**
   * Indicates an IOException was encountered trying to communicate with the
   * remote server.
   */
  case class Offline(url: URL) extends Failure

  /**
   * Indicates that the server returned a result that we didn't expect and
   * couldn't parse.
   */
  case class Error(url: URL, msg: String) extends Failure

  /**
   * Indicates an unknown and unexpected problem happened while attempting the
   * query.
   */
  case class Other(url: URL, ex: Exception) extends Failure
}