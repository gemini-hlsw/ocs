package edu.gemini.p2checker.rules.ghost

import edu.gemini.pot.ModelConverters._
import edu.gemini.p2checker.api.{IConfigRule, IP2Problems, IRule, ObservationElements, P2Problems, Problem}
import edu.gemini.p2checker.util.{AbstractConfigRule, SequenceRule}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.config2.{Config, ItemKey}
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.gemini.ghost.{Ghost, GhostScienceAreaGeometry}
import edu.gemini.spModel.target.env.AsterismType
import edu.gemini.spModel.target.offset.OffsetUtil

import scala.collection.JavaConverters._

object GhostRule extends IRule {
  object CoordinatesOutOfFOVRule extends IRule {
    override def check(elems: ObservationElements): IP2Problems = {
      val problems = new P2Problems()

      // COORDINATES, i.e. sky positions
      for {
        toc <- elems.getTargetObsComponentNode.asScalaOpt
        ctx <- elems.getObsContext.asScalaOpt
        env <- Option(ctx.getTargets)
        base <- ctx.getBaseCoordinates.asScalaOpt
        c <- env.getCoordinates.asScalaList
        if Coordinates.difference(base.toNewModel, c.coordinates).distance.toArcsecs > GhostScienceAreaGeometry.Radius.toArcsecs
      } problems.addError(GhostRule.Prefix + "CoordinatesOutOfRange", String.format(CoordinatesOutOfRange, c.getName), toc)

      // TARGETS
      for {
        toc <- elems.getTargetObsComponentNode.asScalaOpt
        ctx <- elems.getObsContext.asScalaOpt
        env <- Option(ctx.getTargets)
        base <- env.getAsterism.basePosition(None) //ctx.getBaseCoordinates.asScalaOpt
        t <- env.getAsterism.allTargets
        c <- t.coords(ctx.getSchedulingBlockStart.asScalaOpt.map(Long2long))
        if Coordinates.difference(base, c).distance.toArcsecs > GhostScienceAreaGeometry.Radius.toArcsecs
      } problems.addError(GhostRule.Prefix + "CoordinatesOutOfRange", String.format(CoordinatesOutOfRange, t.name), toc)

      problems
    }

    private val CoordinatesOutOfRange: String = "The coordinates for %s are out of range at the base position."
  }

  object OffsetsRule extends IRule {
    override def check(elements: ObservationElements): IP2Problems = {
      val problems = new P2Problems()

      for {
        ctx <- elements.getObsContext.asScalaOpt
        env <- Option(ctx.getTargets)
        if (env.getAsterism.asterismType == AsterismType.GhostSingleTarget && ctx.getSciencePositions.size > 1) ||
          (env.getAsterism.asterismType != AsterismType.GhostSingleTarget && !OffsetUtil.allOffsetPosLists(elements.getObservationNode).isEmpty)
      } problems.addError(GhostRule.Prefix + "AsterismDoesNotSupportOffsets",
        "GHOST only supports offsets in single target mode, and only a single offset.", elements.getSeqComponentNode)

      problems
    }
  }

  object CosmicRayExposureRule extends AbstractConfigRule {

    val id: String =
      GhostRule.Prefix + "CosmicRayExposureTime"

    val limitSeconds: Int =
      1800

    val message: String =
      s"Exposure time exceeds the recommended maximum ($limitSeconds seconds) due to cosmic ray contamination";

    override def check(config: Config, step: Int, elements: ObservationElements, state: Any): Problem = {
      def checkTime(key: ItemKey): Option[Problem] =
        Option(SequenceRule.getItem(config, classOf[Double], key)).collect {
          case d: java.lang.Double if d > limitSeconds =>
            new Problem(
              Problem.Type.WARNING,
              id,
              message,
              SequenceRule.getInstrumentOrSequenceNode(step, elements)
            )
        }

      checkTime(Ghost.RED_EXPOSURE_TIME_KEY)
        .orElse(checkTime(Ghost.BLUE_EXPOSURE_TIME_KEY))
        .orNull
    }

  }

  val ConfigRules: java.util.Collection[IConfigRule] =
    List[IConfigRule](CosmicRayExposureRule).asJava

  val Rules: List[IRule] = List(
    CoordinatesOutOfFOVRule,
    OffsetsRule
  )

  override def check(elems: ObservationElements): IP2Problems =
    (new SequenceRule(ConfigRules, null) :: Rules).foldLeft(IP2Problems.EMPTY) { (ps, rule) =>
      ps.appended(rule.check(elems))
    }

  val Prefix: String = "GhostRule_"
}