package edu.gemini.util.security.permission

import scalaz._
import Scalaz._
import java.security.{BasicPermission, Permission}
import edu.gemini.spModel.core.SPProgramID

trait ProgramPermission { this : Permission => }

object ProgramPermission {

  /** Does the user have permission to read the specified program? */
  case class Read(id:SPProgramID) extends BasicPermission(id.toString, "read") with ProgramPermission

}