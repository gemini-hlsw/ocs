package edu.gemini.shared.util

import org.junit.Test
import org.junit.Assert._
import java.util.UUID

/**
 *
 */

class VersionVectorTest {
  val d = VersionVector("Clinton" -> 1992, "Obama" -> 2008)
  val r = VersionVector("Reagan"  -> 1980, "Bush"  -> 1988)

  @Test def testDefault() {
    assertEquals(0, r("McCain"))
  }

  @Test def testJoin() {
    val r2 = VersionVector("Bush" -> 2000)
    assertEquals(VersionVector("Reagan" -> 1980, "Bush" -> 2000), r sync r2)

    val r3 = VersionVector("Romney" -> 2012)
    assertEquals(VersionVector("Reagan" -> 1980, "Bush" -> 1988, "Romney" -> 2012), r sync r3)
  }

  @Test def testOrdering() {
    val d2 = VersionVector("Clinton" -> 1992, "Obama" -> 2012)
    assertEquals(Some( 1), d2.tryCompareTo(d))
    assertEquals(Some(-1), d.tryCompareTo(d2))

    val d3 = VersionVector("Clinton" -> 1992, "Obama" -> 2008, "Henderson" -> 2016)
    assertEquals(Some(1), d3.tryCompareTo(d))

    val d4 = VersionVector("Clinton" -> 1992, "Carter" -> 1976)
    assertEquals(None, d4.tryCompareTo(d))
    assertEquals(None, d.tryCompareTo(d4))
  }

  val uuid_cc8 = UUID.fromString("cc88b1dc-a913-4cbb-8c23-93cc0d2bd4e4")
  val uuid_94e = UUID.fromString("94efc2fa-db28-44a7-8d43-c9ee31ad8da9")

  @Test def textX() {
    val v8115 = VersionVector(uuid_cc8 -> 2, uuid_94e -> 6)
    val v8116 = VersionVector(uuid_94e -> 6)

    assertEquals(Some( 1), v8115.tryCompareTo(v8116))
    assertEquals(Some(-1), v8116.tryCompareTo(v8115))
  }

  @Test def textY() {
    val v8115 = VersionVector(uuid_cc8 -> 1)
    val v8116 = VersionVector.empty[UUID, Int]

    assertEquals(Some( 1), v8115.tryCompareTo(v8116))
    assertEquals(Some(-1), v8116.tryCompareTo(v8115))
  }

  @Test def testZ() {
    val v8115 = VersionVector(uuid_94e -> 1)
    val v8116 = VersionVector(uuid_94e -> 2)

    assertEquals(Some(-1), v8115.tryCompareTo(v8116))
    assertEquals(Some( 1), v8116.tryCompareTo(v8115))
  }
}
