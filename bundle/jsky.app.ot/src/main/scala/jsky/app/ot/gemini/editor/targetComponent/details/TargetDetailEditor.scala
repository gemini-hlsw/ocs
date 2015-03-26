package jsky.app.ot.gemini.editor.targetComponent.details

import javax.swing.JPanel
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ITarget.Tag
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor

object TargetDetailEditor {

  def forTag(t: Tag): TargetDetailEditor =
    t match {
      case Tag.JPL_MINOR_BODY   => new JplMinorBodyDetailEditor
      case Tag.MPC_MINOR_PLANET => new MpcMinorPlanetDetailEditor
      case Tag.NAMED            => new NamedDetailEditor
      case Tag.SIDEREAL         => new SiderealDetailEditor
    }

}

abstract class TargetDetailEditor(val getTag: Tag) extends JPanel with TelescopePosEditor {

  def edit(ctx: GOption[ObsContext], spTarget: SPTarget): Unit = {

    // Verify that our target has the correct tag. Note that we're racing here but there's not
    // much we can do about it. We just have to assume that nobody is replacing the ITarget
    // right now. If this turns out to be wrong then there will be problems down the line.
    val tag = spTarget.getTarget().getTag()
    require(tag == getTag, "Expected " + getTag + ", received " + tag)

  }

}
