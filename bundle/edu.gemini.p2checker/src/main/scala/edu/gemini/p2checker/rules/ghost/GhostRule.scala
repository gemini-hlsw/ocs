package edu.gemini.p2checker.rules.ghost

import edu.gemini.p2checker.api.{IP2Problems, IRule, ObservationElements, P2Problems}
import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.gemini.ghost.GhostScienceAreaGeometry

object GhostRule extends IRule {
  object CoordinatesOutOfFOVRule extends IRule {
    override def check(elems: ObservationElements): IP2Problems = {
      val problems = new P2Problems()
      println("****** CHECKING GHOST")
      for {
        ctx <- elems.getObsContext.asScalaOpt
        env <- Option(ctx.getTargets)
      } {
        println("***** DATA *****")
        println(s"targets: ${env.getTargets.size()}, coordinates: ${env.getCoordinates.size()}")
        println(s"sptargets: ${env.getAsterism.allSpTargets.size}, targets: ${env.getAsterism.allTargets.size}, spcoords: ${env.getAsterism.allSpCoordinates.size}")
      }
      for {
        ctx <- elems.getObsContext.asScalaOpt
        base <- ctx.getBaseCoordinates.asScalaOpt
        env <- Option(ctx.getTargets)
        c <- env.getCoordinates.asScalaList
      } {
        println("***** DATA *****")
        println(s"targets: ${env.getTargets.size()}, coordinates: ${env.getCoordinates.size()}")
        println(s"sptargets: ${env.getAsterism.allSpTargets.size}, targets: ${env.getAsterism.allTargets.size}, spcoords: ${env.getAsterism.allSpCoordinates.size}")
        println(s"****** ITEM: ${c.getName} DIST IN AM: ${Coordinates.difference(base.toNewModel, c.coordinates).distance.toArcmins}")
      }
      for {
        toc <- elems.getTargetObsComponentNode.asScalaOpt
        ctx <- elems.getObsContext.asScalaOpt
        base <- ctx.getBaseCoordinates.asScalaOpt
        env <- Option(ctx.getTargets)
        c <- env.getCoordinates.asScalaList
        if Coordinates.difference(base.toNewModel, c.coordinates).distance.toArcsecs > GhostScienceAreaGeometry.radius.toArcsecs
      } {
        println("****** PROBLEM")
        problems.addError(GhostRule.Prefix + "CoordinatesOutOfRange", String.format(GhostRule.CoordinatesOutOfRange, c.getName), toc)
      }
      println("******* DONE CHECKING GHOST")
      problems
    }
  }

  override def check(elems: ObservationElements): IP2Problems = {
    val probs: P2Problems = new P2Problems()
    probs.append(CoordinatesOutOfFOVRule.check(elems))
    probs
  }

  val Prefix: String = "GhostRule_"
  val CoordinatesOutOfRange: String = "The coordinates for %s are out of range of the base position."
}