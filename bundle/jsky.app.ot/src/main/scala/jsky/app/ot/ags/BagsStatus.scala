package jsky.app.ot.ags

import scalaz._
import Scalaz._

sealed trait BagsStatus {
  def message: Option[String] = None
  def reason: Option[String] = None
  def toPending: BagsStatus = BagsStatus.Pending(reason)
  def toRunning: BagsStatus = BagsStatus.Running(reason)
}

object BagsStatus {
  case object Success extends BagsStatus

  case class Pending(override val reason: Option[String]) extends BagsStatus {
    override val message = reason.map(r => s"$r. Will retry catalog lookup.").getOrElse("Catalog looking pending.").some
  }
  case class Running(override val reason: Option[String]) extends BagsStatus {
    override val message = reason.map(r => s"$r. Retrying catalog lookup.").getOrElse("Catalog lookup running...").some
  }

  val NewPending: BagsStatus = Pending(None)
  val NewRunning: BagsStatus = Running(None)
}