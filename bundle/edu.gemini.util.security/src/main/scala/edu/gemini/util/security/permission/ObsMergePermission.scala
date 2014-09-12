package edu.gemini.util.security.permission

import edu.gemini.pot.sp.ISPObservation
import edu.gemini.spModel.core.SPProgramID

import java.security.Permission

import scalaz._
import Scalaz._

case class ObsMergePermission(obs: Option[ISPObservation], update: Option[ISPObservation]) extends Permission(~(obs orElse update).map(_.getObservationID).map(_.toString)) {
  assert(obs.isDefined || update.isDefined)

  def getActions: String = ""
  def implies(permission: Permission) = permission == this

  def pid: Option[SPProgramID] = (obs orElse update).flatMap(o => Option(o.getProgram.getProgramID))
}
