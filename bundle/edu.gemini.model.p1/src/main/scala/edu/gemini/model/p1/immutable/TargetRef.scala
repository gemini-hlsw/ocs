package edu.gemini.model.p1.immutable

import java.util.UUID

object TargetRef {
  def apply(i:Target):TargetRef = apply(i.uuid)
  val empty = apply(UUID.randomUUID)
}

final case class TargetRef private (uuid:UUID) extends Ref[Target] {
  def apply(p:Proposal) = p.targets.find(_.uuid == uuid)
}

