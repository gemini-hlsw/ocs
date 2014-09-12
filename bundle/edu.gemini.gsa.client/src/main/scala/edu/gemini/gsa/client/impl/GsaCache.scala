package edu.gemini.gsa.client.impl

import edu.gemini.gsa.client.api.{GsaResult, GsaParams}
import edu.gemini.gsa.client.api.GsaResult.Success
import edu.gemini.model.p1.immutable.{Proposal, Observation}

object GsaCache {
  val empty = new GsaCache(Map.empty)

  def apply(p: Proposal): GsaCache = empty.update(p.observations)
}

/**
 * Provides support for working with a list of observations.
 */
case class GsaCache(entries: Map[GsaParams, GsaResult]) {

  /**
   * Lookup the GsaResult associated with the given Observation if previously
   * defined via the update method.
   */
  def get(obs: Observation): Option[GsaResult] =
    GsaParams.get(obs) flatMap { p => entries.get(p) }

  /**
   * Updates the cache to contain a result for each complete observation in the
   * given observation list.
   */
  def update(lst: List[Observation]): GsaCache = {
    val all = params(lst)
    removeFailures.filter(all).updateMissing(all)
  }

  private[impl] def updateMissing(all: Set[GsaParams]): GsaCache =
    copy(entries ++ all.filterNot(p => entries.contains(p)).par.map { p =>
      p -> GsaClientImpl.query(p)
    })

  /**
   * Returns a new GsaCache which only contains entries corresponding to
   * observations in the given list.
   */
  def filter(lst: List[Observation]): GsaCache = filter(params(lst))

  private def filter(all: Set[GsaParams]): GsaCache =
    copy(entries = entries.filter {
      case (p, _) => all.contains(p)
      case _      => false
    })

  /**
   * Returns a new GsaCache which only contains entries corresponding to
   * successful results.
   */
  def removeFailures: GsaCache =
    copy(entries = entries.filter {
      case (_, _: Success) => true
      case _               => false
    })

  /**
   * Gets all the GsaParams for observations in the given list.
   */
  def params(lst: List[Observation]): Set[GsaParams] =
    (for {
      obs <- lst
      p   <- GsaParams.get(obs)
    } yield p).toSet
}