package jsky.app.ot.gemini.editor.targetComponent.details2

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{DropDownListBoxWidgetWatcher, DropDownListBoxWidget}

final class TargetTypeEditor extends DropDownListBoxWidget[AnyRef] with TelescopePosEditor with ReentrancyHack {

  private[this] var spt: SPTarget = new SPTarget

  sealed trait TargetType
  case object `Sidereal Target`       extends TargetType
  case object `Nonsidereal Target`    extends TargetType
  case object `Target of Opportunity` extends TargetType

  setChoices(Array[Object](`Sidereal Target`, `Nonsidereal Target`)) // TODO: allow TOO
  addWatcher(new DropDownListBoxWidgetWatcher[AnyRef] {
    def dropDownListBoxAction(w: DropDownListBoxWidget[AnyRef], index: Int, value: String): Unit =
      nonreentrant {
        w.getSelectedItem.asInstanceOf[TargetType] match {
          case `Sidereal Target`       => spt.setSidereal()
          case `Nonsidereal Target`    => spt.setNonSidereal()
          case `Target of Opportunity` => spt.setTOO()
        }
      }
  })

  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {
    spt = spTarget
    nonreentrant {
      setSelectedItem(
        spt.getNewTarget.fold(
          _ => `Target of Opportunity`,
          _ => `Sidereal Target`,
          _ => `Nonsidereal Target`
        )
      )
    }
  }

}