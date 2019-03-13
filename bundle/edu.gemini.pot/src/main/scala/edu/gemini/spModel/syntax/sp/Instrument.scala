package edu.gemini.spModel.syntax.sp

import edu.gemini.pot.sp.Instrument
import edu.gemini.pot.sp.Instrument._

// NB: it would make sense to add this to the Instrument enum itself, but that
// would break serialization.

final class InstrumentOps(val self: Instrument) extends AnyVal {

  /**
   * Is this a "visitor" instrument.  In other words, is it fully integrated
   * with the observing system and DHS?
   */
  def isVisitor: Boolean =
    self match {
      case AcquisitionCamera | Phoenix | Texes | Visitor => true
      case _                                             => false
    }

  /**
   * Is this is a facility instrument that is fully integerated with the
   * observing system and DHS?
   */
  def isFacility: Boolean =
    !isVisitor

}

trait ToInstrumentOps {
  implicit def ToInstrumentOps(i: Instrument): InstrumentOps =
    new InstrumentOps(i)
}

object instrument extends ToInstrumentOps
