package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{GridBagConstraints, GridBagLayout}
import javax.swing.JPanel

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters.ScalaOptionOps
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import jsky.app.ot.gemini.editor.targetComponent.{GuidingFeedbackEditor, TelescopePosEditor}

import scalaz.syntax.id._

final class TargetDetailPanel extends JPanel with TelescopePosEditor with ReentrancyHack {

  // This doodad will ensure that any change event coming from the SPTarget will get turned into
  // a call to `edit`, so we don't have to worry about that case everywhere. Everything from here
  // on down only needs to care about implementing `edit`.
  val tpw = new ForwardingTelescopePosWatcher(this)

  // Fields
  private[this] var tde: TargetDetailEditor  = null

  def curDetailEditor: Option[TargetDetailEditor] = Option(tde)

  def curDetailEdiorJava: GOption[TargetDetailEditor] = curDetailEditor.asGeminiOpt

  val source = new SourceDetailsEditor
  val gfe    = new GuidingFeedbackEditor

  // Put it all together
  setLayout(new GridBagLayout)
  add(source.peer, new GridBagConstraints() <| { c =>
    c.anchor    = GridBagConstraints.NORTH
    c.gridx     = 1
    c.gridy     = 0
    c.weighty   = 1
    c.fill      = GridBagConstraints.VERTICAL
  })
  add(gfe.getComponent, new GridBagConstraints() <| { c =>
    c.gridx     = 0
    c.gridy     = 1
    c.weightx   = 1
    c.gridwidth = 2
    c.fill      = GridBagConstraints.HORIZONTAL
  })

  def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {

    // Create or replace the existing detail editor, if needed
    val tag = spTarget.getTarget.getTag
    if (tde == null || tde.getTag != tag) {
      if (tde != null) remove(tde)
      tde = TargetDetailEditor.forTag(tag)
      add(tde, new GridBagConstraints() <| { c =>
        c.gridx = 0
        c.gridy = 0
        c.weightx = 1
        c.fill = GridBagConstraints.HORIZONTAL
      })
      revalidate()  // REL-2331?  revalidate/repaint for good measure
      repaint()
    }

    // Forward the `edit` call.
    tpw.   edit(obsContext, spTarget, node)
    tde.   edit(obsContext, spTarget, node)
    gfe.   edit(obsContext, spTarget, node)
    source.edit(obsContext, spTarget, node)

  }

}

