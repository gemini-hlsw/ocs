package edu.gemini.spModel.gemini

import scalaz.Equal

/**
 *
 */
package object security {
  implicit val UserRolePrivilegesEqual: Equal[UserRolePrivileges] =
    Equal.equalA
}
