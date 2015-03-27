package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{ GridBagConstraints, GridBagLayout}
import javax.swing.JComponent

import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ConicTarget
import edu.gemini.spModel.target.system.ITarget.Tag

import jsky.app.ot.gemini.editor.targetComponent.MagnitudeEditor

import scalaz.syntax.id._

final class JplMinorBodyDetailEditor extends TargetDetailEditor(Tag.JPL_MINOR_BODY) {
  import NumericPropertySheet.Prop

  val props = NumericPropertySheet[ConicTarget]("Orbital Elements", _.getTarget.asInstanceOf[ConicTarget],
    Prop("EPOCH", "Orbital Element Epoch (JD)",        _.getEpoch),
    Prop("IN",    "Inclination (deg)",                 _.getInclination),
    Prop("OM",    "Longitude of Ascending Node (deg)", _.getANode),
    Prop("W",     "Argument of Perihelion (deg)",      _.getPerihelion),
    Prop("QR",    "Perihelion Distance (AU)",          _.getAQ),
    Prop("EC",    "Eccentricity",                      _.getE, _.setE(_)),
    Prop("TP",    "Time of Perihelion Passage (JD)",   _.getEpochOfPeri)
  )

  val magnitudeEditor = new MagnitudeEditor {
    getComponent.asInstanceOf[JComponent].setBorder(titleBorder("Magnitudes"))
  }

  // Initialization
  setLayout(new GridBagLayout)

  // Add the magnitude editor
  add(magnitudeEditor.getComponent, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 0
    c.fill = GridBagConstraints.BOTH
  })

  // Add the param panel
  add(props, new GridBagConstraints <| { c =>
    c.gridx = 1
    c.gridy = 0
    c.fill = GridBagConstraints.HORIZONTAL
  })

  override def edit(obsContext: GOption[ObsContext], spTarget: SPTarget): Unit = {
    super.edit(obsContext, spTarget)
    magnitudeEditor.edit(obsContext, spTarget)
    props.edit(obsContext, spTarget)
  }

}