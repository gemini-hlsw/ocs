package edu.gemini.spModel.core

import org.junit.Test
import org.junit.Assert._

class ProgramIdTest {
  val Sem13B = Semester.parse("2013B")
  val Sem14A = Semester.parse("2014A")
  val Sem14B = Semester.parse("2014B")

  @Test def testScience(): Unit = {
    ProgramId.parse("GS-2014B-Q-42") match {
      case ProgramId.Science(Site.GS, Sem14B, ProgramType.Queue, 42) => // ok
      case _ => fail("GS-2014B-Q-42")
    }
  }

  @Test def testSciencePatternUnrecognizedType(): Unit = {
    val Sem14B = Semester.parse("2014B")
    ProgramId.parse("GS-2014B-XXX-42") match {
      case ProgramId.Arbitrary(Some(Site.GS), Some(Sem14B), None, "GS-2014B-XXX-42") => // ok
      case _ => fail("GS-2014B-XXX-42")
    }
  }

  @Test def testDaily(): Unit = {
    ProgramId.parse("GS-ENG20140321") match {
      case ProgramId.Daily(Site.GS, ProgramType.Engineering, 2014, 3, 21) => // ok
      case _ => fail("GS-ENG20140321")
    }
  }

  @Test def testDailySemester(): Unit = {
    val semList = List(
      "GS-CAL20140131" -> Sem13B,
      "GS-CAL20140201" -> Sem14A,
      "GS-CAL20140731" -> Sem14A,
      "GS-CAL20140801" -> Sem14B
    )
    semList.foreach { case (pidStr, sem) =>
      ProgramId.parse(pidStr) match {
        case d: ProgramId.Daily if d.semester.exists(_ == sem) => // ok
        case _ => fail(s"Expected semester $sem for id $pidStr")
      }
    }
  }

  @Test def testArbitraryEng(): Unit = {
    ProgramId.parse("GS-ENG-Telescope-Setup") match {
      case ProgramId.Arbitrary(Some(Site.GS), None, Some(ProgramType.Engineering), "GS-ENG-Telescope-Setup") => // ok
      case _ => fail("GS-ENG-Telescope-Setup")
    }
  }

  @Test def testShortName(): Unit = {
    def shorten(fullName: String): String =
      ProgramId.parse(fullName).shortName

    assertEquals("science pid", "N-18A-Q-2",   shorten("GN-2018A-Q-2"))
    assertEquals("daily pid",   "S-ENG180102", shorten("GS-ENG20180102"))
    assertEquals("daily pid",   "S-ENG180319", shorten("GS-ENG20180319"))
    assertEquals("arbitrary",   "Andy",        shorten("Andy"))
  }
}
