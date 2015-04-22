package edu.gemini.itc.shared

import edu.gemini.spModel.core.Peer
import edu.gemini.util.trpc.client.TrpcClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.{Failure, Success, Validation}

/**
 * ITC calculation result for a single CCD.
 * Depending on the observation type (imaging vs. spectroscopy) and calculation method different results are returned.
 */
sealed trait ItcCalcResult
final case class ItcImagingResult(source: SourceDefinition, singleSNRatio: Double, totalSNRatio: Double, peakPixelFlux: Double) extends ItcCalcResult
final case class ItcSpectroscopyResult(/*TODO*/) extends ItcCalcResult

/**
 * ITC results.
 * Note that GMOS ITC recipes provide a separate result for each of the three CCDs while all other instruments only
 * return one result (either because they don't have more than one CCD or because all different CCDs are assumed to
 * have the same characteristics).
 */
sealed trait ItcResult extends Serializable {
  /** Returns the results for all CCDs. */
  def ccds: Array[ItcCalcResult]

  /** Returns the result for the first (and in most cases only) CCD. */
  def ccd = ccds(0)
}

object ItcResult {

  import edu.gemini.itc.shared.ItcService._

  /** Creates an ITC result in case of an error. */
  def forException(e: Throwable): Result = Failure(List(e.getMessage))

  /** Creates an ITC result with a single problem/error message. */
  def forMessage(msg: String): Result = Failure(List(msg))

  /** Creates an ITC result with a list of problem/error messages. */
  def forMessages(messages: List[String]): Result = Failure(messages)

  /** Creates an ITC result for a single CCD. */
  def forCcd(result: ItcCalcResult): Result = Success(new ItcResult { val ccds = Array(result) })

  /** Creates an ITC result for an array of CCDs. */
  def forCcds(results: Array[ItcCalcResult]): Result = Success(new ItcResult { val ccds = results })
}

/**
 * Service interface for ITC calculations.
 */
trait ItcService {

  import edu.gemini.itc.shared.ItcService._

  def calculate(source: SourceDefinition, obs: ObservationDetails, cond: ObservingConditions, tele: TelescopeDetails, ins: InstrumentDetails): Result

}

object ItcService {

  type Result = Validation[List[String], ItcResult]

  /** Performs an ITC call on the given host. */
  def calculate(peer: Peer, source: SourceDefinition, obs: ObservationDetails, cond: ObservingConditions, tele: TelescopeDetails, ins: InstrumentDetails): Future[Result] =
    TrpcClient(peer).withoutKeys future { r =>
      r[ItcService].calculate(source, obs, cond, tele, ins)
    }

}