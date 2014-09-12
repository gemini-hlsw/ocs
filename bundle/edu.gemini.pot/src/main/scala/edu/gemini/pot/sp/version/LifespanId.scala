package edu.gemini.pot.sp.version

import java.util.UUID

/**
 * A wrapper to give a more specific type to a UUID.  This is used as a
 * VersionVector key to store a node's version information for the lifespan of a
 * particular program in a particular database.  If the program is deleted and
 * imported again from XML, it gets a new life span id.
 */
case class LifespanId(uuid: UUID) {
  override def toString: String = uuid.toString
}

object LifespanId {
  def random: LifespanId = LifespanId(UUID.randomUUID())
  def fromString(uuid: String): LifespanId = LifespanId(UUID.fromString(uuid))
}