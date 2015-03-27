package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ITarget
import edu.gemini.shared.util.immutable.{ Option => GOption }
import java.awt.{Insets, GridBagConstraints, GridBagLayout, Color}
import javax.swing.{BorderFactory, JPanel, JLabel}
import edu.gemini.spModel.target.system.ITarget.Tag
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import jsky.util.gui.{DropDownListBoxWidgetWatcher, DropDownListBoxWidget}
import scalaz.syntax.id._

final class TargetDetailPanel extends JPanel with TelescopePosEditor with ReentrancyHack {

  // This doodad will ensure that any change event coming from the SPTarget will get turned into
  // a call to `edit`, so we don't have to worry about that case everywhere. Everything from here
  // on down only needs to care about implementing `edit`.
  val tpw = new ForwardingTelescopePosWatcher(this)

  // Dropdown to change the target type (!)
  val targetType = new DropDownListBoxWidget {
    setChoices(ITarget.Tag.values.asInstanceOf[Array[AnyRef]])
    addWatcher(new DropDownListBoxWidgetWatcher {
      def dropDownListBoxAction(w: DropDownListBoxWidget, index: Int, value: String): Unit =
        nonreentrant {
          spt.setTargetType(w.getSelectedItem.asInstanceOf[ITarget.Tag])
        }
    })
  }

  // Fields, never null
  private[this] var spt: SPTarget = new SPTarget
  private[this] var tde: TargetDetailEditor = TargetDetailEditor.forTag(Tag.SIDEREAL)

  // Styling and child components
  setBorder(BorderFactory.createLineBorder(Color.RED))
  setLayout(new GridBagLayout)
  add(new JLabel("Target Type"), new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 0
    c.fill = GridBagConstraints.HORIZONTAL
    c.insets = new Insets(0, 0, 0, 5)
  })
  add(targetType, new GridBagConstraints <| { c =>
    c.gridx = 1
    c.gridy = 0
    c.insets = new Insets(0, 5, 0, 0)
    c.fill = GridBagConstraints.HORIZONTAL
  })

  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget): Unit = {

    // Create or replace the existing detail editor, if needed
    val tag = spTarget.getTarget.getTag
    if (tde.getTag != tag) {
      remove(tde)
      tde = TargetDetailEditor.forTag(tag)
      add(tde, new GridBagConstraints() <| { c =>
        c.gridx = 0
        c.gridy = 1
        c.gridwidth = 2
        c.insets = new Insets(15, 0, 0, 0)
        c.fill = GridBagConstraints.BOTH
      })
    }
  
    // Forward the `edit` call.
    tpw.edit(obsContext, spTarget)
    tde.edit(obsContext, spTarget)
  
    // Local updates
    spt = spTarget
    nonreentrant {
      targetType.setSelectedItem(spt.getTarget.getTag)
    }

  }

}



