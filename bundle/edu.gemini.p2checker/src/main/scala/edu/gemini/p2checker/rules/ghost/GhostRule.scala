package edu.gemini.p2checker.rules.ghost

import edu.gemini.pot.ModelConverters._
import edu.gemini.p2checker.api.{IP2Problems, IRule, ObservationElements, P2Problems}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.gemini.ghost.GhostScienceAreaGeometry
import edu.gemini.spModel.target.env.AsterismType

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
        if Coordinates.difference(base.toNewModel, c.coordinates).distance.toArcsecs > GhostScienceAreaGeometry.radius.toArcsecs
      } problems.addError(GhostRule.Prefix + "CoordinatesOutOfRange", String.format(CoordinatesOutOfRange, c.getName), toc)

      // TARGETS
      for {
        toc <- elems.getTargetObsComponentNode.asScalaOpt
        ctx <- elems.getObsContext.asScalaOpt
        env <- Option(ctx.getTargets)
        base <- env.getAsterism.basePosition(None) //ctx.getBaseCoordinates.asScalaOpt
        t <- env.getAsterism.allTargets
        c <- t.coords(ctx.getSchedulingBlockStart.asScalaOpt.map(Long2long))
        if Coordinates.difference(base, c).distance.toArcsecs > GhostScienceAreaGeometry.radius.toArcsecs
      } problems.addError(GhostRule.Prefix + "CoordinatesOutOfRange", String.format(CoordinatesOutOfRange, t.name), toc)

      problems
    }

    private val CoordinatesOutOfRange: String = "The coordinates for %s are out of range at the base position."
  }

  object OffsetsAllowedRule extends IRule {
    override def check(elements: ObservationElements): IP2Problems = {
      val problems = new P2Problems()

      for {
        ctx <- elements.getObsContext.asScalaOpt
        env <- Option(ctx.getTargets)
        if env.getAsterism.asterismType != AsterismType.GhostSingleTarget
        if !ctx.getSciencePositions.isEmpty
      } problems.addError(GhostRule.Prefix + "AsterismDoesNotSupportOffsets",
        "GHOST only supports offsets in standard resolution single target mode.", elements.getSeqComponentNode)

      problems
    }
  }

  object OffsetLimitRule extends IRule {
    override def check(elements: ObservationElements): IP2Problems = {
      val problems = new P2Problems()

      for {
        ctx <- elements.getObsContext.asScalaOpt
        env <- Option(ctx.getTargets)
        if env.getAsterism.asterismType == AsterismType.GhostSingleTarget
        if ctx.getSciencePositions.size > 1
      } problems.addError(GhostRule.Prefix + "AsterismDoesNotSupportOffsets",
        "GHOST only supports one offset in standard resolution single target mode..", elements.getSeqComponentNode)

      problems
    }
  }

  override def check(elems: ObservationElements): IP2Problems = {
    val probs: P2Problems = new P2Problems()
    probs.append(CoordinatesOutOfFOVRule.check(elems))
    probs.append(OffsetsAllowedRule.check(elems))
    probs.append(OffsetLimitRule.check(elems))
    probs
  }

  val Prefix: String = "GhostRule_"
}