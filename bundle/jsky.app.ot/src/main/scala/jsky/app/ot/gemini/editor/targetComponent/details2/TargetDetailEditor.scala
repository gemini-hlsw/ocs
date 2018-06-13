package jsky.app.ot.gemini.editor.targetComponent.details2

import javax.swing.JPanel
import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor

abstract class TargetDetailEditor extends JPanel with TelescopePosEditor[SPTarget] {
  def edit(ctx: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {
    require(ctx      != null, "obsContext should never be null")
    require(spTarget != null, "spTarget should never be null")
  }
}
