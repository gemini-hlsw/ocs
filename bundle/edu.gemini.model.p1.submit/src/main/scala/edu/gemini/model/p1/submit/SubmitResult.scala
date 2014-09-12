package edu.gemini.model.p1.submit

import edu.gemini.model.p1.immutable.Proposal

/**
 * ProposalSubmitResult contains a new proposal updated with the submission
 * key and any results from the individual remote servers.
 */
case class ProposalSubmitResult(proposal: Proposal, results: List[DestinationSubmitResult] = Nil)

/**
 * DestinationSubmitResult combines a single destination and the outcome of the
 * submission to that destination.
 */
case class DestinationSubmitResult(destination: SubmitDestination, result: SubmitResult)


/**
 * A trait for results of submitting a proposal to a single backend server.
 */
sealed trait SubmitResult {
  def isSuccess = false
}

object SubmitResult {

  /**
   * A successful submission includes the partner reference assigned to the
   * proposal, the server timestamp when it was received, a contact and any
   * additional information that the submission service wants to pass back.
   */
  case class Success(partnerRef: String, timestamp: Long, contact: String, msg: String) extends SubmitResult {
    override def isSuccess = true
  }

  /**
   * A failed submission of any kind contains a message.
   */
  sealed trait Failure extends SubmitResult {
    def message: String
    def destination: Option[SubmitDestination]
  }

  /**
   * When we encounter an IO exception while trying to submit a proposal, we
   * assume the service is offline.
   */
  case class Offline(destination: Option[SubmitDestination] = None) extends Failure {
    def message = "Sorry, the submission service appears to be offline or unreachable."
  }

  /**
   * SubmitClientError identifies problems on the client end of the submission
   * such as a missing or unreadable PDF attachment.
   */
  case class ClientError(message: String, destination: Option[SubmitDestination] = None) extends Failure

  /**
   * A SubmitServiceError identifies errors returned by the remote service.
   * This includes an error code (of questionable value) and a message.
   */
  case class ServiceError(destination: Option[SubmitDestination], code: Int, message: String) extends Failure

  /**
   * SubmitException is a catch-all for unexpected problems.
   */
  case class SubmitException(destination: Option[SubmitDestination], t: Throwable) extends Failure {
    def message = "Sorry, an unexpected error happened while submitting the proposal%s.".format(
      Option(t.getMessage).map(m => ": " + m).getOrElse(""))
  }
}



