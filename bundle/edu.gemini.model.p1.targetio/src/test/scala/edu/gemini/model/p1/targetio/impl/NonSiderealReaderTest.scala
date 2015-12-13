package edu.gemini.model.p1.targetio.impl

import TargetUtil._

import edu.gemini.model.p1.immutable.NonSiderealTarget

import org.junit.Assert._
import org.junit.{Ignore, Test}

class NonSiderealReaderTest extends ReaderTestBase(NonSiderealReader) {

  val csvHeader = "ID,Name,UTC,RAJ2000,DecJ2000,Mag"

  @Test def testMissingColumn() {
    missing(List("ID"), """Name,UTC,RAJ2000,DecJ2000,Mag
Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
""")
    missing(List("UTC"), """ID,Name,RAJ2000,DecJ2000,Mag
1,Ceres,23:52:00.00,-11:20:00.0,9.0
""")
    missing(List("UTC", "DecJ2000"), """ID,Name,RAJ2000,Mag
1,Ceres,23:52:00.00,9.0
""")
  }


  import NonSiderealColumns._

  @Test def testBadData() {
    onebadrow(ID, "", csvHeader + """
,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
""")
    onebadrow(ID, "x", csvHeader + """
x,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
""")
    onebadrow(NAME, "", csvHeader + """
1,,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
""")
    onebadrow(UTC, "2011-Mec-30 00:00", csvHeader + """
1,Ceres,2011-Mec-30 00:00,23:52:00.00,-11:20:00.0,9.0
""")
    onebadrow(RA, "x", csvHeader + """
1,Ceres,2011-Dec-30 00:00,x,-11:20:00.0,9.0
""")
  }

  @Test def testtSingleEphemeris() {
    good(mkTarget("Ceres", List(dec30)), csvHeader + """
1,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
""")
  }

  @Test def testMultipleEphemeris() {
    good(mkTarget("Ceres", List(dec30, dec31)), csvHeader + """
1,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
1,Ceres,2011-Dec-31 00:00,23:52:01.00,-11:20:01.0,9.1
""")
  }

  @Test def testMissingMagColumnOk() {
    good(mkTarget("Ceres", List(dec30.copy(magnitude = None), dec31.copy(magnitude = None))),
"""ID,Name,UTC,RAJ2000,DecJ2000
1,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0
1,Ceres,2011-Dec-31 00:00,23:52:01.00,-11:20:01.0
""")
  }

  @Test def testMissingMagOk() {
    good(mkTarget("Ceres", List(dec30, dec31.copy(magnitude = None))), csvHeader + """
1,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
1,Ceres,2011-Dec-31 00:00,23:52:01.00,-11:20:01.0,
""")
  }

  @Test def testUnorderedRowsOk() {
    good(mkTarget("Ceres", List(dec30, dec31, jan01)), csvHeader + """
1,Ceres,2012-Jan-01 00:00,23:52:02.00,-11:20:02.0,9.2
1,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
1,Ceres,2011-Dec-31 00:00,23:52:01.00,-11:20:01.0,9.1
""")
  }

  @Test def testDegreesOk() {
    good(mkTarget("Ceres", List(dec30)), csvHeader + """
1,Ceres,2011-Dec-30 00:00,358,-11.33333333,9.0
""")
  }

  @Test def testUnorderedColumnsOk() {
    good(mkTarget("Ceres", List(dec30, dec31, jan01)),
"""UTC,Name,ID,RAJ2000,DecJ2000,Mag
2012-Jan-01 00:00,Ceres,1,23:52:02.00,-11:20:02.0,9.2
2011-Dec-30 00:00,Ceres,1,23:52:00.00,-11:20:00.0,9.0
2011-Dec-31 00:00,Ceres,1,23:52:01.00,-11:20:01.0,9.1
""")
  }

  val multiTargets = List(
    mkTarget("Ceres",  List(dec30, dec31, jan01)),
    mkTarget("Halley", List(aug15, aug16, aug17))
  )

  @Test def testMultiTargets() {
    good(multiTargets, csvHeader + """
1,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
1,Ceres,2011-Dec-31 00:00,23:52:01.00,-11:20:01.0,9.1
1,Ceres,2012-Jan-01 00:00,23:52:02.00,-11:20:02.0,9.2
2,Halley,2012-Aug-15 00:00,15:00:00.00,-15:00:00.0,15.0
2,Halley,2012-Aug-16 00:00,16:00:00.00,-16:00:00.0,16.0
2,Halley,2012-Aug-17 00:00,17:00:00.00,-17:00:00.0,17.0
""")
  }

  @Test def testScrambled() {
    good(multiTargets, csvHeader + """
1,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
2,Halley,2012-Aug-16 00:00,16:00:00.00,-16:00:00.0,16.0
1,Ceres,2012-Jan-01 00:00,23:52:02.00,-11:20:02.0,9.2
1,Ceres,2011-Dec-31 00:00,23:52:01.00,-11:20:01.0,9.1
2,Halley,2012-Aug-17 00:00,17:00:00.00,-17:00:00.0,17.0
2,Halley,2012-Aug-15 00:00,15:00:00.00,-15:00:00.0,15.0
""")
  }

  @Test def testMixed() {
    val input = csvHeader + """
1,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
1,Ceres,2011-Dec-31 00:00,23:52:01.00,-11:20:01.0,9.1
1,Ceres,2012-Jan-01 00:00,23:52:02.00,-11:20:02.0,9.s
2,Halley,2012-Aug-15 00:00,15:00:00.00,-15:00:00.0,15.0
2,Halley,2012-Augg-16 00:00,16:00:00.00,-16:00:00.0,16.0
2,Halley,2012-Aug-17 00:00,17:00:00.00,-17:00:00.0,17.0
"""
    val expected = List(
      mkTarget("Ceres",  List(dec30, dec31)),
      mkTarget("Halley", List(aug15, aug17))
    )

    NonSiderealReader.read(input) match {
      case Left(err)  => fail(err.msg)
      case Right(lst) =>
        val (lefts, rights) = lst.partition(_.isLeft)
        val expectedMessages = List(
          NonSiderealColumns.MAG.parseError("9.s"),
          NonSiderealColumns.UTC.parseError("2012-Augg-16 00:00")
        )
        assertEquals(expectedMessages, lefts.map(_.left.get.msg))
        validateTargets(expected, rights map { e => e.right.get })
    }
  }

  @Test def testMultiTargetsSameName() {
    good(List(mkTarget("Ceres", List(dec30, dec31, jan01)), mkTarget("Ceres", List(aug15, aug16, aug17))), csvHeader + """
1,Ceres,2011-Dec-30 00:00,23:52:00.00,-11:20:00.0,9.0
1,Ceres,2011-Dec-31 00:00,23:52:01.00,-11:20:01.0,9.1
1,Ceres,2012-Jan-01 00:00,23:52:02.00,-11:20:02.0,9.2
2,Ceres,2012-Aug-15 00:00,15:00:00.00,-15:00:00.0,15.0
2,Ceres,2012-Aug-16 00:00,16:00:00.00,-16:00:00.0,16.0
2,Ceres,2012-Aug-17 00:00,17:00:00.00,-17:00:00.0,17.0
""")
  }

  def validateTarget(expected: NonSiderealTarget, actual: NonSiderealTarget) {
    TargetUtil.validateTarget(expected, actual)
  }
}