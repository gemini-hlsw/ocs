package edu.gemini.sp.vcs

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.shared.util.VersionVector

import org.junit.Test
import org.junit.Assert._

import edu.gemini.pot.sp.version._

/**
 *
 */
class TestVersionMap {
  val key1 = new SPNodeKey()
  val key2 = new SPNodeKey()
  val id1  = LifespanId.random
  val id2  = LifespanId.random

  val emptyVv = VersionVector.javaInt[LifespanId]()

  private def versionMap(entries: (SPNodeKey, VersionVector[LifespanId, java.lang.Integer])*): VersionMap = entries.toMap

  private def lteq(jvm1: VersionMap, jvm2: VersionMap) {
    assertTrue(VersionMap.ordering.tryCompare(jvm1, jvm2).exists(_ < 0))
    assertTrue(VersionMap.ordering.lteq(jvm1, jvm2))
    assertTrue(VersionMap.ordering.gteq(jvm2, jvm1))
    assertFalse(VersionMap.ordering.gteq(jvm1, jvm2))
  }

  @Test def noConflicts() {
    val vv1 = emptyVv.updated(id1 -> 1)
    val vv2 = emptyVv.updated(id1 -> 2)
    val vv3 = emptyVv.updated(id1 -> 3)

    val jvm1 = versionMap(key1 -> vv1)
    val jvm2 = versionMap(key1 -> vv2)

    // vv1 < vv2
    assertTrue(vv1.tryCompareTo(vv2).exists( _ < 0))
    lteq(jvm1, jvm2)

    // Still less than when we add a new UUID to the node's version vector
    val vm2a = jvm2.updated(key1, vv2.updated(id2 -> 3))
    lteq(jvm1, vm2a)

     // Still less than when we add a new node not in jvm1
    val vm2b = vm2a.updated(key2, vv3)
    lteq(jvm1, vm2b)
  }

  @Test def oneNodeConflict() {
    val vv1 = emptyVv.updated(id1 -> 1).updated(id2 -> 2)
    val vv2 = emptyVv.updated(id1 -> 2).updated(id2 -> 1)
    assertTrue(vv1.tryCompareTo(vv2).isEmpty)

    val jvm1 = versionMap(key1 -> vv1)
    val jvm2 = versionMap(key1 -> vv2)
    assertTrue(VersionMap.ordering.tryCompare(jvm1, jvm2).isEmpty)
    assertFalse(VersionMap.ordering.lteq(jvm1, jvm2))
    assertFalse(VersionMap.ordering.gteq(jvm1, jvm2))
  }

  @Test def twoNodeConflict() {
    val vv1 = emptyVv.updated(id1 -> 1)
    val vv2 = emptyVv.updated(id1 -> 2)
    assertTrue(vv1.tryCompareTo(vv2).exists( _ < 0))

    val vv3 = emptyVv.updated(id1 -> 2)
    val vv4 = emptyVv.updated(id1 -> 1)
    assertTrue(vv4.tryCompareTo(vv3).exists( _ < 0))

    val jvm1 = versionMap(key1 -> vv1, key2 -> vv3)
    val jvm2 = versionMap(key1 -> vv2, key2 -> vv4)
    assertTrue(VersionMap.ordering.tryCompare(jvm1, jvm2).isEmpty)
    assertFalse(VersionMap.ordering.lteq(jvm1, jvm2))
    assertFalse(VersionMap.ordering.gteq(jvm1, jvm2))
  }
}
