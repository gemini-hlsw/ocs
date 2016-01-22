package jsky.app.ot.gemini.editor.targetComponent.details2

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.{HmsDegTarget, ITarget}
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{DropDownListBoxWidgetWatcher, DropDownListBoxWidget}

final class TargetTypeEditor extends DropDownListBoxWidget[AnyRef] with TelescopePosEditor with ReentrancyHack {

  private[this] var spt: SPTarget = new SPTarget

  setChoices(Array[Object]("Sidereal Target", "Nonsidereal Target"))
  addWatcher(new DropDownListBoxWidgetWatcher[AnyRef] {
    def dropDownListBoxAction(w: DropDownListBoxWidget[AnyRef], index: Int, value: String): Unit =
      nonreentrant {
        w.getSelectedItem.asInstanceOf[String] match {
          case "Sidereal Target"    => spt.setTargetType(ITarget.Tag.SIDEREAL)
          case "Nonsidereal Target" => spt.setTargetType(ITarget.Tag.JPL_MINOR_BODY)
        }
      }
  })

  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {
    spt = spTarget
    nonreentrant {
      setSelectedItem(spt.getTarget match {
        case s: HmsDegTarget => "Sidereal Target"
        case _               => "Nonsidereal Target"
      })
    }
  }

}