package jsky.app.ot.vcs

import edu.gemini.sp.vcs.{TrpcVcsServer, VcsServer}
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.spModel.core.{Peer, SPProgramID}
import edu.gemini.pot.sp.{ISPNode, ISPProgram}
import jsky.app.ot.OT

/**
 * Support for VCS GUI elements.  Provides access to the VcsServer to use for
 * particular program ids.  Publishes VcsGui.VcsRegistrationEvent as a Swing
 * Event.
 */
object VcsGui {
  private var reg: Option[VcsRegistrar] = None

  def registrar: Option[VcsRegistrar] = reg

  def registrar_=(vcsReg: Option[VcsRegistrar]) {
    reg = vcsReg
  }

  def peer(id: SPProgramID): Option[Peer] =
    for {
      r <- reg
      p <- r.registration(id)
    } yield p

  // For our slow-witted friend "Java"
  def peerOrNull(id: SPProgramID): Peer = peer(id).orNull

  /**
   * Get the VCS server to use with the given program id.  Checks the registry
   * (if set) and if found there returns the mapping.  Otherwise the default
   * host/port.
   */
  def server(id: SPProgramID): Option[VcsServer] = peer(id).map(TrpcVcsServer(OT.getKeyChain, _))

  case class VcsServerNotFound(title: String, msg: String)
  case class ProgramVcsServer(prog: ISPProgram, srv: VcsServer)

  private val NoProgram   = "Please check out a program from the database first."
  private val NoProgramId = "This program cannot be updated because it does not have a program ID.  Check your program out from a database."
  private val NoServer    = "This program cannot be updated because it was not checked out from a remote database."
  private val NotScience  = "You can only update Science Programs."

  /**
   * Gets the VCS server associated with the program in which the given node may
   * be found, or else information on why the server could not be determined.
   *
   * @param node program node whose corresponding server is sought
   *
   * @return program and server for the given node, or an error message
   */
  def server(node: ISPNode): Either[VcsServerNotFound, ProgramVcsServer] = node match {
    case null          => Left(VcsServerNotFound("No Program", NoProgram))
    case p: ISPProgram =>
      for {
        id  <- Option(p.getProgramID).toRight(VcsServerNotFound("No Program ID", NoProgramId)).right
        srv <- VcsGui.server(id).toRight(VcsServerNotFound("No Server", NoServer)).right
      } yield ProgramVcsServer(p, srv)
    case _             => Left(VcsServerNotFound("Not a Science Program", NotScience))
  }
}
