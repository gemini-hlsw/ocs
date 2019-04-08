package edu.gemini.ags.servlet.json

import edu.gemini.ags.servlet.arb
import edu.gemini.spModel.core._


object SiderealTargetCodecSpec extends InvertibilityTest {

  import siderealtarget._

  import arb.siderealtarget._

  "SiderealTargetCodec" >> {
    "SiderealTarget" ! testInvertibility[SiderealTarget]
  }

}
