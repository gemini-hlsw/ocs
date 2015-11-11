package jsky.app.ot.ags

sealed abstract class BagsStatus(val message: String)

object BagsStatus {
  case object Pending extends BagsStatus("BAGS lookup pending.")
  case object Running extends BagsStatus("BAGS running...")
  case class Failed(reason: String)  extends BagsStatus(s"BAGS failed to execute: $reason")
  case object NoTarget extends BagsStatus("BAGS could not find a ")
}
