package edu.gemini.spModel.gemini.gnirs

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.ConfigSequence
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.{Wavelength, Camera, PixelScale}
import edu.gemini.spModel.test.InstrumentSequenceTestBase

import org.junit.Test
import org.junit.Assert._

/**
 *
 */
class CameraCalcTest extends InstrumentSequenceTestBase[InstGNIRS, SeqConfigGNIRS] {

  override def getObsCompSpType: SPComponentType = InstGNIRS.SP_TYPE
  override def getSeqCompSpType: SPComponentType = SeqConfigGNIRS.SP_TYPE

  private def configSeq: ConfigSequence   = ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.IDENTITY_MAP)
  private def cameraAt(step: Int): Camera = configSeq.getItemValue(step, GNIRSConstants.CAMERA_KEY).asInstanceOf[Camera]
  private def camera: Camera              = cameraAt(0)


  @Test def testStatic(): Unit = {
    // Default value corresponds to SHORT_BLUE
    assertEquals(Camera.SHORT_BLUE, camera)

    // Change to LONG BLUE
    val gnirs = getInstDataObj
    assertTrue(gnirs.getCentralWavelength.doubleValue() < 2.5)
    gnirs.setPixelScale(PixelScale.PS_005)
    storeStaticUpdates()
    assertEquals(Camera.LONG_BLUE, camera)

    // Change to LONG RED
    gnirs.setPixelScale(PixelScale.PS_005)
    gnirs.setCentralWavelength(2.6)
    storeStaticUpdates()
    assertEquals(Camera.LONG_RED, camera)

    // Change to SHORT RED
    gnirs.setPixelScale(PixelScale.PS_015)
    gnirs.setCentralWavelength(2.6)
    storeStaticUpdates()
    assertEquals(Camera.SHORT_RED, camera)
  }

  @Test def testIteration(): Unit = {
    import InstrumentSequenceTestBase._

    def assertCameras(expected: Camera*): Unit = {
      println("EXPECTED:")
      expected.foreach(println(_))

      val actual = configSeq.getItemValueAtEachStep(GNIRSConstants.CAMERA_KEY)
      println("ACTUAL:")
      actual.foreach(println(_))
      assertEquals(expected.size, actual.size)
      expected.zip(actual).foreach { case (exp, act) => assertEquals(exp, act) }
    }

    val gnirs = getInstDataObj
    gnirs.setPixelScale(PixelScale.PS_005) // LONG_*
    storeStaticUpdates()

    val sc = createSysConfig()
    // RED, BLUE, RED
    sc.putParameter(getParam(GNIRSConstants.CENTRAL_WAVELENGTH_PROP, new Wavelength("2.5"), new Wavelength("2.4"), new Wavelength("2.6")))
    setSysConfig(sc)
    assertCameras(Camera.LONG_RED, Camera.LONG_BLUE, Camera.LONG_RED)

    getInstDataObj.setPixelScale(PixelScale.PS_015) // SHORT_*
    storeStaticUpdates()
    assertCameras(Camera.SHORT_RED, Camera.SHORT_BLUE, Camera.SHORT_RED)
  }
}
