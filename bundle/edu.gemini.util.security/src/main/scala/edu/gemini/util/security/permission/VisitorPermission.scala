package edu.gemini.util.security.permission

import java.security.Permission
import scalaz._
import Scalaz._
import edu.gemini.spModel.core.SPProgramID

case class VisitorPermission(id: Option[SPProgramID]) extends Permission(~id.map(_.toString)) {
  def this(id:SPProgramID) = this(Option(id))
  def this() = this(None)
  def getActions: String = ""
  def implies(permission: Permission) = permission == this
}
