package jsky.app.ot.gemini.editor.targetComponent.details2

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.core.Target
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{TextBoxWidget, TextBoxWidgetWatcher}

import scalaz.Scalaz._

final class TooNameEditor extends TelescopePosEditor[SPTarget] with ReentrancyHack {
  private[this] var spt = new SPTarget // never null

  val name = new TextBoxWidget <| { w =>
    w.setColumns(20)
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {
      override def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant(spt.setTarget(Target.name.set(spt.getTarget, tbwe.getValue)))
    })
  }

  def edit(ctx: GOption[ObsContext], target: SPTarget, node: ISPNode): Unit = {
    this.spt = target
    nonreentrant {
      name.setText(Target.name.get(spt.getTarget))
    }
  }

}
