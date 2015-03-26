package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ITarget
import jsky.app.ot.gemini.editor.targetComponent.GuidingFeedbackEditor
import jsky.app.ot.gemini.editor.targetComponent.ProperMotionEditor
import jsky.app.ot.gemini.editor.targetComponent.TrackingEditor

import java.awt._

final class SiderealDetailEditor extends TargetDetailEditor(ITarget.Tag.SIDEREAL) {

  private val ped = new ProperMotionEditor
  private val gfe = new GuidingFeedbackEditor
  private val te  = new TrackingEditor

  setLayout(new BorderLayout)
  add(ped.getComponent, BorderLayout.CENTER)
  add(te .getComponent, BorderLayout.EAST)
  add(gfe.getComponent, BorderLayout.SOUTH)

  override def edit(obsContext: GOption[ObsContext], spTarget: SPTarget): Unit = {
    super.edit(obsContext, spTarget)
    ped  .edit(obsContext, spTarget)
    te   .edit(obsContext, spTarget)
    gfe  .edit(obsContext, spTarget)
  }

}
