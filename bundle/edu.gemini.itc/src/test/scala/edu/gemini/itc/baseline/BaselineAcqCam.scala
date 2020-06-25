package edu.gemini.itc.baseline

import edu.gemini.itc.baseline.util._
import edu.gemini.itc.shared.AcquisitionCamParameters
import edu.gemini.spModel.gemini.acqcam.AcqCamParams._

/**
 * Acquisition camera baseline test bits and pieces.
 */
object BaselineAcqCam {

  lazy val Fixtures = RBandImaging

  private lazy val RBandImaging = Fixture.rBandImgFixtures(List(
    new AcquisitionCamParameters(
      ColorFilter.R_G0154,
      NDFilter.NEUTRAL)
  ))

}
