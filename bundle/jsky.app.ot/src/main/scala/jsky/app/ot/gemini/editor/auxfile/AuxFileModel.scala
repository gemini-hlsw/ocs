package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.auxfile.api.AuxFile
import edu.gemini.auxfile.client.AuxFileClient
import edu.gemini.sp.vcs.reg.VcsRegistrar
import edu.gemini.spModel.core.SPProgramID

import AuxFileState.{Busy, Idle}

import scala.swing.Publisher
import jsky.app.ot.OT


/**
 * The AuxFileModel manages/manipulates/publishes the current state of the
 * auxiliary file information in the Observing Tool.  It is not thread-safe and
 * is meant to be used solely from the Swing event loop.
 */
class AuxFileModel(reg: VcsRegistrar) extends Publisher {
  var currentState: Option[AuxFileState]      = None
  def currentPid: Option[SPProgramID]         = currentState.map(_.pid)
  def currentFiles: Option[List[AuxFile]]     = currentState.map(_.files)
  def currentSelection: Option[List[AuxFile]] = currentState.map(_.selection)

  def client: Option[AuxFileClient] =
    for {
      s <- currentState
      p <- reg.registration(s.pid)
    } yield new AuxFileClient(OT.getKeyChain, p.host, p.port)

  private def update(pid: SPProgramID, condition: AuxFileState => Boolean)(modification: AuxFileState => AuxFileState) {
    if (currentState.exists(s => s.pid == pid && condition(s))) {
      currentState = currentState.map(modification)
      publish(AuxFileStateEvent(currentState))
    }
  }

  def select(pid: SPProgramID, sel: List[AuxFile]) {
    update(pid, s => s.status == Idle && sel.forall(f => currentFiles.exists(_.contains(f)))) {
      _.copy(selection = sel)
    }
  }

  def init(pid: SPProgramID) {
    currentState = Some(AuxFileState(pid))
    publish(new AuxFileStateEvent(currentState))
  }

  def busy(pid: SPProgramID) {
    update(pid, _.status == Idle) { _.copy(status = Busy) }
  }

  def success(pid: SPProgramID, files: List[AuxFile]) {
    update(pid, _.status == Busy) { _.copy(status = Idle, files = files.sorted, selection = Nil) }
  }

  def failure(pid: SPProgramID) {
    update(pid, _ => true) { _.copy(status = Idle) }
  }
}
