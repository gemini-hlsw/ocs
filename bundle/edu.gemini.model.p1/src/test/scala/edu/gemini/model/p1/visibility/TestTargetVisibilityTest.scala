package edu.gemini.model.p1.visibility

import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.immutable.SemesterOption.{A, B}
import edu.gemini.model.p1.mutable.Band.BAND_1_2
import edu.gemini.model.p1.mutable.GnirsFilter.ORDER_3
import edu.gemini.spModel.core.{Declination, Angle, RightAscension, Coordinates}

import org.junit.Test
import org.junit.Assert._

import TargetVisibility.{Good, Limited, Bad}
import java.util.UUID

class TestTargetVisibilityTest {

  val gnLgs = GnirsBlueprintImaging(AltairLGS(pwfs1 = false), GnirsPixelScale.PS_005, ORDER_3)
  val gnNgs = gnLgs.copy(altair = AltairNone)
  val gsNgs = GmosSBlueprintImaging(Nil)
  val gsLgs = GsaoiBlueprint(Nil)

  val baseTarget   = SiderealTarget(UUID.randomUUID(), "x", Coordinates.zero, CoordinatesEpoch.J_2000, None, Nil)
  val baseObsGNNgs = Observation(Some(gnNgs), None, Some(baseTarget), BAND_1_2, None)
  val baseObsGNLgs = Observation(Some(gnLgs), None, Some(baseTarget), BAND_1_2, None)
  val baseObsGSNgs = Observation(Some(gsNgs), None, Some(baseTarget), BAND_1_2, None)
  val baseObsGSLgs = Observation(Some(gsLgs), None, Some(baseTarget), BAND_1_2, None)

  val semA = Semester(2012, A)
  val semB = Semester(2012, B)

  def coordinates(raStr: String, decStr: String): Coordinates = Coordinates(RightAscension.fromAngle(Angle.parseHMS(raStr).getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS(decStr).getOrElse(Angle.zero)).getOrElse(Declination.zero))

  @Test def testMissingBlueprint(): Unit = {
    val obs = baseObsGNNgs.copy(blueprint = None)
    assertEquals(None, TargetVisibilityCalc.get(semA, obs))
  }

  @Test def testMissingTarget(): Unit = {
    val obs = baseObsGNNgs.copy(target = None)
    assertEquals(None, TargetVisibilityCalc.get(semA, obs))
  }

  private def gnNgsA(expected: TargetVisibility, coords: (String, String)*): Unit = {
    ngs(expected, baseObsGNNgs, semA, coords: _*)
  }

  private def gnNgsB(expected: TargetVisibility, coords: (String, String)*): Unit = {
    ngs(expected, baseObsGNNgs, semB, coords: _*)
  }

  private def gsNgsB(expected: TargetVisibility, coords: (String, String)*): Unit = {
    ngs(expected, baseObsGSNgs, semB, coords: _*)
  }

  private def gsNgsA(expected: TargetVisibility, coords: (String, String)*): Unit = {
    ngs(expected, baseObsGSNgs, semA, coords: _*)
  }

  private def ngs(expected: TargetVisibility, observation: Observation, semester: Semester, coords: (String, String)*): Unit = {
    coords.foreach { tup =>
      val (raStr, decStr) = tup
      val target = baseTarget.copy(coords = coordinates(raStr, decStr))
      val obs    = observation.copy(target = Some(target))
      assertEquals(Some(expected), TargetVisibilityCalc.get(semester, obs))
    }
  }

  private def gsLgsA(expected: TargetVisibility, coords: (String, String)*): Unit = {
    lgs(expected, baseObsGSLgs, semA, coords: _*)
  }

  private def gsLgsB(expected: TargetVisibility, coords: (String, String)*): Unit = {
    lgs(expected, baseObsGSLgs, semB, coords: _*)
  }

  private def gnLgsA(expected: TargetVisibility, coords: (String, String)*): Unit = {
    lgs(expected, baseObsGNLgs, semA, coords: _*)
  }

  private def gnLgsB(expected: TargetVisibility, coords: (String, String)*): Unit = {
    lgs(expected, baseObsGNLgs, semB, coords: _*)
  }

  private def lgs(expected: TargetVisibility, observation: Observation, semester: Semester, coords: (String, String)*): Unit = {
    coords foreach { tup =>
      val (raStr, decStr) = tup
      val target = baseTarget.copy(coords = coordinates(raStr, decStr))
      val obs    = observation.copy(target = Some(target))
      assertEquals(Some(expected), TargetVisibilityCalc.get(semester, obs))
    }
  }

  @Test def testRaGoodDecGood(): Unit = {
    gnNgsA(Good, ("10:00:00", "20:00:00"))
  }

  @Test def testRaGoodDecBad(): Unit = {
    gnNgsA(Bad, ("10:00:00", "-37:00:00.1"))
  }

  @Test def testRaGoodDecLimited(): Unit = {
    gnNgsA(Limited, ("10:00:00",  "79:00:00"))
  }

  @Test def testRaIffyDecGood(): Unit = {
    gnNgsA(Limited, ("5:00:00",  "20:00:00"))
  }

  @Test def testRaBadDecGood(): Unit = {
    gnNgsA(Bad, ("1:00:00",  "20:00:00"), ("3:59:59.9", "20:00:00"))
  }

  @Test def testRaBadDecIffy(): Unit = {
    gnNgsA(Bad, ("1:00:00",  "73:00:00"), ("3:59:59.9", "78:59:59.9"))
  }

  @Test def testDecForGSLgs(): Unit = {
    gsLgsA(Limited, ("09:00:00",  "-72:00:00"))
    gsLgsA(Good, ("09:00:00",  "0:00:00"))
    gsLgsA(Limited, ("09:00:00",  "12:00:00"))
    gsLgsA(Bad, ("09:00:00",  "20:00:00"))
  }

  @Test def testDecForGSLgsSemester(): Unit = {
    // between higher than -70 and less than 5 is Good
    gsLgsB(Good, ("0:00:00",  "-69:59:59.999"))
    gsLgsB(Good, ("0:00:00",  "4:59:59.999"))
    // Less or equal to -75 is Bad
    gsLgsB(Bad, ("0:00:00",  "-75:00:00"))
    gsLgsB(Bad, ("0:00:00",  "-90:00:00"))
    // More or equal to 5 and less than 15 is warning
    gsLgsB(Limited, ("0:00:00",  "10:00:00"))
    gsLgsB(Limited, ("0:00:00",  "14:59:59.999"))
    // More than 15 is bad
    gsLgsB(Bad, ("0:00:00",  "15:00:00"))
    gsLgsB(Bad, ("0:00:00",  "90:00:00"))
    // Up to 10 should be good
    gsLgsB(Good, ("0:00:00",  "9:59:59"))
  }

  @Test def testRaGSLgsBSemester(): Unit = {
    // More or equal to 8 and less than 11 is warning
    gsLgsB(Limited, ("8:00:00", "0:00:00"))
    gsLgsB(Limited, ("10:59:59.999", "0:00:00"))
    // More or equal to 11 and less and equal to 19 is bad
    gsLgsB(Bad, ("11:00:00", "0:00:00"))
    gsLgsB(Bad, ("19:00:00", "0:00:00"))
    // More than 19 and less or equal to 20 is warning
    gsLgsB(Limited, ("19:00:00.001", "0:00:00"))
    gsLgsB(Limited, ("20:00:00", "0:00:00"))
    // Other points are good
    gsLgsB(Good, ("0:00:00", "0:00:00"))
    gsLgsB(Good, ("21:00:00", "0:00:00"))
  }

  @Test def testRaGSNgsSemester(): Unit = {
    // More or equal to 9 and less than 12 is warning
    gsNgsB(Limited, ("9:00:00", "0:00:00"))
    gsNgsB(Limited, ("11:59:59.999", "0:00:00"))
    // More or equal to 12 and less and equal to 16 is bad
    gsNgsB(Bad, ("12:00:00", "0:00:00"))
    gsNgsB(Bad, ("16:00:00", "0:00:00"))
    // More than 16 and less or equal to 19 is warning
    gsNgsB(Limited, ("16:00:00.001", "0:00:00"))
    gsNgsB(Limited, ("19:00:00", "0:00:00"))
    // Other points are good
    gsNgsB(Good, ("0:00:00", "0:00:00"))
    gsNgsB(Good, ("20:00:00", "0:00:00"))
  }

  @Test def testDecGSNgsBSemester(): Unit = {
    // between more than -87 and less than 22 is Good
    gsNgsB(Good, ("22:00:00",  "-86:59:59.999"))
    gsNgsB(Good, ("22:00:00",  "21:59:59.999"))
    // Less or equal than -87 is warning
    gsNgsB(Limited, ("22:00:00",  "-87:00:00"))
    gsNgsB(Limited, ("22:00:00",  "-90:00:00"))
    // More or equal to 22 and less than 28 is warning
    gsNgsB(Limited, ("22:00:00",  "22:00:00"))
    gsNgsB(Limited, ("22:00:00",  "27:59:59.999"))
    // More than than 28 is bad
    gsNgsB(Bad, ("22:00:00",  "28:00:00.0001"))
  }

  @Test def testRaGSNgsASemester(): Unit = {
    // More or equal to 0 and less than 2 is warning
    gsNgsA(Limited, ("0:00:00", "0:00:00"))
    gsNgsA(Limited, ("1:59:59.999", "0:00:00"))
    // More than 5 and less or equal to 7 is warning
    gsNgsA(Limited, ("5:00:00.001", "0:00:00"))
    gsNgsA(Limited, ("7:00:00", "0:00:00"))
    // More than 23 and less or equal to 24 is warning
    gsNgsA(Limited, ("23:00:00.001", "0:00:00"))
    gsNgsA(Limited, ("0:00:00", "0:00:00"))
    // More or equal to 2 and less and equal to 5 is bad
    gsNgsA(Bad, ("2:00:00", "0:00:00"))
    gsNgsA(Bad, ("5:00:00", "0:00:00"))
    // Other points are good
    gsNgsA(Good, ("7:00:00.001", "0:00:00"))
    gsNgsA(Good, ("23:00:00", "0:00:00"))
  }

  @Test def testRaGSLgsASemester(): Unit = {
    // More than 6 and less or equal to 7 is warning
    gsLgsA(Limited, ("6:00:00.001", "0:00:00"))
    gsLgsA(Limited, ("7:00:00", "0:00:00"))
    // More than 18 and less or equal to 20 is warning
    gsLgsA(Limited, ("18:00:00.001", "0:00:00"))
    gsLgsA(Limited, ("19:59:59.999", "0:00:00"))
    // More or equal to 20 and less and equal to 6 is bad
    gsLgsA(Bad, ("20:00:00", "0:00:00"))
    gsLgsA(Bad, ("6:00:00", "0:00:00"))
    // Other points are good
    gsLgsA(Good, ("7:00:00.001", "0:00:00"))
    gsLgsA(Good, ("18:00:00", "0:00:00"))
  }

  @Test def testDecForGNNgs(): Unit = {
    // between higher than -37 and less or equal than -30 is warning
    gnNgsB(Limited, ("9:00:00",  "-36:59:59.999"))
    gnNgsB(Limited, ("9:00:00",  "-30:00:00"))
    // More or equal than 73 is Limited
    gnNgsB(Limited, ("9:00:00",  "73:00:00"))
    gnNgsB(Limited, ("9:00:00",  "90:00:00"))
    // Between 0 and up to -37 is Bad
    gnNgsB(Bad, ("9:00:00",  "-90:00:00"))
    gnNgsB(Bad, ("9:00:00",  "-37:00:00"))
    // Between less than -30 less than -73 is Good
    gnNgsB(Good, ("9:00:00",  "-29:59:59.999"))
    gnNgsB(Good, ("9:00:00",  "72:59:59.999"))

  }

  @Test def testDecForGNLgs(): Unit = {
    // between higher than -27 and less or equal than -22 is warning
    gnLgsB(Limited, ("9:00:00",  "-26:59:59.999"))
    gnLgsB(Limited, ("9:00:00",  "-22:00:00"))
    // between higher or equal than 65 and less than 68 is warning
    gnLgsB(Limited, ("9:00:00",  "65:00:00"))
    gnLgsB(Limited, ("9:00:00",  "67:59:59.999"))
    // Less or equal to -30 is Bad
    gnLgsB(Bad, ("9:00:00",  "-30:00:00"))
    gnLgsB(Bad, ("9:00:00",  "-90:00:00"))
    // More or or equal to 70 is Bad
    gnLgsB(Bad, ("9:00:00",  "70:00:00"))
    gnLgsB(Bad, ("9:00:00",  "90:00:00"))
    // Between more than -22 and less than 65 is Good
    gnLgsB(Good, ("0:00:00",  "-21:59:59.999"))
    gnLgsB(Good, ("0:00:00",  "64:59:59.001"))
  }

  @Test def testRaGNNgsASemester(): Unit = {
    // More or equal to 0 and less than 1 is warning
    gnNgsA(Limited, ("0:00:00", "0:00:00"))
    gnNgsA(Limited, ("0:59:59.999", "0:00:00"))
    // More than 4 and less or equal to 7 is warning
    gnNgsA(Limited, ("4:00:00.001", "0:00:00"))
    gnNgsA(Limited, ("7:00:00", "0:00:00"))
    // More than 22 and less or equal to 24 is warning
    gnNgsA(Limited, ("22:00:00.001", "0:00:00"))
    gnNgsA(Limited, ("0:00:00", "0:00:00"))
    // More or equal to 1 and less and equal to 4 is bad
    gnNgsA(Bad, ("1:00:00", "0:00:00"))
    gnNgsA(Bad, ("4:00:00", "0:00:00"))
    // Other points are good
    gnNgsA(Good, ("7:00:00.1", "0:00:00"))
    gnNgsA(Good, ("22:00:00", "0:00:00"))
  }

  @Test def testRaGNLgsASemester(): Unit = {
    // More or equal to 21 and less than 24 is warning
    gnLgsA(Limited, ("21:00:00", "0:00:00"))
    gnLgsA(Limited, ("23:59:59.999", "0:00:00"))
    // More than 5 and less or equal to 8 is warning
    gnLgsA(Limited, ("5:00:00.001", "0:00:00"))
    gnLgsA(Limited, ("8:00:00", "0:00:00"))
    // More or equal to 0 and less and equal to 5 is bad
    gnLgsA(Bad, ("0:00:00", "0:00:00"))
    gnLgsA(Bad, ("5:00:00", "0:00:00"))
    // Other points are good
    gnLgsA(Good, ("8:00:00.001", "0:00:00"))
    gnLgsA(Good, ("20:59:59.999", "0:00:00"))
  }

  @Test def testRaGNNgsBSemester(): Unit = {
    // More or equal to 11 and less than 13:30 is warning
    gnNgsB(Limited, ("11:00:00", "0:00:00"))
    gnNgsB(Limited, ("13:29:59.999", "0:00:00"))
    // More than 17 and less or equal to 19 is warning
    gnNgsB(Limited, ("17:00:00.001", "0:00:00"))
    gnNgsB(Limited, ("19:00:00", "0:00:00"))
    // More or equal to 13:30 and less and equal to 17 is bad
    gnNgsB(Bad, ("13:30:00", "0:00:00"))
    gnNgsB(Bad, ("17:00:00", "0:00:00"))
    // Other points are good
    gnNgsB(Good, ("0:00:00", "0:00:00"))
    gnNgsB(Good, ("10:59:59.999", "0:00:00"))
    gnNgsB(Good, ("19:00:00.001", "0:00:00"))
    gnNgsB(Good, ("23:59:59.999", "0:00:00"))
  }

  @Test def testRaGNLgsBSemester(): Unit = {
    // More or equal to 10 and less than 12:30 is warning
    gnLgsB(Limited, ("10:00:00", "0:00:00"))
    gnLgsB(Limited, ("12:29:59.999", "0:00:00"))
    // More than 18 and less or equal to 20 is warning
    gnLgsB(Limited, ("18:00:00.001", "0:00:00"))
    gnLgsB(Limited, ("20:00:00", "0:00:00"))
    // More or equal to 12:30 and less and equal to 18 is bad
    gnLgsB(Bad, ("12:30:00", "0:00:00"))
    gnLgsB(Bad, ("18:00:00", "0:00:00"))
    // Other points are good
    gnNgsB(Good, ("0:00:00", "0:00:00"))
    gnNgsB(Good, ("9:59:59.999", "0:00:00"))
    gnNgsB(Good, ("20:00:00.001", "0:00:00"))
    gnNgsB(Good, ("23:59:59.999", "0:00:00"))
  }

  @Test def testRaWrap(): Unit = {
    val t0 = baseTarget.copy(coords = coordinates("23:00:00", "20:00:00"))
    val t1 = baseTarget.copy(coords = coordinates("00:00:00", "20:00:00"))
    val t2 = baseTarget.copy(coords = coordinates("00:30:00", "20:00:00"))

    List(t0, t1, t2) foreach { t =>
      assertEquals(Some(Limited), TargetVisibilityCalc.get(semA, baseObsGNNgs.copy(target = Some(t))))
      assertEquals(Some(Good),    TargetVisibilityCalc.get(semB, baseObsGNNgs.copy(target = Some(t))))
    }
  }

  @Test def testRaForSpecialCases(): Unit = {
    val t0 = baseTarget.copy(coords = coordinates("23:00:00", "20:00:00"))
    val t1 = baseTarget.copy(coords = coordinates("00:00:00", "20:00:00"))
    val t2 = baseTarget.copy(coords = coordinates("00:30:00", "20:00:00"))

    List(t0, t1, t2) foreach { t =>
      assertEquals(Some(Good), TargetVisibilityCalc.getOnDec(semA, baseObsGNNgs.copy(target = Some(t))))
      assertEquals(Some(Good),    TargetVisibilityCalc.getOnDec(semB, baseObsGNNgs.copy(target = Some(t))))
    }
  }

  @Test def testDecWrap(): Unit = {
    val t0 = baseTarget.copy(coords = coordinates("10:00:00", "-38:00:00"))
    val t1 = baseTarget.copy(coords = coordinates("10:00:00", "-90:00:00"))
    val t2 = baseTarget.copy(coords = coordinates("10:00:00",  "-50:00:00"))

    List(t0, t1, t2) foreach { t =>
      assertEquals(Some(Bad), TargetVisibilityCalc.get(semA, baseObsGNNgs.copy(target = Some(t))))
      assertEquals(Some(Bad), TargetVisibilityCalc.get(semB, baseObsGNNgs.copy(target = Some(t))))
    }
  }

  @Test def testDecWrapForSpecialCases(): Unit = {
    val t0 = baseTarget.copy(coords = coordinates("10:00:00", "-38:00:00"))
    val t1 = baseTarget.copy(coords = coordinates("10:00:00", "-90:00:00"))
    val t2 = baseTarget.copy(coords = coordinates("10:00:00",  "-50:00:00"))

    List(t0, t1, t2) foreach { t =>
      assertEquals(Some(Bad), TargetVisibilityCalc.get(semA, baseObsGNNgs.copy(target = Some(t))))
      assertEquals(Some(Bad), TargetVisibilityCalc.get(semB, baseObsGNNgs.copy(target = Some(t))))
    }
  }
}
