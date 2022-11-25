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
  def gain: GhostReadNoiseGain

  /** Binning. */
  def binning: GhostBinning

  /** Cost for a single 1x1 readout for this camera at the associated speed. */
  protected def oneByOneReadout: Duration

  /** Cost for the a single exposure readout using the associated speed and binning. */
  def oneReadout: Duration =
    //The readout time scales with the binning so,
    //readout = readout(1x1) / (xbin * ybin)
    oneByOneReadout
      .dividedBy((binning.getSpatialBinning * binning.getSpectralBinning).toLong)

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

  private def duration(sec: Int, deciSeconds: Int): Duration =
    Duration.ofSeconds(sec.toLong, deciSeconds.toLong * 100000000L)

  final case class Red(
    count:       Int,
    oneExposure: Duration,
    gain:        GhostReadNoiseGain,
    binning:     GhostBinning
  ) extends GhostCamera {

    // Red camera, 1x1 binning
    // Slow readout: 95.1 sec
    // Fast readout: 20.7 sec
    override protected def oneByOneReadout: Duration =
      gain match {
        case GhostReadNoiseGain.SLOW_LOW   => duration(95, 1)
        case GhostReadNoiseGain.MEDIUM_LOW => duration(50, 0)
        case GhostReadNoiseGain.FAST_LOW |
             GhostReadNoiseGain.FAST_HIGH  => duration(20, 7)
      }
  }

  final case class Blue(
    count:       Int,
    oneExposure: Duration,
    gain:        GhostReadNoiseGain,
    binning:     GhostBinning
  ) extends GhostCamera {
    // Blue camera, 1x1 binning
    // Slow readout: 45.6 sec
    // Fast readout: 10.3 sec
    override protected def oneByOneReadout: Duration =
      gain match {
        case GhostReadNoiseGain.SLOW_LOW   => duration(45, 6)
        case GhostReadNoiseGain.MEDIUM_LOW => duration(24, 2)
        case GhostReadNoiseGain.FAST_LOW |
             GhostReadNoiseGain.FAST_HIGH  => duration(10, 3)
      }
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

  }

  object Blue {

    def fromConfig(c: Config): Blue =
      Blue(
        configToCount(c, Ghost.BLUE_EXPOSURE_COUNT_OBS_KEY),
        configToDuration(c, Ghost.BLUE_EXPOSURE_TIME_OBS_KEY),
        configToGain(c, Ghost.BLUE_READ_NOISE_GAIN_KEY, GhostReadNoiseGain.DEFAULT_BLUE),
        configToBinning(c, Ghost.BLUE_BINNING_KEY)
      )

  }

  implicit val EqualGhostCamera: Equal[GhostCamera] =
    Equal.equalA

}
