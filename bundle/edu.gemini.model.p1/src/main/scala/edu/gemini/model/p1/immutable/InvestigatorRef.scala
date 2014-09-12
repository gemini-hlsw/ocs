package edu.gemini.model.p1.immutable

import java.util.UUID

object InvestigatorRef {
  def apply(i:Investigator):InvestigatorRef = apply(i.uuid)
  val empty = apply(UUID.randomUUID)
}

final case class InvestigatorRef private (uuid:UUID) extends Ref[Investigator] {
  def apply(p:Proposal) = p.investigators.all.find(_.uuid == uuid)
}

