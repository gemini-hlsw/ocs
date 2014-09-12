package edu.gemini.model.p1.dtree.exchange

import edu.gemini.model.p1.dtree.SingleSelectNode
import edu.gemini.model.p1.immutable.{KeckBlueprint, KeckInstrument}

object Keck {
  def apply() = new InstrumentNode

  class InstrumentNode extends SingleSelectNode[Unit, KeckInstrument, KeckBlueprint](()) {
    val title       = "Keck Instrument"
    val description = "Select the Keck instrument."
    def choices     = KeckInstrument.values.toList

    def apply(i: KeckInstrument) = Right(KeckBlueprint(i))

    def unapply = {
      case b: KeckBlueprint => b.instrument
    }
  }

}