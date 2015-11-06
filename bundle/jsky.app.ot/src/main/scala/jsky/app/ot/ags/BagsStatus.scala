package jsky.app.ot.ags

sealed abstract class BagsStatus(val message: String)

object BagsStatus {
  case object Pending extends BagsStatus("BAGS lookup pending.")
  case object Running extends BagsStatus("BAGS running...")
  case object Failed  extends BagsStatus("BAGS failed to execute properly.")
}
