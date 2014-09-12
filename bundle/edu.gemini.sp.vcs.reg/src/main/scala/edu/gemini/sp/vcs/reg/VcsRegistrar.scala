package edu.gemini.sp.vcs.reg

import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.core.Peer

/**
 * A service that provides persistent/mutable mapping between program ids and
 * VCS server locations.
 */
trait VcsRegistrar {
  def allRegistrations: Map[SPProgramID, Peer]
  def registration(id: SPProgramID): Option[Peer]
  def register(id: SPProgramID, loc: Peer): Unit
  def unregister(id: SPProgramID): Unit

  // For Java ...
  def registrationOrNull(id: SPProgramID): Peer = registration(id).orNull
}
