package edu.gemini.spModel.gemini.gnirs

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.config2.ConfigSequence
import edu.gemini.spModel.data.config.{DefaultParameter, IParameter}
import edu.gemini.spModel.obscomp.InstConstants
import edu.gemini.spModel.test.InstrumentSequenceTestBase
import InstrumentSequenceTestBase._
import GNIRSParams.AcquisitionMirror.{IN, OUT}
import GNIRSParams.Filter.{J, K, X_DISPERSED, Y}

import org.junit.Test
import org.junit.Assert._

import scala.collection.JavaConverters._

class ObsWavelengthTest extends InstrumentSequenceTestBase[InstGNIRS, SeqConfigGNIRS] {

  override def getObsCompSpType: SPComponentType = InstGNIRS.SP_TYPE
  override def getSeqCompSpType: SPComponentType = SeqConfigGNIRS.SP_TYPE

  private def configSeq: ConfigSequence =
    ConfigBridge.extractSequence(getObs, null, ConfigValMapInstances.IDENTITY_MAP)

  def param[T](n: String, vs: Seq[T]): IParameter =
    DefaultParameter.getInstance(n, vs.toList.asJava)

  private def setOverrideMode(over: Boolean): Unit = {
    val gnirs = getInstDataObj
    gnirs.setOverrideAcqObsWavelength(over)
    storeStaticUpdates()
  }

  private def setCentralWavelength(d: Double): Unit = {
    val gnirs = getInstDataObj
    gnirs.setCentralWavelength(d)
    storeStaticUpdates()
  }

  private def setSequence(s: (GNIRSParams.AcquisitionMirror, GNIRSParams.Filter)*): Unit = {
    val sc = createSysConfig()
    val (as, fs) = s.unzip
    sc.putParameter(param(GNIRSConstants.ACQUISITION_MIRROR_PROP, as))
    sc.putParameter(param(GNIRSConstants.FILTER_PROP,             fs))
    setSysConfig(sc)
  }

  private def assertWavelengths(wl: String*): Unit = {
    val actual = configSeq.getItemValueAtEachStep(InstConstants.OBSERVING_WAVELENGTH_KEY)
    wl.zipAll(actual, "", "").foreach { case (e,a) =>
      assertEquals(s"Expected: $e, actual: $a", e, a)
    }
  }

  @Test def testAcqImagingWavelength(): Unit = {
    // In general, new acquisition imaging observations should take observing
    // wavelength from filter
    setSequence((IN, J), (IN, K), (IN, Y))
    assertWavelengths("1.25", "2.20", "1.03")
  }

  @Test def testOldObservation(): Unit = {
    // Old executed observations should not override observing wavelength
    setOverrideMode(false)
    setCentralWavelength(4.0)
    setSequence((IN, J), (IN, K), (IN, Y))
    assertWavelengths("4.0", "4.0", "4.0")
  }

  @Test def testNotAcq(): Unit = {
    // For steps without the acquisition mirror in, use grating central wavelength
    setCentralWavelength(4.0)
    setSequence((IN, J), (OUT, K), (IN, Y))
    assertWavelengths("1.25", "4.0", "1.03")
  }

  @Test def testNoFilter(): Unit = {
    // If filters aren't defined, use grating central wavelength
    setCentralWavelength(4.0)
    val sc = createSysConfig()
    sc.putParameter(param(GNIRSConstants.ACQUISITION_MIRROR_PROP, List(IN, IN, IN)))
    setSysConfig(sc)

    assertWavelengths("4.0", "4.0", "4.0")
  }

  @Test def testNoFilterWavelength(): Unit = {
    // If no wavelength associated with a filter, use grating central wl
    setCentralWavelength(4.0)
    setSequence((IN, J), (IN, X_DISPERSED), (IN, Y))
    assertWavelengths("1.25", "4.0", "1.03")
  }

  @Test def testOldObservationCopy(): Unit = {
    setOverrideMode(false)
    setCentralWavelength(4.0)
    setSequence((IN, J), (IN, K), (IN, Y))
    assertWavelengths("4.0", "4.0", "4.0")

    // Copying should produce a copy with a true override value
    val obs2 = getFactory.createObservationCopy(getProgram, getObs, false)
    val seq2 = ConfigBridge.extractSequence(obs2, null, ConfigValMapInstances.IDENTITY_MAP)
    val act2 = seq2.getItemValueAtEachStep(InstConstants.OBSERVING_WAVELENGTH_KEY)
    val exp2 = List("1.25", "2.20", "1.03")

    exp2.zipAll(act2, "", "").foreach { case (e,a) =>
      assertEquals(s"Expected: $e, actual: $a", e, a)
    }
  }

}
