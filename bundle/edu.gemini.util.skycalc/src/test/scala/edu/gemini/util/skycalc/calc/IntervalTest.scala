package edu.gemini.util.skycalc.calc

import org.junit.Test
import org.junit.Assert._

class IntervalTest {

  @Test
  def overlap(): Unit = {
    val i1 = Interval(5,10)
    val i2 = Interval(2,6)
    val i3 = Interval(6,9)
    val i4 = Interval(8,12)

    assertEquals(Interval(5,6), i1.overlap(i2))
    assertEquals(Interval(6,9), i1.overlap(i3))
    assertEquals(Interval(6,9), i3.overlap(i1))
    assertEquals(Interval(8,10), i1.overlap(i4))

  }

  // check some common and some corner cases..

  @Test
  def reduceByOne(): Unit = {
    assertEquals(
      Seq(Interval(1000, 1200), Interval(1300, 2000)),
      Interval.reduce(Interval(1000, 2000), Seq(Interval(1200, 1300))))
  }

  @Test
  def reduceByTwo(): Unit = {
    assertEquals(
      Seq(Interval(1000, 1200), Interval(1300, 1500), Interval(1800, 2000)),
      Interval.reduce(Interval(1000, 2000), Seq(Interval(1200, 1300), Interval(1500, 1800))))
  }

  @Test
  def reduceByThree(): Unit = {
    assertEquals(
      Seq(Interval(1000, 1200), Interval(1300, 1500), Interval(1800, 1950)),
      Interval.reduce(Interval(1000, 2000), Seq(Interval(1200, 1300), Interval(1500, 1800), Interval(1950, 2000))))
  }

  @Test
  def reduceAtHead(): Unit = {
    assertEquals(
      Seq(Interval(1300, 2000)),
      Interval.reduce(Interval(1000, 2000), Seq(Interval(1000, 1300))))
  }

  @Test
  def reduceAtEnd(): Unit = {
    assertEquals(
      Seq(Interval(1000, 1700)),
      Interval.reduce(Interval(1000, 2000), Seq(Interval(1700, 2000))))
  }

  @Test
  def reduceByTwoConsecutive(): Unit = {
    assertEquals(
      Seq(Interval(1000, 1200), Interval(1300, 2000)),
      Interval.reduce(Interval(1000, 2000), Seq(Interval(1200, 1250), Interval(1250, 1300))))
  }

  @Test
  def reduceByThreeConsecutive(): Unit = {
    assertEquals(
      Seq(Interval(1000, 1200), Interval(1400, 2000)),
      Interval.reduce(Interval(1000, 2000), Seq(Interval(1200, 1250), Interval(1250, 1300), Interval(1300, 1400))))
  }

  @Test
  def reduceByTwoConsecutiveAtHead(): Unit = {
    assertEquals(
      Seq(Interval(1200, 2000)),
      Interval.reduce(Interval(1000, 2000), Seq(Interval(1000, 1100), Interval(1100, 1200))))
  }

  @Test
  def reduceByTwoConsecutiveAtEnd(): Unit = {
    assertEquals(
      Seq(Interval(1000, 1800)),
      Interval.reduce(Interval(1000, 2000), Seq(Interval(1800, 1900), Interval(1900, 2000))))
  }

}
