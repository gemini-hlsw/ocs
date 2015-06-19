package edu.gemini.model.p1.targetio.impl

import TargetUtil._

import edu.gemini.model.p1.immutable.SiderealTarget
import edu.gemini.spModel.core.{MagnitudeSystem, MagnitudeBand}

import org.junit.Test


class SiderealReaderTest extends ReaderTestBase(SiderealReader) {

  @Test def testMissingColumn() {
    missing(List("Name"), """RAJ2000,DecJ2000
1:00:00,2:00:00
""")
    missing(List("RAJ2000"), """Name,DecJ2000
ngc007,00:00:07.00
""")
    missing(List("DecJ2000"), """Name,RAJ2000
ngc007,00:00:07.00
""")
  }

  import SiderealColumns._

  val csvHeader = "Name,RAJ2000,DecJ2000,pmRA,pmDec"

  @Test def testBadData() {
    onebadrow(NAME, "", csvHeader + """
,1:00:00,2:00:00,1.0,2.0
""")
    onebadrow(RA, "x", csvHeader + """
ngc007,x,2:00:00,1.0,2.0
""")
    onebadrow(RA, "24:00:00.00", csvHeader + """
ngc007,24:00:00.00,2:00:00,1.0,2.0
""")
    onebadrow(DEC, "", csvHeader + """
ngc007,1:00:00.00,,1.0,2.0
""")
    onebadrow(PM_RA, "1.7mas", csvHeader + """
ngc007,1:00:00.00,2:00:00,1.7mas,2.0
""")
  }

  val target1 = mkTarget("ngc007", "01:00:00.00", "02:00:00", 1.0, 2.0)

  @Test def testSingleTarget() {
    good(target1, csvHeader + """
ngc007,01:00:00.00,02:00:00,1.0,2.0
""")
  }

  @Test def testDegrees() {
    good(target1, csvHeader + """
ngc007,15.0,2.0,1.0,2.0
""")
    good(target1, csvHeader + """
ngc007,15,2,1.0,2.0
""")
  }

  @Test def testNoProperMotion() {
    good(target1.copy(properMotion = None), """Name,RAJ2000,DecJ2000
ngc007,15,2
""")
  }

  @Test def testColumnOrder() {
    good(target1, """DecJ2000,Name,RAJ2000,pmDec,pmRA
02:00:00.00,ngc007,01:00:00,2.0,1.0
""")
  }


  @Test def testMagWithNoSystem() {
    good(target1.copy(magnitudes = List(mkMag(6.6, MagnitudeBand.R), mkMag(7.7, MagnitudeBand.J))), csvHeader + """,R,J
ngc007,01:00:00.00,02:00:00,1.0,2.0,6.6,7.7
""")
  }

  @Test def testMagWithSystem() {
    good(target1.copy(magnitudes = List(mkMag(6.6, MagnitudeBand.R, MagnitudeSystem.JY), mkMag(7.7, MagnitudeBand.J))), csvHeader + """,R,J,R_sys
ngc007,01:00:00.00,02:00:00,1.0,2.0,6.6,7.7,JY
""")
    good(target1.copy(magnitudes = List(mkMag(6.6, MagnitudeBand.R, MagnitudeSystem.JY), mkMag(7.7, MagnitudeBand.J))), csvHeader + """,R,J,R_sys,J_sys
ngc007,01:00:00.00,02:00:00,1.0,2.0,6.6,7.7,JY,INDEF
""")
    good(target1.copy(magnitudes = List(mkMag(6.6, MagnitudeBand.R, MagnitudeSystem.JY))), csvHeader + """,R,J,R_sys,J_sys
ngc007,01:00:00.00,02:00:00,1.0,2.0,6.6,INDEF,JY,INDEF
""")
  }

  val target2 = mkTarget("ngc008", "08:00:00.00", "-02:00:00", 10.0, 11.0)


  @Test def testMultiTarget() {
    good(List(target1, target2), csvHeader + """
ngc007,01:00:00.00,02:00:00,1.0,2.0
ngc008,08:00:00.00,-02:00:00,10.0,11.0
""")
  }

  protected def validateTarget(expected: SiderealTarget, actual: SiderealTarget) {
    TargetUtil.validateTarget(expected, actual)
  }
}