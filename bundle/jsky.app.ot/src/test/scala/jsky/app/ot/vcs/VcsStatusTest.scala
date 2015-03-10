package jsky.app.ot.vcs

import edu.gemini.shared.util._

import org.junit.Test
import org.junit.Assert._

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.sp.vcs.ProgramStatus
import ProgramStatus.PendingSync
import edu.gemini.pot.sp.version.LifespanId


class VcsStatusTest {
  val id_cc8 = LifespanId.fromString("cc88b1dc-a913-4cbb-8c23-93cc0d2bd4e4")
  val id_94e = LifespanId.fromString("94efc2fa-db28-44a7-8d43-c9ee31ad8da9")

  val node_606f = new SPNodeKey("a757b0fc-800b-494d-9d50-4dacc15d606f")
  val node_e77d = new SPNodeKey("6c956839-c35e-4dd0-9abd-f48f1268e77d")
  val node_5c1f = new SPNodeKey("3d9234d9-2550-4164-a61f-486e58805c1f")

  @Test def textComp() {
    val m8115 = Map(
      node_5c1f -> VersionVector(id_cc8 -> new java.lang.Integer(1)),
      node_e77d -> VersionVector(id_cc8 -> new java.lang.Integer(2), id_94e -> new java.lang.Integer(6)),
      node_606f -> VersionVector(id_94e -> new java.lang.Integer(1))
    )
    val m8116 = Map(
      node_e77d -> VersionVector(id_94e -> new java.lang.Integer(6)),
      node_606f -> VersionVector(id_94e -> new java.lang.Integer(2))
    )

    assertEquals(PendingSync, ProgramStatus(m8116, m8115))
  }
}
