package edu.gemini.spModel.dataset

import edu.gemini.spModel.pio.Param
import edu.gemini.spModel.pio.codec.{ParseError, PioError, ParamCodec}

import scalaz._
import Scalaz._

/** Details the possible statuses in which a QA update request may find itself.
  */
sealed trait QaRequestStatus {
  import QaRequestStatus._

  def description: String = this match {
    case PendingPost    => "Awaiting send to FITS Server"
    case ProcessingPost => "Sending to FITS Server"
    case Accepted       => "Accepted by FITS Server"
    case Failed(msg)    => s"Error in FITS Server: $msg"
  }
}

object QaRequestStatus {

  /** A user has requested an update to the QA state but it has not yet been
    * sent to the GSA.  Our notion of the GSA state is presumed to be more or
    * less up-to-date.
    */
  case object PendingPost extends QaRequestStatus

  /** A request to update QA state is underway in the GSA server. The response
    * has not yet been returned.  The request is accepted or rejected
    * synchronously but the GSA dataset record is updated asynchronously.
    */
  case object ProcessingPost extends QaRequestStatus

  /** A QA state update request failed for some reason.  This state should
    * automatically transition as it will be retried.
    */
  final case class Failed(msg: String) extends QaRequestStatus

  /** A QA state update request was accepted but may not yet have been applied
    * to the GSA dataset record. Presumably at some point in the future the
    * GSA record will be updated accordingly and the state will transition to
    * `Idle`.  If not, the time is recorded to provide an indication of how
    * long we have been waiting for the change to be applied.
    */
  case object Accepted extends QaRequestStatus

  implicit val EqualQaRequestStatus: Equal[QaRequestStatus] = Equal.equalA

  implicit val ParamCodecQaRequestStatus =
    new ParamCodec[QaRequestStatus] {
      val failed = """Failed\((.*)\)""".r

      override def encode(key: String, a: QaRequestStatus): Param =
        ParamCodec[String].encode(key, a.toString)

      override def decode(p: Param): PioError \/ QaRequestStatus =
        ParamCodec[String].decode(p).flatMap {
          case "PendingPost"    => PendingPost.right
          case "ProcessingPost" => ProcessingPost.right
          case "Accepted"       => Accepted.right
          case failed(msg)      => Failed(msg).right
          case s                => ParseError(p.getName, s, "QaRequestStatus").left
        }
    }
}
