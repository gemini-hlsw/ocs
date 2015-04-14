package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.BorderLayout

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.NamedTarget
import edu.gemini.spModel.target.system.ITarget.Tag
import jsky.util.gui.{DropDownListBoxWidgetWatcher, DropDownListBoxWidget}

final class NamedDetailEditor extends TargetDetailEditor(Tag.NAMED) {

  val solarObject = new DropDownListBoxWidget {
    setChoices(NamedTarget.SolarObject.values.asInstanceOf[Array[AnyRef]])
    addWatcher(new DropDownListBoxWidgetWatcher {
      def dropDownListBoxAction(w: DropDownListBoxWidget, index: Int, value: String) {
        // TODO
      }
    })
  }


  setLayout(new BorderLayout())
  add(solarObject, BorderLayout.CENTER)

  override def edit(ctx: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {
    super.edit(ctx, spTarget, node)
  }

}
