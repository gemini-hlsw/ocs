package edu.gemini.util.skycalc.calc

/**
 * Base trait for all calculators.
 * A calculator basically holds a matrix of values which are sampled at defined points in time over a given interval.
 * For each sampling point in time a vector with an arbitrary number of values can be stored.
 */
trait Calculator {

  val times: Vector[Long]
  def toIndex(t: Long): Int
  val values: Vector[Vector[Double]]

  lazy val start = times.head
  lazy val end = times.last
  lazy val samples = times.size

  /** True if the values for the given time are covered by this target. */
  def isDefinedAt(t: Long) = t >= start && t <= end
  def value(field: Int, ix: Int) = values(field)(ix)
  def valueAt(field: Int, t: Long) = values(field)(toIndex(t))
  def timedValues(field: Int) = times.zip(values(field))

  def min(field: Int): Double = values(field).min
  def max(field: Int): Double = values(field).max
  def mean(field: Int): Double = values(field).sum / samples

}

/**
 * Base trait for all calculators that provide a sampling of values over time at a given rate.
 * Define a single time to make this work.
 */
trait SingleValueCalculator extends Calculator {
  val time: Long
  val times = Vector(time)
  def toIndex(t: Long) = 0
}

/**
 * Base trait for all calculators that provide a sampling of values over time at a given rate.
 * Define an interval and a sampling rate to make this work.
 */
trait FixedRateCalculator extends Calculator {
  require(rate > 0)

  val defined: Interval
  val rate: Long

  // the number of samples we need to have a sampling rate >= than expected
  private val cnt: Int = Math.ceil(defined.duration.toDouble/rate).toInt
  // the precise rate in milliseconds that corresponds to the expected rate
  private val preciseRate: Double = defined.duration.toDouble / cnt

  /** Calculates a vector with times that cover the given interval. */
  val times: Vector[Long] = {
    val ts = for {
      i <- 0 to cnt
    } yield {
      Math.ceil(defined.start + i*preciseRate).toLong     // always round up
    }
    require(ts.head == defined.start)
    require(ts.last >= defined.end)
    Vector(ts:_*)
  }

  /** Gets the index to the left of the given value t. */
  def toIndex(t: Long) = {
    require(t >= start)
    require(t <= end)
    val ix = Math.floor((t - start) / preciseRate).toInt   // always round down; the sample at this index gives a value <= t
    require(times(ix) <= t)
    require(ix == samples-1 || times(ix+1) > t)
    ix
  }

}

/**
 * Sampling at irregular intervals e.g middle dark time etc.
 * Define a vector with sampling times to make this work.
 */
trait IrregularIntervalCalculator extends Calculator {
  require(times.size > 0)

  /** Irregular interval calculators need to define a vector of times at which to sample the data. */
  val times: Vector[Long]

  /** Gets the index to the left of the given value t. */
  def toIndex(t: Long) = {
    require(t >= start)
    require(t <= end)
    val ix = times.zipWithIndex.reverse.dropWhile(_._1 > t).head._2
    // postconditions: useful for debugging / documentation
    // require(ix >= 0 && ix < samples)
    // require(times(ix) <= t && (ix == samples-1 || times(ix+1) > t))
    ix
  }
}


/**
 * Add capabilities for linear interpolation for times that fall between two calculated values.
 */
trait LinearInterpolatingCalculator extends Calculator {

  /**
   * Gets the value at time t. If t falls between two values a linear approximation for the value is calculated
   * from the values to the left and to the right.
   */
  override def valueAt(field: Int, t: Long): Double = {
    val ix = toIndex(t)
    val t0 = times(ix)
    val v0 = values(field)(ix)
    if (t0 == t || ix == samples-1) v0
    else {
      val t1 = times(ix+1)
      // require(t0 <= t && t < t1)
      val v1 = values(field)(ix+1)
      val v = v0 + (t - t0).toDouble/(t1 - t0) * (v1 - v0)
      // require((v0 >= v1 && v0 >= v && v >= v1) || (v0 < v1 && v0 <= v && v <= v1))
      v
    }
  }

}

