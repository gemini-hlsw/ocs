package edu.gemini.ags.servlet.json

import edu.gemini.ags.servlet.arb
import edu.gemini.spModel.core._


object CoordinatesCodecSpec extends InvertibilityTest {

  import coordinates._

  import arb.coordinates._

  "CoordinatesCodec" >> {
    "RightAscension"  ! testInvertibility[RightAscension]
    "Declination"     ! testInvertibility[Declination]
    "Coordinates"     ! testInvertibility[Coordinates]
  }


}
