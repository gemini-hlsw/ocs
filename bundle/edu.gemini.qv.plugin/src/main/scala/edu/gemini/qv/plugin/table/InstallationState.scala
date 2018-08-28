package edu.gemini.qv.plugin.table

/**
 * Instrument component installation state, according to the ICTD.
 */
sealed trait InstallationState extends Product with Serializable

object InstallationState {

  /** All instrument components and custom mask (if any) known to be installed. */
  case object AllInstalled     extends InstallationState

  /** One or more components or custom mask known to be not installed. */
  case object SomeNotInstalled extends InstallationState

  /** Unknown installation state (ICTD unavailable). */
  case object Unknown          extends InstallationState
}
