package edu.gemini.dataman.core

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.core.SPProgramID
import edu.gemini.spModel.dataset.DatasetLabel

import scalaz._

/** Identifier for a program, observation, or dataset label.
  */
sealed trait DmanId {
  def pid: SPProgramID
}

object DmanId {
  final case class Prog(pid: SPProgramID)    extends DmanId

  final case class Obs(oid: SPObservationID) extends DmanId {
    def pid: SPProgramID = oid.getProgramID
  }

  final case class Dset(label: DatasetLabel) extends DmanId {
    def pid: SPProgramID     = oid.getProgramID
    def oid: SPObservationID = label.getObservationId
  }

  implicit val EqualDmanId: Equal[DmanId] = Equal.equalA

  def parse(s: String): Option[DmanId] = {
    def parse[A](c: String => A): Option[A] =
      \/.fromTryCatch { c(s) }.toOption

    parse(s => Dset(new DatasetLabel(s)))     orElse
     parse(s => Obs(new SPObservationID(s)))  orElse
     parse(s => Prog(SPProgramID.toProgramID(s)))
  }
}
