package edu.gemini.itc.baseline

import edu.gemini.itc.acqcam.AcquisitionCamParameters
import edu.gemini.itc.baseline.util._

/**
 * Acquisition camera baseline test bits and pieces.
 */
object BaselineAcqCam {

  lazy val Fixtures = RBandImaging

  private lazy val RBandImaging = Fixture.rBandImgFixtures(List(
    new AcquisitionCamParameters(
      "R",
      AcquisitionCamParameters.NDA)
  ))

}
