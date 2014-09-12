package jsky.app.ot.gemini.editor.auxfile

import edu.gemini.auxfile.api.AuxFile
import edu.gemini.spModel.core.SPProgramID

import scala.swing.event.Event

object AuxFileState {
  sealed trait ActionStatus
  case object Busy extends ActionStatus
  case object Idle extends ActionStatus

  def apply(pid: SPProgramID): AuxFileState = AuxFileState(pid, Idle, Nil, Nil)
}

import AuxFileState.ActionStatus

case class AuxFileState(pid: SPProgramID, status: ActionStatus, files: List[AuxFile], selection: List[AuxFile])

case class AuxFileStateEvent(s: Option[AuxFileState]) extends Event