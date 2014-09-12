package jsky.app.ot.plugin

import edu.gemini.pot.sp.{ISPObservation, SPObservationID, ISPProgram}
import edu.gemini.sp.vcs.VcsFailure
import edu.gemini.spModel.core.SPProgramID

import scalaz._

trait OtViewerService {
  def registerView(view: AnyRef)
  def unregisterView(view: AnyRef)
  def load(pid: SPProgramID): \/[VcsFailure, ISPProgram]
  def load(oid: SPObservationID): \/[VcsFailure, Option[ISPObservation]]
  def loadAndView(pid: SPProgramID): \/[VcsFailure, ISPProgram]
  def loadAndView(oid: SPObservationID): \/[VcsFailure, Option[ISPObservation]]
}
