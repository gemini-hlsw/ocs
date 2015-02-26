package edu.gemini.ags.gems.mascot

import org.junit.{Ignore, Test}
import org.junit.Assert._
import breeze.linalg._
import edu.gemini.ags.gems.mascot.util.YUtils._
import MascotConf._
import MascotUtils._

/**
 * Tests methods in the MascotUtils class.
 * <p>
 * Notes:
 * <p><pre>
 * Yorick         Scala
 * ------------+---------------
 * c(,+)*d(,+)    c*d.t
 * c(+,)*d(+,)    c.t*d
 * c(,+)*d(+,)    c*d
 * c(+,)*d(,+)    c.t*d.t
 * </pre>
 */

class MascotUtilsTest {

  //  /**
  //   * wfs_noise([11.9,12.3,13.2])=[0.00702195,0.00702195,0.00845244,0.00845244,0.0128579,0.0128579]
  //   */
  @Test def testWfsNoise() {
    val mag = Array(11.9, 12.3, 13.2)
    val expect = DenseVector(0.00702195, 0.00702195, 0.00845244, 0.00845244, 0.0128579, 0.0128579)
    assertVectorsEqual(expect, wfsNoise(mag), 0.0001)
  }

  /**
   * magsky()=[3544.85,5545.6,12097.4,43228.6]
   */
  @Test def testMagsky() {
    assertVectorsEqual(DenseVector(3544.85, 5545.6, 12097.4, 43228.6), magsky(), 0.1)
  }

  /**
   * magstar(11.9)=1.61069e+07
   * magstar(12.3)=1.11432e+07
   * magstar(13.2)=4.8642e+06
   */
  @Test def testMagstar() {
    assertEquals(1.61069e+07, magstar(11.9), 50)
    assertEquals(1.11432e+07, magstar(12.3), 50)
    assertEquals(4.8642e+06, magstar(13.2), 50)
  }

  @Test def testReadZernikeSpectra() {
    val m = fitsRead("zernike_spectra.fits")
    assertTrue(m.isInstanceOf[Matrix[Double]])
    assertEquals(2000, m.rows)
    assertEquals(6, m.cols)
    val err = 0.001
    assertEquals(0.001, m(0, 0), err)
    assertEquals(0.011, m(1, 0), err)
    assertEquals(0.021, m(2, 0), err)
    assertEquals(0.031, m(3, 0), err)
  }

  // Compare result with a FITS file saved from the yorick version
  @Test def testNullModesSpectra() {
    val m = nullModesSpectra()
    assertEquals(4000, m.rows)
    assertEquals(6, m.cols)
    val mf = fitsRead("null_modes_spectra.fits")
    assertMatricesEqual(mf, m, 0.0001)
  }


  @Test def testVibSpectra() {
    var m = vibSpectra()
    assertEquals(4000, m.rows)
    assertEquals(6, m.cols)
    val mf = fitsRead("vib_spectra.fits")

    if (m(::, 0).max > sampfreq) {
      val tmp = where(m(::, 0), _ < sampfreq)
      val w = tmp(tmp.size - 1)
      m = m(0 to w, ::)
    }

    assertMatricesEqual(mf, m, 0.0001)
  }


  //> fff=ftcb(1.,2.,3.,4.,5)
  //> fff
  //[[0.1,0.2,0.3,0.4,0.5],[8.61236,0.582888,0.100617,0.0250806,0.00731436],
  //[0.145737,0.326927,0.825702,1.40629,1.01085],[1.25514,0.190562,0.0830799,
  //0.0352705,0.00739372]]
  //> dimsof(fff)
  //[2,5,4]
  @Test def testFtcb() {
    assertMatricesEqual(
      DenseMatrix(
        (0.1, 0.2, 0.3, 0.4, 0.5),
        (8.61236, 0.582888, 0.100617, 0.0250806, 0.00731436),
        (0.145737, 0.326927, 0.825702, 1.40629, 1.01085),
        (1.25514, 0.190562, 0.0830799, 0.0352705, 0.00739372)
    ).t,
    ftcb(1.0, 2.0, 3.0, 4.0, 5), 0.0001)
  }
}