// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.gemini.ghost

import edu.gemini.spModel.config2.{Config, ItemKey}
import edu.gemini.spModel.seqcomp.GhostExpSeqComponent

import java.time.Duration

import scalaz.Equal

sealed trait GhostCamera extends Product with Serializable {

  def count: Int
  def time:  Duration

  def totalDuration: Duration =
    time.multipliedBy(count.toLong)

  def totalSeconds: Double =
    totalDuration.toMillis.toDouble / 1000.0

}

object GhostCamera {

  final case class Red(count: Int, time: Duration) extends GhostCamera
  final case class Blue(count: Int, time: Duration) extends GhostCamera

  def red(count: Int, time: Duration): GhostCamera =
    Red(count, time)

  private def secondsToDuration(secs: Double): Duration =
    Duration.ofMillis(Math.round(secs * 1000.0))

  private def configToDuration(config: Config, key: ItemKey): Duration =
    secondsToDuration(
      Option(config.getItemValue(key)).collect {
        case d: java.lang.Double => d.doubleValue
      }.getOrElse(0.0)
    )

  private def configToCount(config: Config, key: ItemKey): Int =
    Option(config.getItemValue(key)).collect {
      case i: java.lang.Integer => i.intValue
    }.getOrElse(1)

  object Red {

    def fromGhostComponent(g: Ghost): Red =
      Red(g.getRedExposureCount, secondsToDuration(g.getRedExposureTime))

    def fromGhostSeqComponent(g: GhostExpSeqComponent): Red =
      Red(g.getRedExposureCount, secondsToDuration(g.getRedExposureTime))

    def fromConfig(c: Config): Red =
      Red(
        configToCount(c, Ghost.RED_EXPOSURE_COUNT_KEY),
        configToDuration(c, Ghost.RED_EXPOSURE_TIME_KEY)
      )


  }

  def blue(count: Int, time: Duration): GhostCamera =
    Blue(count, time)

  object Blue {

    def fromGhostComponent(g: Ghost): Blue =
      Blue(g.getBlueExposureCount, secondsToDuration(g.getBlueExposureTime))

    def fromGhostSeqComponent(g: GhostExpSeqComponent): Blue =
      Blue(g.getBlueExposureCount, secondsToDuration(g.getBlueExposureTime))

    def fromConfig(c: Config): Blue =
      Blue(
        configToCount(c, Ghost.BLUE_EXPOSURE_COUNT_KEY),
        configToDuration(c, Ghost.BLUE_EXPOSURE_TIME_KEY)
      )

  }

  implicit val EqualGhostCamera: Equal[GhostCamera] =
    Equal.equalA

}
