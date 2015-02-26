package edu.gemini.itc.baseline

import edu.gemini.itc.acqcam.{AcqCamRecipe, AcquisitionCamParameters}
import edu.gemini.itc.baseline.util.Baseline._
import edu.gemini.itc.baseline.util._

/**
 * Acquisition camera baseline test bits and pieces.
 */
object BaselineAcqCam {

  lazy val Fixtures = RBandImaging

  def executeRecipe(f: Fixture[AcquisitionCamParameters]): Output =
    cookRecipe(w => new AcqCamRecipe(f.src, f.odp, f.ocp, f.ins, f.tep, w))

  private lazy val RBandImaging = Fixture.rBandImgFixtures(List(
    new AcquisitionCamParameters(
      "R",
      AcquisitionCamParameters.NDA)
  ))

}
