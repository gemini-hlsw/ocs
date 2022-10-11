// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package jsky.app.ot.nsp

import edu.gemini.pot.sp.ISPObsComponent
import edu.gemini.pot.sp.ISPObservation
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GuideFiberState.Enabled
import edu.gemini.spModel.gemini.ghost.GhostAsterism.GhostTarget
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.Asterism
import edu.gemini.spModel.target.env.UserTarget
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.util.SPTreeUtil
import scalaz.NonEmptyList

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
    ast: Asterism
  ): Unit =
    switchAsterism(
      tn,
      toc,
      ast,
      t => GhostAsterism.SingleTarget(GhostTarget(t, Enabled), None)
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

  def matchAsterismToInstrument(obs: ISPObservation): Unit =
    for {
      tn <- Option(SPTreeUtil.findTargetEnvNode(obs))
      toc = tn.getDataObject.asInstanceOf[TargetObsComp]
      a   = toc.getAsterism
      i  <- Option(SPTreeUtil.findInstrument(obs)).map(_.getType)
    } (i, a) match {
      case (SPComponentType.INSTRUMENT_GHOST, _: GhostAsterism) => // do nothing
      case (SPComponentType.INSTRUMENT_GHOST, a)                => switchToGhostAsterism(tn, toc, a)
      case (_, g: GhostAsterism)                                => switchToDefaultAsterism(tn, toc, g)
      case _                                                    => // do nothing
    }

}
