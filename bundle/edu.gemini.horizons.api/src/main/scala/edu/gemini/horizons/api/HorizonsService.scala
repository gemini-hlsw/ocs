package edu.gemini.horizons.api

import edu.gemini.spModel.core.Peer
import edu.gemini.util.trpc.client.TrpcClient
import edu.gemini.util.trpc.common.Try
import java.util.Date
import edu.gemini.util.skycalc.calc.Interval
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import edu.gemini.util.security.auth.keychain.KeyChain

/**
 * Client side service end point for Horizons queries.
 * Queries are executed on the ODB using the TRPC protocol.
 */
object HorizonsService {

  /** Creates and executes a horizons query with the given parameters. */
  def execute(kc: Option[KeyChain], peer: Peer, objectId: String, interval: Interval, stepWidth: Int = 5, stepUnits: HorizonsQuery.StepUnits = HorizonsQuery.StepUnits.TIME_MINUTES): Future[HorizonsReply] =
    execute(kc, peer, createQuery(peer, objectId, interval, stepWidth, stepUnits))

  /** Creates and executes a horizons query with the given parameters and waits for the result. */
  def executeAndWait(kc: Option[KeyChain], peer: Peer, objectId: String, interval: Interval, stepWidth: Int = 5, stepUnits: HorizonsQuery.StepUnits = HorizonsQuery.StepUnits.TIME_MINUTES): Try[HorizonsReply] =
    executeAndWait(kc, peer, createQuery(peer, objectId, interval, stepWidth, stepUnits))

  /** Executes a horizons query. */
  def execute(kc: Option[KeyChain], peer: Peer, query: HorizonsQuery): Future[HorizonsReply] =
    TrpcClient(peer).withOptionalKeyChain(kc).future( remote => remote[IQueryExecutor].execute(query))

  /** Executes a horizons query and waits for the result. */
  def executeAndWait(kc: Option[KeyChain], peer: Peer, query: HorizonsQuery): Try[HorizonsReply] =
    TrpcClient(peer).withOptionalKeyChain(kc) { remote => remote[IQueryExecutor].execute(query) }

  /** Creates a query for the given parameters. */
  private def createQuery(peer: Peer, objectId: String, interval: Interval, stepWidth: Int, stepUnits: HorizonsQuery.StepUnits): HorizonsQuery = {
    val query = new HorizonsQuery(peer.site)
    query.setStartDate(new Date(interval.start))
    query.setEndDate(new Date(interval.end))
    query.setObjectId(objectId)
    query.setSteps(stepWidth, stepUnits)
    query
  }
}
