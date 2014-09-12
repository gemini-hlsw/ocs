package edu.gemini.ags.gems.mascot.util

import breeze.linalg._
import Spline._
import YUtils._
import org.junit.{Ignore, Test}

/**
 * Tests methods in the Spline class.
 */

@Ignore class SplineTest {

  /**
   * Tests the spline function.
   * Example (from MascotUtils.magstar()):
   * <p><pre>
   * lambda=[3.5e-07,4e-07,5e-07,6e-07,7e-07,8e-07,9e-07,1e-06,1.1e-06]
   * qe=[0,0.08,0.48,0.592,0.592,0.512,0.368,0.16,0]
   * lvec=[3.5e-07,4.33333e-07,5.16667e-07,6e-07,6.83333e-07,7.66667e-07,8.5e-07,9.33333e-07,1.01667e-06,1.1e-06]
   * spline(qe, lambda, lvec)=[0,0.201141,0.524044,0.592,0.596478,0.546713,0.449436,0.300589,0.129101,0]
   </pre>
   */
  @Test def testSpline3() {
    val lambda = DenseVector(3.5e-07, 4e-07, 5e-07, 6e-07, 7e-07, 8e-07, 9e-07, 1e-06, 1.1e-06)
    val qe = DenseVector(0.0, 0.08, 0.48, 0.592, 0.592, 0.512, 0.368, 0.16, 0.0)
    val lvec = DenseVector(3.5e-07, 4.33333e-07, 5.16667e-07, 6e-07, 6.83333e-07, 7.66667e-07, 8.5e-07, 9.33333e-07, 1.01667e-06, 1.1e-06)
    val expect = DenseVector(0.0, 0.201141, 0.524044, 0.592, 0.596478, 0.546713, 0.449436, 0.300589, 0.129101, 0.0)
    assertVectorsEqual(expect, spline(qe, lambda, lvec), 0.0001)
  }

  /**
   */
  @Test def testSpline2() {
    val y = DenseVector(2.0, 5.0, 7.0, 9.0, 6.0, 5.0, 4.0, 2.0)
    val x = DenseVector(3.0, 4.0, 6.0, 7.0, 9.0, 7.0, 4.0, 2.0)
    val expect = DenseVector(3.18504, 2.62991, -1.14956, 9.63371, -48.0032, 15.6337, -2.91381, 2.95691)
    // Double values may not exactly match the float values used by default by Yorick, so just check that it is close
    assertVectorsEqual(expect, spline(y, x), 0.0001)
  }


  /**
   */
  @Test def testTSpline2() {
    val y = DenseVector(2.0, 5.0, 7.0, 9.0, 6.0, 5.0, 4.0, 2.0)
    val x = DenseVector(3.0, 4.0, 6.0, 7.0, 9.0, 7.0, 4.0, 2.0)
    val tension = 3.0
    val expect = DenseVector(0, 0.899195, -45.4722, 305.293, -2840.54, 268.189, -18.4252, 0.0)
    // Double values may not exactly match the float values used by default by Yorick, so just check that it is close
    assertVectorsEqual(expect, tspline(tension, y, x), 0.01)
  }


  /**
   * Tests the spline function.
   * <p><pre>
   * lambda=[3.5e-07,4e-07,5e-07,6e-07,7e-07,8e-07,9e-07,1e-06,1.1e-06]
   * qe=[0,0.08,0.48,0.592,0.592,0.512,0.368,0.16,0]
   * lvec=[3.5e-07,4.33333e-07,5.16667e-07,6e-07,6.83333e-07,7.66667e-07,8.5e-07,9.33333e-07,1.01667e-06,1.1e-06]
   * tension=3.
   * tspline(tension, qe, lambda, lvec)=[0,0.204591,0.518999,0.592,0.596899,0.545155,0.447527,0.30055,0.130001,0]
   *
   </pre>
   */
  @Test def testTSpline3() {
    val lambda = DenseVector(3.5e-07, 4e-07, 5e-07, 6e-07, 7e-07, 8e-07, 9e-07, 1e-06, 1.1e-06)
    val qe = DenseVector(0.0, 0.08, 0.48, 0.592, 0.592, 0.512, 0.368, 0.16, 0.0)
    val lvec = DenseVector(3.5e-07, 4.33333e-07, 5.16667e-07, 6e-07, 6.83333e-07, 7.66667e-07, 8.5e-07, 9.33333e-07, 1.01667e-06, 1.1e-06)
    val expect = DenseVector(0.0, 0.204591, 0.518999, 0.592, 0.596899, 0.545155, 0.447527, 0.30055, 0.130001, 0.0)
    val tension = 3.0
    assertVectorsEqual(expect, tspline(tension, qe, lambda, lvec), 0.0001)
  }


}