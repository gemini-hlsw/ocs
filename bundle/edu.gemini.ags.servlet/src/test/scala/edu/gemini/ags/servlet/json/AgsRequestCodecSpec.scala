package edu.gemini.ags.servlet.json

import edu.gemini.ags.servlet.arb
import edu.gemini.ags.servlet.AgsRequest

object AgsRequestCodecSpec extends InvertibilityTest {

  import agsrequest._
  import arb.agsrequest._

  "AgsRequestCodec" >> {
    "AgsRequest" ! testInvertibility[AgsRequest]
  }

}
