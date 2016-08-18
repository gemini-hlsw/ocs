package jsky.app.ot.visitlog

import jsky.app.ot.plugin.{OtActionPlugin, OtContext}
import edu.gemini.skycalc.ObservingNight
import jsky.util.gui.Resources

class ShowVisitLogAction extends OtActionPlugin("Observation Visit Log") {
  override val toolTip = "View the observation visit log for a given night"

  override def enabledFor(ctx: OtContext): Boolean = ctx.observingPeer.isDefined

  override def apply(ctx: OtContext, window: java.awt.Window): Unit =
    ctx.observingPeer.foreach { peer =>
      val night = new ObservingNight(peer.site, System.currentTimeMillis)

      val dialog = new VisitLogDialog(ctx.keyChain, peer)
      dialog.pack()
      dialog.location = window.getLocationOnScreen
      dialog.load(night)
      Resources.setOTFrameIcon(dialog.peer)
      dialog.visible  = true
    }
}
