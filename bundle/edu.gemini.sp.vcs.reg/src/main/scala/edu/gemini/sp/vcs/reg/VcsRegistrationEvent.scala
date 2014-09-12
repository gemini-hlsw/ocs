package edu.gemini.sp.vcs.reg

import edu.gemini.spModel.core.{Peer, SPProgramID}

case class VcsRegistrationEvent(pid: SPProgramID, peer: Option[Peer])