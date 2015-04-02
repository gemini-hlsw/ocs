package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{Insets, GridBagConstraints, GridBagLayout}
import javax.swing.{JLabel, JPanel, JComponent}

import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ConicTarget
import edu.gemini.spModel.target.system.ITarget.Tag

import jsky.app.ot.gemini.editor.targetComponent.MagnitudeEditor

import scalaz.syntax.id._

final class JplMinorBodyDetailEditor extends TargetDetailEditor(Tag.JPL_MINOR_BODY) {
  import NumericPropertySheet.Prop

  // Editors

  val kind = new TargetTypeEditor

  val props = NumericPropertySheet[ConicTarget]("Orbital Elements", _.getTarget.asInstanceOf[ConicTarget],
    Prop("EPOCH", "Orbital Element Epoch (JD)",        _.getEpoch),
    Prop("IN",    "Inclination (deg)",                 _.getInclination),
    Prop("OM",    "Longitude of Ascending Node (deg)", _.getANode),
    Prop("W",     "Argument of Perihelion (deg)",      _.getPerihelion),
    Prop("QR",    "Perihelion Distance (AU)",          _.getAQ),
    Prop("EC",    "Eccentricity",                      _.getE, _.setE(_)),
    Prop("TP",    "Time of Perihelion Passage (JD)",   _.getEpochOfPeri)
  )

  val mags = new MagnitudeEditor {
    getComponent.asInstanceOf[JComponent].setBorder(titleBorder("Magnitudes"))
  }

  // Layout

  setLayout(new GridBagLayout)

  val general = new JPanel <| { p =>
    p.setLayout(new GridBagLayout)
    p.setBorder(titleBorder("General"))

    p.add(new JLabel("Target Type"), new GridBagConstraints <| { c =>
      c.gridx = 0
      c.gridy = 0
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(0, 2, 0, 5)
    })

    p.add(kind, new GridBagConstraints <| { c =>
      c.gridx = 1
      c.gridy = 0
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(0, 5, 0, 2)
      c.weightx = 2
    })

  }

  add(general, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 0
    c.gridwidth = 2
    c.fill = GridBagConstraints.HORIZONTAL
  })


  add(mags.getComponent, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 1
    c.fill = GridBagConstraints.BOTH
  })

  add(props, new GridBagConstraints <| { c =>
    c.gridx = 1
    c.gridy = 1
    c.fill = GridBagConstraints.HORIZONTAL
  })

  // Implementation

  override def edit(obsContext: GOption[ObsContext], spTarget: SPTarget): Unit = {
    super.edit(obsContext, spTarget)
    kind .edit(obsContext, spTarget)
    mags .edit(obsContext, spTarget)
    props.edit(obsContext, spTarget)
  }

}