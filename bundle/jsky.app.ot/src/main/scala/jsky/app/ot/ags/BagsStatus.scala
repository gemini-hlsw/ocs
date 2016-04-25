package jsky.app.ot.ags

sealed trait BagsStatus {
  def message: String
  def reason: Option[String] = None
  def toPending: BagsStatus = BagsStatus.Pending(reason)
  def toRunning: BagsStatus = BagsStatus.Running(reason)
}

object BagsStatus {
  case class Pending(override val reason: Option[String]) extends BagsStatus {
    override val message = reason.map(r => s"$r. Will retry catalog lookup.").getOrElse("Catalog looking pending.")
  }
  case class Running(override val reason: Option[String]) extends BagsStatus {
    override val message = reason.map(r => s"$r. Retrying catalog lookup.").getOrElse("Catalog lookup running...")
  }

  val NewPending: BagsStatus = Pending(None)
  val NewRunning: BagsStatus = Running(None)
}