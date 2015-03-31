package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.{ Option => GOption }
import java.awt.{ GridBagConstraints, GridBagLayout, Color}
import javax.swing.{BorderFactory, JPanel }
import edu.gemini.spModel.target.system.ITarget.Tag
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import scalaz.syntax.id._

final class TargetDetailPanel extends JPanel with TelescopePosEditor with ReentrancyHack {

  // This doodad will ensure that any change event coming from the SPTarget will get turned into
  // a call to `edit`, so we don't have to worry about that case everywhere. Everything from here
  // on down only needs to care about implementing `edit`.
  val tpw = new ForwardingTelescopePosWatcher(this)

  // Fields, never null
  private[this] var tde: TargetDetailEditor = TargetDetailEditor.forTag(Tag.SIDEREAL)

  // Put it all together
  setBorder(BorderFactory.createLineBorder(Color.RED))
  setLayout(new GridBagLayout)

  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget): Unit = {

    // Create or replace the existing detail editor, if needed
    val tag = spTarget.getTarget.getTag
    if (tde.getTag != tag) {
      remove(tde)
      tde = TargetDetailEditor.forTag(tag)
      add(tde, new GridBagConstraints() <| { c =>
        c.gridx = 0
        c.gridy = 0
        c.fill = GridBagConstraints.BOTH
      })
    }
  
    // Forward the `edit` call.
    tpw.edit(obsContext, spTarget)
    tde.edit(obsContext, spTarget)

  }

}

