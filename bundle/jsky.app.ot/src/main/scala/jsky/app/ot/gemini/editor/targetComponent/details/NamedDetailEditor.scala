package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.BorderLayout
import javax.swing.JLabel

import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ITarget.Tag

final class NamedDetailEditor extends TargetDetailEditor(Tag.NAMED) {

  val label = new JLabel()

  setLayout(new BorderLayout())
  add(label, BorderLayout.CENTER)

  override def edit(ctx: GOption[ObsContext], spTarget: SPTarget): Unit = {
    super.edit(ctx, spTarget)
    label.setText("NAMED: " + spTarget.getTarget().toString())
  }

}
