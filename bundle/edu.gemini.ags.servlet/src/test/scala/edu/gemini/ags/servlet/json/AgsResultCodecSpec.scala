package edu.gemini.ags.servlet.json

import edu.gemini.ags.servlet.arb
import edu.gemini.ags.servlet.AgsResult

object AgsResultCodecSpec extends InvertibilityTest {

  import agsresult._
  import arb.agsresult._

  "AgsResultCodec" >> {
    "AgsResult" ! testInvertibility[AgsResult]
  }

}
