package edu.gemini.spModel.gemini.security

import org.junit.Test
import org.junit.Assert._

class UserRoleTest {

  @Test
  def checkKoreasUserRole {
    assertEquals(UserRole.NGO_KR, UserRole.getUserRoleByName("NGO/KR"))
    assertEquals(UserRole.NGO_KR, UserRole.getUserRoleByDisplayName("NGO-Korea"))
  }
}
