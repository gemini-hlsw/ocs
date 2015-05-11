package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt.{GridBagConstraints, GridBagLayout, Insets}
import javax.swing.{JComponent, JLabel, JPanel}

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.{HmsDegTarget, ITarget}
import jsky.app.ot.gemini.editor.targetComponent.MagnitudeEditor

import scalaz.syntax.id._

final class SiderealDetailEditor extends TargetDetailEditor(ITarget.Tag.SIDEREAL) {
  import NumericPropertySheet.Prop

  // Editor Components

  val kind   = new TargetTypeEditor
  val name   = new SiderealNameEditor
  val coords = new CoordinateEditor

  val mags   = new MagnitudeEditor <| { e =>
    e.getComponent.asInstanceOf[JComponent].setBorder(titleBorder("Magnitudes"))
  }

  val props = NumericPropertySheet[HmsDegTarget](Some("Motion"), _.getTarget.asInstanceOf[HmsDegTarget],
    Prop("∆ RA",     "mas/year", _.getPM1),
    Prop("∆ Dec",    "mas/year", _.getPM2),
    Prop("Epoch",    "JD",       _.getEpoch),
    Prop("Parallax", "arcsec",   _.getParallax),
    Prop("RV",       "km/sec",   _.getRV)
  )

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
      c.weightx = 1
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(0, 5, 0, 2)
    })

    p.add(new JLabel("Target Name"), new GridBagConstraints <| { c =>
      c.gridx = 0
      c.gridy = 1
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 2, 0, 5)
    })

    p.add(name, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx = 1
      c.gridy = 1
      c.insets = new Insets(2, 5, 0, 2)
    })

    p.add(new JLabel("Coordinates"), new GridBagConstraints <| { c =>
      c.gridx = 0
      c.gridy = 2
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 2, 0, 5)
    })

    p.add(coords, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx = 1
      c.gridy = 2
      c.insets = new Insets(2, 5, 0, 2)
    })

  }

  add(general, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 0
    c.weightx = 1
    c.fill = GridBagConstraints.HORIZONTAL
    c.gridwidth = 2
  })

  add(mags.getComponent, new GridBagConstraints <| { c =>
    c.gridx = 0
    c.gridy = 1
    c.fill = GridBagConstraints.VERTICAL
  })

  add(props, new GridBagConstraints <| { c =>
    c.anchor = GridBagConstraints.WEST
    c.gridx = 1
    c.gridy = 1
    c.weightx = 1
    c.fill = GridBagConstraints.HORIZONTAL
  })

  override def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {
    super .edit(obsContext, spTarget, node)
    kind  .edit(obsContext, spTarget, node)
    name  .edit(obsContext, spTarget, node)
    coords.edit(obsContext, spTarget, node)
    mags  .edit(obsContext, spTarget, node)
    props .edit(obsContext, spTarget, node)
  }

}
