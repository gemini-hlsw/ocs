package edu.gemini.ags.servlet.json

import edu.gemini.ags.api.AgsStrategy.Selection
import edu.gemini.ags.servlet.arb

object SelectionCodecSpec extends InvertibilityTest {

  import selection._
  import arb.selection._

  "SelectionCodec" >> {
    "Selection" ! testInvertibility[Selection]
  }

}
