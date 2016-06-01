package edu.gemini.ags.client.api

import edu.gemini.model.p1.immutable.Observation
import java.net.URL

import scala.concurrent.{ExecutionContext, Future}

/**
 * Base trait for AGS client implementations.
 */
trait AgsClient {
  type Callback = AgsResult => Unit

  /**
   * Obtains the URL used to query the AGS server based on the given
   * observation.
   */
  def url(obs: Observation, time: Long): Option[URL]

  /**
   * Performs an asynchronous estimation request, providing the result to the
   * given callback function when available.
   */
  def estimate(obs: Observation, time: Long)(callback: Callback)(implicit ec: ExecutionContext) {
    Future.apply { callback(estimateNow(obs, time)) }
  }

  /**
   * Peforms a synchronous estimation request, waiting for and returning the
   * result to the caller.
   */
  def estimateNow(obs: Observation, time: Long): AgsResult
}
