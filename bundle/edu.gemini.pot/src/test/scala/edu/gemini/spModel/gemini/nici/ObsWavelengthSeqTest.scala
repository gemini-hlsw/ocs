package edu.gemini.spModel.gemini.nici

import NICIParams._

import edu.gemini.spModel.config.injector.ObsWavelengthTestBase
import edu.gemini.spModel.data.config.DefaultParameter

import org.junit.Test

import edu.gemini.spModel.test.{InstrumentSequenceTestBase => Base}

/**
 * ObsWavelength sequence tests for NICI.
 */
class ObsWavelengthSeqTest extends ObsWavelengthTestBase[InstNICI, SeqConfigNICI] {
  private def modeParam(m: ImagingMode*): DefaultParameter = param(InstNICI.IMAGING_MODE_PROP, m: _*)
  private def redParam(r: Channel1FW*): DefaultParameter   = param(InstNICI.CHANNEL1_FW_PROP, r: _*)
  private def blueParam(b: Channel2FW*): DefaultParameter  = param(InstNICI.CHANNEL2_FW_PROP, b: _*)
  private def maskParam(m: PupilMask*): DefaultParameter   = param(InstNICI.PUPIL_MASK_PROP, m: _*)

  def getObsCompSpType = InstNICI.SP_TYPE
  def getSeqCompSpType = SeqConfigNICI.SP_TYPE

  private def wl(m: ImagingMode): Option[java.lang.Double] =
    for {
      meta <- Option(ImagingModeMetaconfig.getMetaConfig(m))
    } yield meta.getChannel1Fw.centralWavelength

  private def wl(d: java.lang.Double): Option[java.lang.Double] =
    if (java.lang.Double.isNaN(d.doubleValue)) None else Some(d)

  private def wl(r: Channel1FW): Option[java.lang.Double] = wl(r.centralWavelength)
  private def wl(b: Channel2FW): Option[java.lang.Double] = wl(b.centralWavelength)

  private def s(opt: Option[java.lang.Double]): String = opt.get.toString

  implicit def modeToStringWavelength(m: ImagingMode): String = s(wl(m))
  implicit def redToStringWavelength(r: Channel1FW): String = s(wl(r))
  implicit def blueToStringWavelength(b: Channel2FW): String = s(wl(b))

  @Test def testStatic() {
    getInstDataObj.setImagingMode(ImagingMode.H1SLA)
    storeStaticUpdates()

    // Iterate over a couple of values that don't impact the calculation.
    val sc = Base.createSysConfig
    sc.putParameter(maskParam(PupilMask.EIGHTY_PERCENT, PupilMask.NINETY_FIVE_PERCENT))
    setSysConfig(sc)

    // Observing wavelength should come from the imaging mode in the static
    // component.
    val exp: String = ImagingMode.H1SLA
    verifyWavelength(exp, exp)
    verifyWavelengthWithName(InstNICI.CENTRAL_WAVELENGTH_PROP.getName, exp, exp)
  }

  @Test def testInheritManual() {
    getInstDataObj.setImagingMode(ImagingMode.MANUAL)
    storeStaticUpdates()

    val sc = Base.createSysConfig
    sc.putParameter(redParam(Channel1FW.K, Channel1FW.KS))
    setSysConfig(sc)

    verifyWavelength(Channel1FW.K, Channel1FW.KS)
    verifyWavelengthWithName(InstNICI.CENTRAL_WAVELENGTH_PROP.getName, Channel1FW.K, Channel1FW.KS)
  }

  @Test def testInheritRed() {
    val sc = Base.createSysConfig
    sc.putParameter(modeParam(ImagingMode.H4SLA, ImagingMode.MANUAL))
    setSysConfig(sc)

    getInstDataObj.setChannel1Fw(Channel1FW.K_PRIMMA)
    storeStaticUpdates()

    // In the first step, the explicitly set H4SLA takes precedence.  In the
    // second step, we inherit the value of the red filter wheel.
    verifyWavelength(ImagingMode.H4SLA, Channel1FW.K_PRIMMA)
  }

  @Test def testIterateAll() {
    val sc = Base.createSysConfig
    sc.putParameter(modeParam(ImagingMode.H4SLA, ImagingMode.MANUAL, ImagingMode.MANUAL, ImagingMode.MANUAL))
    sc.putParameter(redParam(Channel1FW.K_PRIMMA, Channel1FW.L_PRIMMA, Channel1FW.BLOCK, Channel1FW.BLOCK))
    sc.putParameter(blueParam(Channel2FW.H, Channel2FW.J, Channel2FW.FE_II, Channel2FW.BLOCK))
    setSysConfig(sc)

    // In the first step the explicitly set H4SLA mode take precedence.  In the
    // second step, the red L_PRIMMA is used, in the third step, the
    // blue FE_II, and in the final step the default value.
    verifyWavelength(ImagingMode.H4SLA, Channel1FW.L_PRIMMA, Channel2FW.FE_II, InstNICI.DEF_CENTRAL_WAVELENGTH.toString)
  }
}