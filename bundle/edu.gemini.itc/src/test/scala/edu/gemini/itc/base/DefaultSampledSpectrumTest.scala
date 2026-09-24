package edu.gemini.itc.base

import org.junit.Test
import org.junit.Assert._

/**
 * A spectrum sampled over part of its range must reproduce the full spectrum's grid bit for bit,
 * otherwise lookups near transmission file edges can round outside the file.
 */
class DefaultSampledSpectrumTest {

  // an interval that is not exact in binary, over a range starting off zero
  private val interval = 0.137 / 1.3
  private val source = {
    val xs = (0 to 2000).map(i => 50.3 + i * 1.7).toArray
    new DefaultArraySpectrum(Array(xs, xs.map(x => math.sin(x / 97.0) + 2.0)))
  }

  private def full = new DefaultSampledSpectrum(source, interval)
  private def cut  = new DefaultSampledSpectrum(source, interval, 811.9, 2340.2)

  private def offsetOf(f: DefaultSampledSpectrum, c: DefaultSampledSpectrum): Int = {
    val k0 = f.getLowerIndex(c.getStart)
    val k  = (k0 - 1 to k0 + 1).find(k => f.getX(k) == c.getStart)
    assertTrue("cut-out start is not on the full grid", k.isDefined)
    k.get
  }

  private def assertSameGrid(f: DefaultSampledSpectrum, c: DefaultSampledSpectrum): Unit = {
    val k = offsetOf(f, c)
    assertTrue(c.getLength < f.getLength)
    for (i <- 0 until c.getLength) {
      assertEquals(f.getX(k + i), c.getX(i), 0.0)
      assertEquals(f.getY(k + i), c.getY(i), 0.0)
    }
    for (i <- 1 until c.getLength - 1) {
      val x = c.getX(i)
      val m = (x + c.getX(i + 1)) / 2
      assertEquals(f.getY(x), c.getY(x), 0.0)
      assertEquals(f.getY(m), c.getY(m), 0.0)
    }
  }

  @Test
  def cutOutKeepsTheGrid(): Unit =
    assertSameGrid(full, cut)

  @Test
  def cloneKeepsTheGrid(): Unit =
    assertSameGrid(full, cut.clone().asInstanceOf[DefaultSampledSpectrum])

  @Test
  def rescaleXKeepsTheGrid(): Unit = {
    val f = full
    val c = cut
    f.rescaleX(1.2345)
    c.rescaleX(1.2345)
    assertSameGrid(f, c)
  }

  @Test
  def zerosLikeKeepsTheGrid(): Unit = {
    val c = cut
    val z = DefaultSampledSpectrum.zerosLike(c)
    assertEquals(c.getLength, z.getLength)
    for (i <- 0 until c.getLength) {
      assertEquals(c.getX(i), z.getX(i), 0.0)
      assertEquals(0.0, z.getY(i), 0.0)
    }
  }

  @Test
  def lowerIndexAtTheStartIsZero(): Unit = {
    val c = cut
    assertEquals(0, c.getLowerIndex(c.getStart))
  }

  @Test
  def lowerIndexBelowTheStartIsNegative(): Unit = {
    val c = cut
    assertTrue(c.getLowerIndex(c.getStart - 3 * interval) < 0)
    assertTrue(full.getLowerIndex(full.getStart - 3 * interval) < 0)
  }

}
