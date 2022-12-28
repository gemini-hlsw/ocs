// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.util

import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.gemini.ghost.Ghost
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostTarget
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GuideFiberState.Enabled
import edu.gemini.spModel.target.SPCoordinates
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.Asterism
import edu.gemini.spModel.target.env.AsterismType
import edu.gemini.spModel.target.env.UserTarget
import edu.gemini.spModel.target.obsComp.TargetObsComp

object AsterismEditUtil {

  private def switchAsterism(
    tn:  ISPObsComponent,
    toc: TargetObsComp,
    ast: Asterism,
    f:   SPTarget => Asterism
  ): Unit = {

    val newAsterism =
      f(ast.allSpTargets.head.clone)

    val userTargets =
      ast.allSpTargets
        .tail
        .map(u => new UserTarget(UserTarget.Type.other, u.clone))
        .toList
        .asImList

    val oldEnv = toc.getTargetEnvironment

    val env =
      oldEnv
        .setAsterism(newAsterism)
        .setUserTargets(userTargets.append(oldEnv.getUserTargets))

    toc.setTargetEnvironment(env)
    tn.setDataObject(toc)

  }

  private def switchToGhostAsterism(
    tn:  ISPObsComponent,
    toc: TargetObsComp,
    ast: Asterism,
    newType: AsterismType
  ): Unit =
    switchAsterism(
      tn,
      toc,
      ast,
      t => {
        val ghostTarget = GhostTarget(t, Enabled)
        newType match {
          case AsterismType.GhostDualTarget                     =>
            GhostAsterism.DualTarget(ghostTarget, GhostTarget.empty, None)
          case AsterismType.GhostTargetPlusSky                  =>
            GhostAsterism.TargetPlusSky(ghostTarget, new SPCoordinates, None)
          case AsterismType.GhostSkyPlusTarget                  =>
            GhostAsterism.SkyPlusTarget(new SPCoordinates, ghostTarget, None)
          case AsterismType.GhostHighResolutionTargetPlusSky    =>
            GhostAsterism.HighResolutionTargetPlusSky(ghostTarget, new SPCoordinates, GhostAsterism.PrvMode.PrvOff, None)
          case AsterismType.GhostHighResolutionTargetPlusSkyPrv =>
            GhostAsterism.HighResolutionTargetPlusSky(ghostTarget, new SPCoordinates, GhostAsterism.PrvMode.PrvOn, None)
          case _ =>
            GhostAsterism.SingleTarget(ghostTarget, None)
        }
      }
    )

  private def switchToDefaultAsterism(
    tn:  ISPObsComponent,
    toc: TargetObsComp,
    gst: GhostAsterism
  ): Unit =
    switchAsterism(
      tn,
      toc,
      gst,
      Asterism.Single(_)
    )

  def matchAsterismToInstrument(obs: ISPObservation): Unit = {
    println("\n\n\n\nmatchAsterismToInstrument\n")
    new RuntimeException().printStackTrace(System.out)

    for {
      tn <- Option(SPTreeUtil.findTargetEnvNode(obs))
      toc = tn.getDataObject.asInstanceOf[TargetObsComp]
      a   = toc.getAsterism
      i  <- Option(SPTreeUtil.findInstrument(obs))
    } (i.getType, a) match {

      case (SPComponentType.INSTRUMENT_GHOST, _: GhostAsterism) =>
        println(s"do nothing: $a")
        // do nothing

      case (SPComponentType.INSTRUMENT_GHOST, a)                =>
        val astType = i.getDataObject.asInstanceOf[Ghost].getPreferredAsterismType
        println(s"preferredAsterismType: $astType")
        switchToGhostAsterism(tn, toc, a, astType)

      case (_, g: GhostAsterism)                                =>
        switchToDefaultAsterism(tn, toc, g)

      case _                                                    =>
        // do nothing
    }
  }

}
