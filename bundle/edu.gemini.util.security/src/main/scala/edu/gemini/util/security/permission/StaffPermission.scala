package edu.gemini.util.security.permission

import java.security.Permission
import edu.gemini.spModel.core.SPProgramID

case class StaffPermission(id:Option[SPProgramID]) extends Permission(id.toString) {
  def this(id:SPProgramID) = this(Option(id))
  def this() = this(None)
  def getActions: String = ""
  def implies(permission: Permission) = permission == this
}


