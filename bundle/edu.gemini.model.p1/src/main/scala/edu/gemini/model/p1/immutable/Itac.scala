package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

object Itac {
  def apply(m: M.Itac): Itac =
    Itac(
      (Option(m.getAccept) map { ma => Right(ItacAccept(ma)) }).orElse(Option(m.getReject).map(_ => Left[ItacReject, ItacAccept](ItacReject))),
      Option(m.getNgoauthority),
      Option(m.getComment)
    )
}

// Placeholder for now...
case class Itac(decision: Option[Either[ItacReject, ItacAccept]], ngoAuthority: Option[NgoPartner], comment: Option[String]) {
  def mutable = {
    val m = Factory.createItac
    decision match {
      case Some(Left(_))  => m.setReject(ItacReject.mutable)
      case Some(Right(a)) => m.setAccept(a.mutable)
      case _              =>
    }
    comment foreach { m.setComment _ }
    ngoAuthority foreach { m.setNgoauthority _ }
    m
  }
}

object ItacAccept {
  def apply(m: M.ItacAccept): ItacAccept =
    ItacAccept(
      m.getProgramId,
      Option(m.getContact),
      Option(m.getEmail),
      m.getBand,
      TimeAmount(m.getAward),
      m.isRollover
    )
}

case class ItacAccept(programId: String, contact: Option[String], email: Option[String], band: Int, award: TimeAmount, rollover: Boolean) {
  def mutable = {
    val m = Factory.createItacAccept()
    m.setProgramId(programId)
    contact foreach { m.setContact _ }
    email foreach { m.setEmail _ }
    m.setBand(band)
    m.setAward(award.mutable)
    m.setRollover(rollover)
    m
  }
}

sealed trait ItacReject {
  def mutable = Factory.createItacReject()
}

case object ItacReject extends ItacReject