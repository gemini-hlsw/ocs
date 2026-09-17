package edu.gemini.itc.base

import org.junit.Test
import org.junit.Assert._

import scala.util.Random

/**
 * The cursor and gallop paths in getLowerIndex must return exactly what a plain binary search
 * over the x axis returns, whatever order the lookups come in.
 */
class DefaultArraySpectrumLookupTest {

  private def reference(xs: Array[Double], x: Double): Int = {
    var lo = 0
    var hi = xs.length
    while (hi - lo > 1) {
      val m = (hi + lo) / 2
      if (xs(m) < x) lo = m else hi = m
    }
    lo
  }

  private def spectrum(xs: Array[Double]): DefaultArraySpectrum =
    new DefaultArraySpectrum(Array(xs.clone, xs.map(_ => 1.0)))

  private def axis(r: Random, n: Int, withRepeats: Boolean): Array[Double] = {
    var x = r.nextDouble() * 100
    Array.fill(n) {
      if (!(withRepeats && r.nextInt(5) == 0)) x += r.nextDouble() * 3 + 1e-3
      x
    }
  }

  private def lookups(r: Random, xs: Array[Double], n: Int): Seq[Double] = {
    val lo = xs.head
    val hi = xs.last
    val span = hi - lo
    var x = lo - 1
    (0 until n).map { _ =>
      r.nextInt(10) match {
        case 0 => x = lo + r.nextDouble() * span
        case 1 => x = x - r.nextDouble() * span / 4
        case 2 => x = if (r.nextBoolean()) hi + r.nextDouble() else lo - r.nextDouble()
        case 3 => x = xs(r.nextInt(xs.length))
        case 4 => x = Double.NaN
        case 5 => x = x + r.nextDouble() * span / 3
        case _ => x = x + r.nextDouble() * span / 200
      }
      x
    }
  }

  private def check(xs: Array[Double], qs: Seq[Double]): Unit = {
    val s = spectrum(xs)
    qs.foreach { q =>
      assertEquals(s"x = $q", reference(xs, q), s.getLowerIndex(q))
    }
  }

  @Test def matchesBinarySearchOnStrictAxis(): Unit = {
    val r = new Random(4711)
    (1 to 200).foreach { _ =>
      val xs = axis(r, 2 + r.nextInt(400), withRepeats = false)
      check(xs, lookups(r, xs, 500))
    }
  }

  @Test def matchesBinarySearchWithRepeatedX(): Unit = {
    val r = new Random(1138)
    (1 to 200).foreach { _ =>
      val xs = axis(r, 2 + r.nextInt(400), withRepeats = true)
      check(xs, lookups(r, xs, 500))
    }
  }

  @Test def ascendingScanOverFinerGrid(): Unit = {
    val xs = (0 until 5000).map(i => 300.0 + i * 0.05).toArray
    check(xs, (0 until 700).map(i => 299.0 + i * 0.37))
  }

  @Test def tinyAxes(): Unit = {
    check(Array(1.0), Seq(0.0, 1.0, 2.0))
    check(Array(1.0, 2.0), Seq(0.0, 1.0, 1.5, 2.0, 3.0, 1.2, Double.NaN, 1.9))
  }
}
