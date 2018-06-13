package jsky.app.ot.gemini.editor.targetComponent.details2

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{None => GNone, Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.{SPSkyObject, TelescopePosWatcher, WatchablePos}
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor

final class ForwardingTelescopePosWatcher[T <: SPSkyObject](tpe: TelescopePosEditor[T],
                                                            initializer: () => T)
  extends TelescopePosEditor[T] with TelescopePosWatcher {

  private[this] var spt:  T = initializer()
  private[this] var ctx:  GOption[ObsContext] = GNone.instance[ObsContext]
  private[this] var node: ISPNode = null

  def edit(obsContext: GOption[ObsContext], spSkyObject: T, ispNode: ISPNode): Unit = {
    require(obsContext != null, "obsContext should never be null")
    require(spSkyObject != null, "spSkyObject should never be null")

    // If this is a new target, switch our watchers
    if (spt != spSkyObject) {
      spt.deleteWatcher(this)
      spSkyObject.addWatcher(this)
    }

    // Remember the context and target so `telescopePosUpdate` can call `edit`
    ctx  = obsContext
    spt  = spSkyObject
    node = ispNode

  }

  def telescopePosUpdate(tp: WatchablePos): Unit =
    tpe.edit(ctx, spt, node)

}
