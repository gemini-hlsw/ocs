package jsky.app.ot.gemini.editor.targetComponent.details2

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption, None => GNone }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.TelescopePosWatcher
import edu.gemini.spModel.target.WatchablePos
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor

/** This is only used with the TargetDetailPanel, so we don't need to worry
  * about SPCoordinates.
  */
final class ForwardingTelescopePosWatcher(tpe: TelescopePosEditor[SPTarget])
  extends TelescopePosEditor[SPTarget] with TelescopePosWatcher {

  private[this] var spt:  SPTarget = new SPTarget
  private[this] var ctx:  GOption[ObsContext] = GNone.instance[ObsContext]
  private[this] var node: ISPNode = null

  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, ispNode: ISPNode): Unit = {
    require(obsContext != null, "obsContext should never be null")
    require(spTarget   != null, "spTarget should never be null")

    // If this is a new target, switch our watchers
    if (spt != spTarget) {
      spt.deleteWatcher(this)
      spTarget.addWatcher(this)
    }

    // Remember the context and target so `telescopePosUpdate` can call `edit`
    ctx  = obsContext
    spt  = spTarget
    node = ispNode

  }

  def telescopePosUpdate(tp: WatchablePos): Unit =
    tpe.edit(ctx, spt, node)

}
