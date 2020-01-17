package edu.gemini.itc.ghost

import edu.gemini.itc.base.{SpectroscopyArrayRecipe, SpectroscopyResult}
import edu.gemini.itc.shared.{ItcParameters, ItcSpectroscopyResult}

/**
 * This class performs the calculations for GHOST.
 */
final class GhostRecipe(p: ItcParameters) extends SpectroscopyArrayRecipe {
  override def calculateSpectroscopy(): Array[SpectroscopyResult] = ???

  override def serviceResult(r: Array[SpectroscopyResult], headless: Boolean): ItcSpectroscopyResult = ???
}
