// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.config2.{Config, ItemKey}

import java.time.Duration

import scalaz.Equal

sealed trait GhostCamera extends Product with Serializable {

  /** Exposure count for this camera. */
  def count: Int

  /** Time for a single exposure. */
  def oneExposure:  Duration

  /** Total exposure time, ignoring readout. */
  def totalExposure: Duration =
    oneExposure.multipliedBy(count.toLong)

  /** Read gain. */
  def readNoiseGain: GhostReadNoiseGain

  /** Binning. */
  def binning: GhostBinning

  /** Cost for the a single exposure readout using the associated speed and binning. */
  def oneReadout: Duration

  /** Total readout time, ignoring exposure time */
  def totalReadout: Duration =
    oneReadout.multipliedBy(count.toLong)

  /** Total exposure + readout time. */
  def totalTime: Duration =
    totalExposure.plus(totalReadout)

  /** Label, "red" or "blue". */
  def label: String =
    this match {
      case _: GhostCamera.Red  => "red"
      case _: GhostCamera.Blue => "blue"
    }

}

object GhostCamera {

  private def duration(sec: Int, milliSeconds: Int): Duration =
    Duration.ofSeconds(sec.toLong, milliSeconds.toLong * 1000000L)

  final case class Red(
                        count:         Int,
                        oneExposure:   Duration,
                        readNoiseGain: GhostReadNoiseGain,
                        binning:        GhostBinning
  ) extends GhostCamera {

    override def oneReadout: Duration =
      Red.ReadoutTime((binning, readNoiseGain))
  }

  final case class Blue(
                         count:         Int,
                         oneExposure:   Duration,
                         readNoiseGain: GhostReadNoiseGain,
                         binning:       GhostBinning
  ) extends GhostCamera {

    override def oneReadout: Duration =
      Blue.ReadoutTime((binning, readNoiseGain))

  }

  private def secondsToDuration(secs: Double): Duration =
    Duration.ofMillis(Math.round(secs * 1000.0))

  private def anyToDuration(any: Option[AnyRef]): Duration =
    secondsToDuration(any.collect {
      case d: java.lang.Double => d.doubleValue
    }.getOrElse(0.0))

  private def configToDuration(config: Config, key: ItemKey): Duration =
    anyToDuration(Option(config.getItemValue(key)))

  private def anyToCount(any: Option[AnyRef]): Int =
    any.collect {
      case i: java.lang.Integer => i.intValue
    }.getOrElse(1)

  private def configToCount(config: Config, key: ItemKey): Int =
    anyToCount(Option(config.getItemValue(key)))

  private def configToA[A](config: Config, key: ItemKey, default: => A)(pf: PartialFunction[AnyRef, A]): A =
    Option(config.getItemValue(key)).collect(pf).getOrElse(default)

  private def configToGain(config: Config, key: ItemKey, default: => GhostReadNoiseGain): GhostReadNoiseGain =
    configToA(config, key, default) {
      case g: GhostReadNoiseGain => g
    }

  private def configToBinning(config: Config, key: ItemKey): GhostBinning =
    configToA(config, key, GhostBinning.DEFAULT) {
      case g: GhostBinning => g
    }


  object Red {

    def fromConfig(c: Config): Red =
      Red(
        configToCount(c, Ghost.RED_EXPOSURE_COUNT_OBS_KEY),
        configToDuration(c, Ghost.RED_EXPOSURE_TIME_OBS_KEY),
        configToGain(c, Ghost.RED_READ_NOISE_GAIN_KEY, GhostReadNoiseGain.DEFAULT_RED),
        configToBinning(c, Ghost.RED_BINNING_KEY)
      )

    val ReadoutTime: Map[(GhostBinning, GhostReadNoiseGain), Duration] = {
      import GhostBinning._
      import GhostReadNoiseGain._
      val m = Map(
        (ONE_BY_ONE, SLOW_LOW)     -> duration(100, 675),
        (ONE_BY_ONE, MEDIUM_LOW)   -> duration( 58, 994),
        (ONE_BY_ONE, FAST_LOW)     -> duration( 23, 520),

        (ONE_BY_TWO, SLOW_LOW)     -> duration( 51, 271),
        (ONE_BY_TWO, MEDIUM_LOW)   -> duration( 30, 230),
        (ONE_BY_TWO, FAST_LOW)     -> duration( 12, 341),

        (ONE_BY_FOUR, SLOW_LOW)    -> duration( 26, 564),
        (ONE_BY_FOUR, MEDIUM_LOW)  -> duration( 15, 838),
        (ONE_BY_FOUR, FAST_LOW)    -> duration(  6, 773),

        (ONE_BY_EIGHT, SLOW_LOW)   -> duration( 14, 198),
        (ONE_BY_EIGHT, MEDIUM_LOW) -> duration(  8, 686),
        (ONE_BY_EIGHT, FAST_LOW)   -> duration(  3, 977),

        (TWO_BY_TWO, SLOW_LOW)     -> duration( 28, 364),
        (TWO_BY_TWO, MEDIUM_LOW)   -> duration( 17, 696),
        (TWO_BY_TWO, FAST_LOW)     -> duration(  8, 577),

        (TWO_BY_FOUR, SLOW_LOW)    -> duration( 15, 146),
        (TWO_BY_FOUR, MEDIUM_LOW)  -> duration(  9, 638),
        (TWO_BY_FOUR, FAST_LOW)    -> duration(  4, 929),

        (TWO_BY_EIGHT, SLOW_LOW)   -> duration(  8, 534),
        (TWO_BY_EIGHT, MEDIUM_LOW) -> duration(  5, 580),
        (TWO_BY_EIGHT, FAST_LOW)   -> duration(  3, 578),

        (FOUR_BY_FOUR, SLOW_LOW)   -> duration(  9, 536),
        (FOUR_BY_FOUR, MEDIUM_LOW) -> duration(  6, 581),
        (FOUR_BY_FOUR, FAST_LOW)   -> duration(  4,  77)
      )
      GhostBinning.values.foldLeft(m) { (m, b) =>
        m.updated((b, FAST_HIGH), m(b, FAST_LOW))
      }
    }
  }

  object Blue {

    def fromConfig(c: Config): Blue =
      Blue(
        configToCount(c, Ghost.BLUE_EXPOSURE_COUNT_OBS_KEY),
        configToDuration(c, Ghost.BLUE_EXPOSURE_TIME_OBS_KEY),
        configToGain(c, Ghost.BLUE_READ_NOISE_GAIN_KEY, GhostReadNoiseGain.DEFAULT_BLUE),
        configToBinning(c, Ghost.BLUE_BINNING_KEY)
      )

    val ReadoutTime: Map[(GhostBinning, GhostReadNoiseGain), Duration] = {
      import GhostBinning._
      import GhostReadNoiseGain._
      val m = Map(
        (ONE_BY_ONE, SLOW_LOW)     -> duration(45, 957),
        (ONE_BY_ONE, MEDIUM_LOW)   -> duration(27, 118),
        (ONE_BY_ONE, FAST_LOW)     -> duration(11,  78),

        (ONE_BY_TWO, SLOW_LOW)     -> duration(23, 808),
        (ONE_BY_TWO, MEDIUM_LOW)   -> duration(14, 237),
        (ONE_BY_TWO, FAST_LOW)     -> duration( 6,  72),

        (ONE_BY_FOUR, SLOW_LOW)    -> duration(12, 741),
        (ONE_BY_FOUR, MEDIUM_LOW)  -> duration( 7, 784),
        (ONE_BY_FOUR, FAST_LOW)    -> duration( 3, 575),

        (ONE_BY_EIGHT, SLOW_LOW)   -> duration( 7, 229),
        (ONE_BY_EIGHT, MEDIUM_LOW) -> duration( 4, 574),
        (ONE_BY_EIGHT, FAST_LOW)   -> duration( 3,  75),

        (TWO_BY_TWO, SLOW_LOW)     -> duration(13, 644),
        (TWO_BY_TWO, MEDIUM_LOW)   -> duration( 8, 633),
        (TWO_BY_TWO, FAST_LOW)     -> duration( 4, 425),

        (TWO_BY_FOUR, SLOW_LOW)    -> duration( 7, 680),
        (TWO_BY_FOUR, MEDIUM_LOW)  -> duration( 5,  24),
        (TWO_BY_FOUR, FAST_LOW)    -> duration( 3,  71),

        (TWO_BY_EIGHT, SLOW_LOW)   -> duration( 4, 722),
        (TWO_BY_EIGHT, MEDIUM_LOW) -> duration( 3, 223),
        (TWO_BY_EIGHT, FAST_LOW)   -> duration( 3,  42),

        (FOUR_BY_FOUR, SLOW_LOW)   -> duration( 5, 226),
        (FOUR_BY_FOUR, MEDIUM_LOW) -> duration( 3, 722),
        (FOUR_BY_FOUR, FAST_LOW)   -> duration( 3,  44)
      )
      GhostBinning.values.foldLeft(m) { (m, b) =>
        m.updated((b, FAST_HIGH), m(b, FAST_LOW))
      }
    }

  }

  implicit val EqualGhostCamera: Equal[GhostCamera] =
    Equal.equalA

}
