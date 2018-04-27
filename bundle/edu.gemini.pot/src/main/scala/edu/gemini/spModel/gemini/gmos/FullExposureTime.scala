package edu.gemini.spModel.gemini.gmos

import edu.gemini.spModel.config2.{ Config, ItemKey }
import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary.OBS_TYPE_ITEM
import edu.gemini.spModel.gemini.gmos.InstGmosCommon.{ NS_STEP_COUNT_PROP_NAME, NUM_NS_CYCLES_PROP, USE_NS_PROP }
import edu.gemini.spModel.obscomp.InstConstants.{ EXPOSURE_TIME_PROP, OBSERVE_TYPE_PROP }

import edu.gemini.spModel.seqcomp.SeqConfigNames.{ INSTRUMENT_CONFIG_NAME, OBSERVE_CONFIG_NAME }

import java.time.Duration

import scalaz._
import Scalaz._

/** Adds support for calculating and inserting "full" exposure time for GMOS,
  * even for nod and shuffle steps.  Nod and shuffle takes a number of exposures
  * per step (cycle count * offset position step count) so the full exposure
  * time is the nominal instrument exposure time times the nod and shuffle count.
  */
object FullExposureTime {

  private object keys {
    val StepCount:    ItemKey = new ItemKey(s"$INSTRUMENT_CONFIG_NAME:$NS_STEP_COUNT_PROP_NAME")
    val CycleCount:   ItemKey = new ItemKey(s"$INSTRUMENT_CONFIG_NAME:${NUM_NS_CYCLES_PROP.getName}")
    val UseNs:        ItemKey = new ItemKey(s"$INSTRUMENT_CONFIG_NAME:${USE_NS_PROP.getName}")
    val ExposureTime: ItemKey = new ItemKey(s"$INSTRUMENT_CONFIG_NAME:$EXPOSURE_TIME_PROP")
    val ObsType:      ItemKey = OBS_TYPE_ITEM.key
  }

  private implicit class ConfigOps(c: Config) {
    import edu.gemini.spModel.config2.ConfigSyntax._

    import keys._

    def useNs: Boolean =
     c.unsafeJavaBoolean(UseNs)

    def isNsObsType: Boolean =
      InstGmosCommon.isNodAndShuffleableObsType(c.unsafeExtract[String](ObsType))

    def nsStepCount: Int =
      c.unsafeJavaInt(StepCount)

    def nsCycleCount: Int =
      c.unsafeJavaInt(CycleCount)

    def exposureTime: Duration =
      Duration.ofMillis((c.unsafeJavaDouble(ExposureTime) * 1000.0).round)

    def fullExposureTime: Duration =
      exposureTime.multipliedBy(if (useNs && isNsObsType) nsStepCount * nsCycleCount else 1)
  }

  /** Calculates full exposure time, even for nod and shuffle steps. */
  def asDuration(c: Config): Duration =
    c.fullExposureTime

  def asDoubleSecs(c: Config): Double =
    asDuration(c).toMillis.toDouble / 1000.0
}
