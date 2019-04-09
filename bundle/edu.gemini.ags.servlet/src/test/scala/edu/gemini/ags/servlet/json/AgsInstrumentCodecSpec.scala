package edu.gemini.ags.servlet.json

import edu.gemini.ags.servlet.arb
import edu.gemini.ags.servlet.AgsInstrument

object AgsInstrumentCodecSpec extends InvertibilityTest {

  import agsinstrument._
  import arb.agsinstrument._

  "AgsInstrumentCodec" >> {
    "AgsInstrument" ! testInvertibility[AgsInstrument]
  }

}
