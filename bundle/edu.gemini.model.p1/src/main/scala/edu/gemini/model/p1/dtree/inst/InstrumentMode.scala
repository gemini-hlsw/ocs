package edu.gemini.model.p1.dtree.inst

import edu.gemini.model.p1.dtree.GenericNode

/**
 * Common observing modes.
 */
object InstrumentMode extends Enumeration {
  val Imaging, Spectroscopy = Value
  type InstrumentMode = Value
}

trait InstrumentModeNode { self: GenericNode =>
  val title       = "Instrument Mode"
  def instrumentName: String
  val description = "%s supports both imaging and spectroscopy.  Please select one.".format(instrumentName)
  val choices     = InstrumentMode.values.toList
}
