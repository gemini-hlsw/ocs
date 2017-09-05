package edu.gemini.spModel.gemini.michelle

import edu.gemini.spModel.gemini.EnumTest

import edu.gemini.spModel.gemini.michelle.MichelleParams._

import org.junit.Test

class MichelleEnumsTest {

  @Test def testChopMode(): Unit =
    EnumTest.test(classOf[ChopMode], ChopMode.valueOf)

  @Test def testChopWaveform(): Unit =
    EnumTest.test(classOf[ChopWaveform], ChopWaveform.valueOf)

  @Test def testDispersrOrder(): Unit =
    EnumTest.test(classOf[DisperserOrder], DisperserOrder.valueOf)

  @Test def testPosition(): Unit =
    EnumTest.test(classOf[Position], Position.valueOf)

  @Test def testEngMask(): Unit =
    EnumTest.test(classOf[EngMask], EngMask.valueOf)

  @Test def testFilterWheelA(): Unit =
    EnumTest.test(classOf[FilterWheelA], FilterWheelA.valueOf)

  @Test def testFilterWheelB(): Unit =
    EnumTest.test(classOf[FilterWheelB], FilterWheelB.valueOf)

}
