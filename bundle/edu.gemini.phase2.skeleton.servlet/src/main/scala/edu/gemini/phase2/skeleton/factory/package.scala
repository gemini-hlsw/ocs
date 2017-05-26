package edu.gemini.phase2.skeleton

import edu.gemini.model.p1.immutable.{Band, Observation, Proposal, TimeAmount}

import scalaz._
import Scalaz._

package object factory {
  // Filter to only the enabled observations by appropriate band.
  def enabledObs(proposal: Proposal): List[Observation] = {
    // If there is an itac acceptance, then use its band assignment.  Otherwise
    // just figure we will use the "normal" band 1/2 observations.
    val band =
      (for {
        itac   <- proposal.proposalClass.itac
        accept <- itac.decision.right.toOption
      } yield accept.band).getOrElse(1) match {
        case 3 => Band.BAND_3
        case _ => Band.BAND_1_2
      }
    proposal.observations.filter(obs => obs.band == band && obs.enabled)
  }



  // Used to calculate the ratio of program time to total time for a proposal for the accepted observations.
  def programTimeRatio(proposal: Proposal): Double = {
    val obs = enabledObs(proposal)

    def timeSum(extract: Observation => Option[TimeAmount]): TimeAmount =
      TimeAmount.sum(obs.map(extract).flatten)

    val programTime = timeSum(_.progTime)
    val partnerTime = timeSum(_.partTime)
    val totalTime = programTime |+| partnerTime
    totalTime.isEmpty ? 1.0 | (programTime.hours / totalTime.hours)
  }
}
