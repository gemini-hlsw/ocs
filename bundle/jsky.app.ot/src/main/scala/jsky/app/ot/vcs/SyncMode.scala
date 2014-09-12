package jsky.app.ot.vcs

sealed trait SyncMode

object SyncMode {
  case object ClosePrompt extends SyncMode
  case object All extends SyncMode
}