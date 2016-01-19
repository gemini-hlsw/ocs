package jsky.app.ot.gemini.editor.targetComponent.details2

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ITarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{DropDownListBoxWidgetWatcher, DropDownListBoxWidget}

final class TargetTypeEditor extends DropDownListBoxWidget[AnyRef] with TelescopePosEditor with ReentrancyHack {

  private[this] var spt: SPTarget = new SPTarget

  setChoices(ITarget.Tag.values.asInstanceOf[Array[AnyRef]])
  addWatcher(new DropDownListBoxWidgetWatcher[AnyRef] {
    def dropDownListBoxAction(w: DropDownListBoxWidget[AnyRef], index: Int, value: String): Unit =
      nonreentrant {
        spt.setTargetType(w.getSelectedItem.asInstanceOf[ITarget.Tag])
      }
  })

  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {
    spt = spTarget
    nonreentrant {
      setSelectedItem(spt.getTarget.getTag)
    }
  }

}