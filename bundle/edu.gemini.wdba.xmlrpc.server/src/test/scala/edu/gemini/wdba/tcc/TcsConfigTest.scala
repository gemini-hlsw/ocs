package edu.gemini.wdba.tcc

import edu.gemini.spModel.core._
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.util.SPTreeUtil
import org.junit.Assert._
import org.junit.Test

import scalaz.==>>


final class TcsConfigTest extends TestBase {

  private def setBase(target: Target): Unit = {
    val oc  = SPTreeUtil.findTargetEnvNode(obs)
    val toc = oc.getDataObject.asInstanceOf[TargetObsComp]
    val env = toc.getTargetEnvironment
    env.getArbitraryTargetFromAsterism.setTarget(target)

    oc.setDataObject(toc)
  }

  @Test
  def testNonSiderealWithoutHorizonsDesignation(): Unit = {
    setBase(NonSiderealTarget("Titan", Ephemeris.empty, None, List.empty, None, None))
    val m = getTcsConfigurationMap(getSouthResults)
    assertNull(m.get(TccNames.EPHEMERIS))
  }

  @Test
  def testSidreal(): Unit = {
    setBase(SiderealTarget("NGC 1407",
                           Coordinates(RightAscension.fromAngle(Angle.parseHMS("3:40:11.9").toOption.get),
                                       Declination.fromAngle(Angle.parseDMS("-18:34:48").toOption.get).get),
                           None,
                           None,
                           None,
                           Nil,
                           None,
                           None))

    val m = getTcsConfigurationMap(getSouthResults)
    assertNull(m.get(TccNames.EPHEMERIS))
  }
}
