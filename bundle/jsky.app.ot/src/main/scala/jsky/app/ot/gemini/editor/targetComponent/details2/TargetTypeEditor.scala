package jsky.app.ot.gemini.editor.targetComponent.details2

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.too.TooType
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{DropDownListBoxWidgetWatcher, DropDownListBoxWidget}

final class TargetTypeEditor extends DropDownListBoxWidget[AnyRef] with TelescopePosEditor[SPTarget] with ReentrancyHack {

  private[this] var spt: SPTarget = new SPTarget

  sealed trait TargetType
  case object `Sidereal Target`       extends TargetType
  case object `Nonsidereal Target`    extends TargetType
  case object `Target of Opportunity` extends TargetType

  object TargetType {
    val    tooChoices = Array[Object](`Sidereal Target`, `Nonsidereal Target`, `Target of Opportunity`)
    val nonTooChoices = Array[Object](`Sidereal Target`, `Nonsidereal Target`)
    def choices(node: ISPNode): Array[Object] =
      Option(node).fold(TooType.none)(_.getProgram.getDataObject.asInstanceOf[SPProgram].getTooType) match {
        case TooType.rapid | TooType.standard => tooChoices
        case TooType.none                     => nonTooChoices
      }
  }

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
    setChoices(TargetType.choices(node))
    spt = spTarget
    nonreentrant {
      setSelectedItem(
        spt.getTarget.fold(
          _ => `Target of Opportunity`,
          _ => `Sidereal Target`,
          _ => `Nonsidereal Target`
        )
      )
    }
  }

}