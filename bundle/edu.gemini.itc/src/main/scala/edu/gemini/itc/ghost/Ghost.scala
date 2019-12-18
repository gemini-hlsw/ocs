package edu.gemini.itc.ghost

import edu.gemini.itc.base.Instrument.Bands
import edu.gemini.itc.base.{Instrument, SpectroscopyInstrument}
import edu.gemini.itc.shared.GhostParameters
import edu.gemini.spModel.core.Site

/**
 * GHOST specification class
 */
// TODO-GHOSTITC
final class Ghost(gp: GhostParameters) extends Instrument(Site.GS, Bands.NEAR_IR, Ghost.instr_dir, Ghost.filename) with SpectroscopyInstrument{
  /**
   * Returns the effective observing wavelength.
   * This is properly calculated as a flux-weighted averate of
   * observed spectrum.  So this may be temporary.
   *
   * @return Effective wavelength in nm
   */
  override def getEffectiveWavelength: Int = ???

  /**
   * Returns the subdirectory where this instrument's data files are.
   */
  override def getDirectory: String = ???

  override def wellDepth(): Double = ???

  override def gain(): Double = ???

  /** Gets the slit width of the mask in arcsec. */
  override def getSlitWidth: Double = ???
}

object Ghost {
  val filename: String = "ghost" + Instrument.getSuffix
  val instr_dir: String = "ghost"
}