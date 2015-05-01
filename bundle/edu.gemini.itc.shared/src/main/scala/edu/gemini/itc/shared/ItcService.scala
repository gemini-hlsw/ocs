package edu.gemini.itc.shared

import edu.gemini.spModel.core.Peer
import edu.gemini.util.trpc.client.TrpcClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.Color
import scalaz.{Failure, Success, Validation}

/**
 * ITC calculation result for a single CCD.
 * Depending on the observation type (imaging vs. spectroscopy) and calculation method different results are returned.
 */
sealed trait ItcResult extends Serializable

// === IMAGING RESULT
final case class ImgData(singleSNRatio: Double, totalSNRatio: Double, peakPixelFlux: Double)
final case class ItcImagingResult(source: SourceDefinition, ccds: Seq[ImgData]) extends ItcResult {
  def ccd(i: Int) = ccds(i % ccds.length)
}

// === SPECTROSCOPY RESULT
final case class SpcData(label: String, color: Color, data: Array[Array[Double]])
final case class SpcDataSet(label: String, title: String, xAxisLabel: String, yAxisLabel: String, series: Seq[SpcData])
final case class SpcDataFile(label: String, file: String)
final case class ItcSpectroscopyResult(source: SourceDefinition, dataSets: Seq[SpcDataSet], dataFiles: Seq[SpcDataFile]) extends ItcResult

object ItcResult {

  import edu.gemini.itc.shared.ItcService._

  /** Creates an ITC result in case of an error. */
  def forException(e: Throwable): Result = Failure(List(e.getMessage))

  /** Creates an ITC result with a single problem/error message. */
  def forMessage(msg: String): Result = Failure(List(msg))

  /** Creates an ITC result with a list of problem/error messages. */
  def forMessages(messages: List[String]): Result = Failure(messages)

  /** Creates an ITC result for a result. */
  def forResult(result: ItcResult): Result = Success(result)

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