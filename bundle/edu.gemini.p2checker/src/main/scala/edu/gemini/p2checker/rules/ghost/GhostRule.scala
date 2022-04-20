package edu.gemini.p2checker.rules.ghost

import edu.gemini.p2checker.api.{IConfigRule, IP2Problems, IRule, ObservationElements, P2Problems, Problem}
import edu.gemini.p2checker.util.{AbstractConfigRule, SequenceRule}
import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.config2.{Config, ItemKey}
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.gemini.ghost.{Ghost, GhostAsterism, GhostIfuPatrolField}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.env.{Asterism, AsterismType}
import edu.gemini.spModel.target.offset.OffsetUtil

import java.time.Instant

import scala.collection.JavaConverters._

object GhostRule extends IRule {
  object CoordinatesOutOfFOVRule extends IRule {
    val id: String = GhostRule.Prefix + "CoordinatesOutOfRange"

    def checkOne(
      name:  String,
      node:  ISPObsComponent,
      base:  Coordinates,
      ifu:   Option[Coordinates],
      field: => GhostIfuPatrolField
    ): List[Problem] =
      ifu.flatMap { p =>
        if (field.inRange(Coordinates.difference(base, p).offset)) None
        else Some(new Problem(Problem.Type.ERROR, id, String.format(CoordinatesOutOfRange, name), node))
      }.toList

    def checkBoth(
      ctx:  ObsContext,
      node: ISPObsComponent,
      base: Coordinates,
      ifu1: Option[Coordinates],
      ifu2: Option[Coordinates]
    ): List[Problem] =
      checkOne("IFU1", node, base, ifu1, GhostIfuPatrolField.ifu1(ctx)) ++
      checkOne("IFU2", node, base, ifu2, GhostIfuPatrolField.ifu2(ctx))

    def checkAsterism(
      ast:  Asterism,
      ctx:  ObsContext,
      node: ISPObsComponent,
      base: Coordinates,
      when: Option[Instant]
    ): List[Problem] =
      ast match {
        case GhostAsterism.SingleTarget(t, _)                   =>
          checkOne("IFU1", node, base, t.coordinates(when), GhostIfuPatrolField.ifu1(ctx))
        case GhostAsterism.DualTarget(t1, t2, _)                =>
          checkBoth(ctx, node, base, t1.coordinates(when), t2.coordinates(when))
        case GhostAsterism.TargetPlusSky(t, s, _)               =>
          checkBoth(ctx, node, base, t.coordinates(when), Some(s.coordinates))
        case GhostAsterism.SkyPlusTarget(s, t, _)               =>
          checkBoth(ctx, node, base, Some(s.coordinates), t.coordinates(when))
        case hr@GhostAsterism.HighResolutionTargetPlusSky(t, _, _) =>
          // The sky position is taken from the user configuration, but the
          // actual SRIFU2 is positioned just south of that.  For the range
          // check we need to use the actual SRIFU2 location.
          checkBoth(ctx, node, base, t.coordinates(when), Some(hr.srifu2(ctx.getPositionAngle)))
        case _                                                  =>
          Nil
      }

    override def check(elems: ObservationElements): IP2Problems = {
      val problems = new P2Problems()

      for {
        toc  <- elems.getTargetObsComponentNode.asScalaOpt
        ctx  <- elems.getObsContext.asScalaOpt
        env  <- Option(ctx.getTargets)
        when = ctx.getSchedulingBlockStart.asScalaOpt.map(t => Instant.ofEpochMilli(t))
        base <- env.getAsterism.basePosition(when)
      } checkAsterism(env.getAsterism, ctx, toc, base, when).foreach(problems.append)

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
      s"Exposure time exceeds the recommended maximum ($limitSeconds seconds) due to cosmic ray contamination"

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