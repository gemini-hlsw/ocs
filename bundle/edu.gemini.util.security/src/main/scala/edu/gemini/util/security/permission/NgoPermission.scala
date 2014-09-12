package edu.gemini.util.security.permission

import java.security.Permission
import edu.gemini.spModel.core.SPProgramID

case class NgoPermission(id:Option[SPProgramID]) extends Permission(id.toString) {
  def this(id:SPProgramID) = this(Option(id))
  def implies(permission: Permission) = permission == this
  def getActions: String = ""
}
