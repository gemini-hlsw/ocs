package edu.gemini.model.p1.submit

import edu.gemini.model.p1.immutable._

import scala.actors.Actor._
import java.util.logging.Logger

object SubmitClient {

  lazy val production = new SubmitClient("Production", productionSubmissionUrls)
  lazy val test       = new SubmitClient("Test",       testSubmissionUrls)

}

/**
 * Proposal submission client.  Performs a post of the proposal XML and the
 * PDF attachment to the specified URL and returns a result.  Upon success,
 * the result contains the partner reference number that was assigned by the
 * backend service.
 */
class SubmitClient(name:String,  url: Map[SubmitDestination, String]) {
  private val LOG = Logger.getLogger(getClass.getName)

  /**
   * Removes the proposal key and all responses and then does a normal
   * submit.
   */
  def resubmit(proposal: Proposal)(callback: ProposalSubmitResult => Unit) {
    asyncSubmit(proposal, SubContainer(proposal).reset, callback)
  }

  def submit(proposal: Proposal)(callback: ProposalSubmitResult => Unit) {
    asyncSubmit(proposal, SubContainer(proposal), callback)
  }

  // Do the submissions in a separate thread, calling the callback with the
  // results.
  private def asyncSubmit(proposal: Proposal, sc: SubContainer, callback: ProposalSubmitResult => Unit) {
    val scWithKey       = sc.withKey
    val proposalWithKey = scWithKey.update(proposal)
    actor { callback(syncSubmit(proposalWithKey, scWithKey)) }
  }

  private def logValidation(proposal: Proposal) {
    ProposalIo.validate(proposal) match {
      case Right(_)  => LOG.fine("Validated proposal '%s'.".format(proposal.title))
      case Left(msg) => LOG.warning("Proposal does not validate: " + msg)
    }
  }

  private def syncSubmit(proposal: Proposal, sc: SubContainer): ProposalSubmitResult = {
    logValidation(proposal)
    val futs    = sc.pendingDestinations map { d => FutureSubmission(d, url(d), proposal) }
    val results = futs.map(_.result)
    ProposalSubmitResult((sc/:results)(_+_).update(proposal), results)
  }

  override def toString = s"${getClass.getSimpleName}($name, $url)\n"

}