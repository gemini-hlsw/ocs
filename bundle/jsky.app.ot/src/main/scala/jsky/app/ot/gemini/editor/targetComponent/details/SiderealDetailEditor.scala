package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{GridBagConstraints, GridBagLayout}
import javax.swing.JComponent

import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.{HmsDegTarget, ITarget}
import jsky.app.ot.gemini.editor.targetComponent.{GuidingFeedbackEditor, MagnitudeEditor}

import scalaz.syntax.id._

final class SiderealDetailEditor extends TargetDetailEditor(ITarget.Tag.SIDEREAL) {


  import NumericPropertySheet.Prop

  val gfe     = new GuidingFeedbackEditor
  val mags    = new MagnitudeEditor {
    getComponent.asInstanceOf[JComponent].setBorder(titleBorder("Magnitudes"))
  }
  val props = NumericPropertySheet[HmsDegTarget]("Orbital Elements", _.getTarget.asInstanceOf[HmsDegTarget],
    Prop("∆ RA",     "mas/year", _.getPM1),
    Prop("∆ Dec",    "mas/year", _.getPM2),
    Prop("Epoch",    "JD",       _.getEpoch),
    Prop("Parallax", "arcsec",   _.getParallax),
    Prop("RV",       "km/sec",   _.getRV)
  )

  setLayout(new GridBagLayout)

  add(mags.getComponent, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 0
    c.fill = GridBagConstraints.VERTICAL
  })

  add(props, new GridBagConstraints <| { c =>
    c.gridx = 1
    c.gridy = 0
  })

  add(gfe.getComponent, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 1
    c.gridwidth = 2
    c.fill = GridBagConstraints.HORIZONTAL
  })

  override def edit(obsContext: GOption[ObsContext], spTarget: SPTarget): Unit = {
    super.edit(obsContext, spTarget)
    mags .edit(obsContext, spTarget)
    gfe  .edit(obsContext, spTarget)
    props.edit(obsContext, spTarget)
  }

}
