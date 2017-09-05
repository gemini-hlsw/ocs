package edu.gemini.spModel.gemini.flamingos2

import edu.gemini.spModel.gemini.EnumTest

import edu.gemini.spModel.gemini.flamingos2.Flamingos2._

import org.junit.Test

class F2EnumsTest {

  @Test def testWindowCover(): Unit =
    EnumTest.test(classOf[WindowCover], WindowCover.valueOf)

  @Test def testReadoutMode(): Unit =
    EnumTest.test(classOf[ReadoutMode], ReadoutMode.valueOf)

  @Test def testReads(): Unit =
    EnumTest.test(classOf[Reads], Reads.valueOf)

}
