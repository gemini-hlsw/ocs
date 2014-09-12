package edu.gemini.pot.sp

import edu.gemini.shared.util.VersionVector

import java.nio.ByteBuffer
import java.util.UUID
import java.util.zip.{Adler32, Checksum}

import scala.collection.JavaConverters._

/**
 *
 */
package object version {
  type NodeVersions = VersionVector[LifespanId, java.lang.Integer]
  val EmptyNodeVersions: NodeVersions = VersionVector.javaInt[LifespanId]()

  type VersionMap = Map[SPNodeKey, NodeVersions]
  val EmptyVersionMap: VersionMap = Map.empty

  def nodeVersions(vm: VersionMap, k: SPNodeKey): NodeVersions = vm.getOrElse(k, EmptyNodeVersions)

  private def children(n: ISPNode): List[ISPNode] = n match {
    case c: ISPContainerNode => c.getChildren.asScala.toList
    case _ => Nil
  }

  /**
   * Gets a VersionMap containing entries for the tree rooted at n.
   */
  def subVersionMap(n: ISPNode): VersionMap = {
    val all = n.getProgram.getVersions

    def subMap(vm: VersionMap, n: ISPNode): VersionMap = {
      val k = n.getNodeKey
      ((vm + (k -> all(k)))/:children(n)) { subMap }
    }
    subMap(EmptyVersionMap, n)
  }

  private def putNodeVersions(buf: ByteBuffer, key: SPNodeKey, nv: NodeVersions): Unit = {
    def putUuid(u: UUID): Unit = {
      buf.putLong(u.getMostSignificantBits)
      buf.putLong(u.getLeastSignificantBits)
    }

    putUuid(key.uuid)
    val clocks    = nv.clocks
    val clockKeys = clocks.keys.toArray.sortBy(_.uuid)
    clockKeys.foreach { clockKey =>
      putUuid(clockKey.uuid)
      buf.putInt(clocks(clockKey))
    }
  }

  /**
   * Calculates a checksum / hash for versions of nodes rooted at 'n'. The
   * intention is to use this to determine whether anything in the subtree
   * rooted at this node changed since the last time the checksum was
   * calculated.
   */
  def nodeChecksum(n: ISPNode): Long = {
    def go(check: Checksum, rem: List[ISPNode]): Long = rem match {
      case Nil      => check.getValue
      case (h :: t) =>
        val key = h.getNodeKey
        val vv  = h.getProgram.getVersions(key)

        // SPNodeKey (UUID) + size * (LifespanId (UUID) -> Int)
        val size = 16 + vv.clocks.size * (16 + 4)
        val bb = ByteBuffer.allocate(size)
        putNodeVersions(bb, key, vv)
        check.update(bb.array(), 0, size)
        go(check, children(h) ++ t)
    }

    go(new Adler32, List(n))
  }

  def vmChecksum(vm: VersionMap): Long = {
    val keys = vm.keys.toArray.sorted
    val size = (0/:keys) { (size, k) => size + 16 + vm(k).clocks.size * 20 }
    val buf  = ByteBuffer.allocate(size)

    keys.foreach { key => putNodeVersions(buf, key, vm(key)) }

    val check = new Adler32()
    check.update(buf.array())
    check.getValue
  }
}
