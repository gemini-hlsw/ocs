package edu.gemini.services.client

import java.net.URI

import edu.gemini.services.client.TelescopeSchedule.Constraint
import edu.gemini.spModel.core.Peer
import edu.gemini.util.security.auth.keychain.KeyChain
import edu.gemini.util.skycalc.calc.Interval
import edu.gemini.util.trpc.client.TrpcClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Client side implementation to get telescope schedules from given peer.
 */
object TelescopeScheduleClient {

  /** Gets a full telescope schedule from the given host. */
  def getSchedule(kc: Option[KeyChain], peer: Peer, timeFrame: Interval): Future[TelescopeSchedule] =
    TrpcClient(peer).withOptionalKeyChain(kc) future { r =>
      r[TelescopeScheduleService].getSchedule(timeFrame)
    }

  /** Gets the uri to displays the calendar in a browser. */
  def getScheduleUrl(kc: Option[KeyChain], peer: Peer): Future[URI] =
    TrpcClient(peer).withOptionalKeyChain(kc) future { r =>
      r[TelescopeScheduleService].getScheduleUrl
    }

  /** Adds a constraint. */
  def addConstraint(kc: Option[KeyChain], peer: Peer, constraint: Constraint): Future[Unit] =
    TrpcClient(peer).withOptionalKeyChain(kc) future { r =>
      r[TelescopeScheduleService].addConstraint(constraint)
    }

  /** Deletes a constraint. */
  def deleteConstraint(kc: Option[KeyChain], peer: Peer, constraint: Constraint): Future[Unit] =
    TrpcClient(peer).withOptionalKeyChain(kc) future { r =>
      r[TelescopeScheduleService].deleteConstraint(constraint)
    }

}
