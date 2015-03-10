package jsky.app.ot.plugin

import edu.gemini.pot.sp.{ISPObservation, SPObservationID, ISPProgram}
import edu.gemini.sp.vcs.OldVcsFailure
import edu.gemini.spModel.core.SPProgramID

import scalaz._

trait OtViewerService {
  def registerView(view: AnyRef)
  def unregisterView(view: AnyRef)
  def load(pid: SPProgramID): \/[OldVcsFailure, ISPProgram]
  def load(oid: SPObservationID): \/[OldVcsFailure, Option[ISPObservation]]
  def loadAndView(pid: SPProgramID): \/[OldVcsFailure, ISPProgram]
  def loadAndView(oid: SPObservationID): \/[OldVcsFailure, Option[ISPObservation]]
}
