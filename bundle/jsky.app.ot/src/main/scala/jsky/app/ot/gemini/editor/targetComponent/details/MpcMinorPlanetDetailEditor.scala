package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ITarget

import javax.swing._
import java.awt._

final class MpcMinorPlanetDetailEditor extends TargetDetailEditor(ITarget.Tag.MPC_MINOR_PLANET) {

  val label = new JLabel();

  setLayout(new BorderLayout)
  add(label, BorderLayout.CENTER)

  override def edit(obsContext: GOption[ObsContext], spTarget: SPTarget): Unit = {
    super.edit(obsContext, spTarget)
    label.setText("MPC_MINOR_PLANET: " + spTarget.getTarget.toString);
  }

}
