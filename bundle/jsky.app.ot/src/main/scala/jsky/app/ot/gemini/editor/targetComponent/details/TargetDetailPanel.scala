package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.pot.sp.ISPNode
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.{ Option => GOption }
import java.awt.{ GridBagConstraints, GridBagLayout}
import javax.swing.{ JPanel}
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor
import scalaz.syntax.id._

final class TargetDetailPanel extends JPanel with TelescopePosEditor with ReentrancyHack {

  // This doodad will ensure that any change event coming from the SPTarget will get turned into
  // a call to `edit`, so we don't have to worry about that case everywhere. Everything from here
  // on down only needs to care about implementing `edit`.
  val tpw = new ForwardingTelescopePosWatcher(this)

  // Fields
  private[this] var tde: TargetDetailEditor = null;

  // Put it all together
  setLayout(new GridBagLayout)

  // Very sadly, we need to know whether or not calling `edit` will change the internal structure
  // of the editor. If so, the editor needs to do some extra work afterwards to make sure enabled
  // state is correct.
  def willCauseStructureChange(spTarget: SPTarget): Boolean = {
    tde == null || tde.getTag != spTarget.getTarget.getTag
  }

  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {

    // Create or replace the existing detail editor, if needed
    val tag = spTarget.getTarget.getTag
    if (tde == null || tde.getTag != tag) {
      if (tde != null) remove(tde)
      tde = TargetDetailEditor.forTag(tag)
      add(tde, new GridBagConstraints() <| { c =>
        c.fill = GridBagConstraints.BOTH
      })
    }
  
    // Forward the `edit` call.
    tpw.edit(obsContext, spTarget, node)
    tde.edit(obsContext, spTarget, node)

  }

}

