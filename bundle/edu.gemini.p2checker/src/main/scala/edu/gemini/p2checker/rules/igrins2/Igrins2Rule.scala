package edu.gemini.p2checker.rules.igrins2

import edu.gemini.p2checker.api._
import edu.gemini.p2checker.util.SequenceRule
import edu.gemini.spModel.config2.Config
import edu.gemini.spModel.gemini.igrins2.Igrins2
import squants.time.TimeConversions._

import scala.collection.JavaConverters._

object Igrins2Rule extends IRule {
  object ExposureTimeRule extends IConfigRule {
    val TooLong = s"${Prefix}EXPOSURE_TIME_RULE_MAX_MESSAGE"
    val TooShort = s"${Prefix}EXPOSURE_TIME_RULE_MIN_MESSAGE"

    override def check(config: Config, step: Int, elements: ObservationElements, state: Any): Problem = {
      val expTime = SequenceRule.getExposureTime(config)
      Option (expTime).flatMap { time =>
        val expTime = time.doubleValue().seconds
        if (expTime < Igrins2.MinExposureTime) {
          val msg = f"Exposure time (${time}%.1f) below minimum of (${Igrins2.MinExposureTime.toSeconds}%.1f)."
          Option(new Problem(IRule.ERROR, TooShort, msg, SequenceRule.getInstrumentOrSequenceNode(step, elements, config)))
        } else if (expTime > Igrins2.MaxExposureTime) {
          val msg = f"Exposure time (${time}%.1f) above maximum of (${Igrins2.MaxExposureTime.toSeconds}%.1f)."
          Option(new Problem(IRule.ERROR, TooLong, msg, SequenceRule.getInstrumentOrSequenceNode(step, elements, config)))
        }
        else None
      }.orNull
    }

    override def getMatcher: IConfigMatcher = IConfigMatcher.ALWAYS
  }

  val ConfigRules: java.util.Collection[IConfigRule] =
    List[IConfigRule](ExposureTimeRule).asJava

  val Rules: List[IRule] = List(
  )

  override def check(elems: ObservationElements): IP2Problems =
    (new SequenceRule(ConfigRules, null) :: Rules).foldLeft(IP2Problems.EMPTY) { (ps, rule) =>
      ps.appended(rule.check(elems))
    }

  val Prefix: String = "Igrins2Rule_"
}