package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.shared.util.immutable.{ Option => GOption, None => GNone }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.TelescopePosWatcher
import edu.gemini.spModel.target.WatchablePos
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor

final class ForwardingTelescopePosWatcher(tpe: TelescopePosEditor)
  extends TelescopePosEditor with TelescopePosWatcher {

  private[this] var ot: Option[SPTarget] = None
  private[this] var ctx: GOption[ObsContext] = GNone.instance[ObsContext]

  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget ) {
    require(obsContext != null, "obsContext should never be null")
    require(spTarget   != null, "spTarget should never be null")

    // If this is a new target, switch our watchers
    if (ot != Some(spTarget)) {
      ot.foreach(_.deleteWatcher(this))
      spTarget.addWatcher(this)
    }

    // Remember the context and target so `telescopePosUpdate` can call `edit`
    ctx = obsContext
    ot  = Option(spTarget)

  }

  def telescopePosUpdate(tp: WatchablePos) {
    ot.foreach(tpe.edit(ctx, _))
  }

}
