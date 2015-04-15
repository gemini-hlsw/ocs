package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{ Option => GOption }
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.{ConicTarget, ITarget}

import javax.swing._
import java.awt._

import jsky.app.ot.gemini.editor.targetComponent.MagnitudeEditor

import scalaz.syntax.id._

abstract class ConicDetailEditor(tag: ITarget.Tag) extends TargetDetailEditor(tag) {

  // Editors

  val kind = new TargetTypeEditor
  val valid  = new ConicValidAtEditor
  val name   = new ConicNameEditor(valid.dateTime)
  val coords = new CoordinateEditor

  val props: NumericPropertySheet[ConicTarget]

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

    p.add(new JLabel("Name"), new GridBagConstraints <| { c =>
      c.gridx = 0
      c.gridy = 1
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 2, 0, 5)
    })

    p.add(name, new GridBagConstraints <| { c =>
      c.gridx = 1
      c.gridy = 1
      c.insets = new Insets(2, 5, 0, 2)
      c.anchor = GridBagConstraints.WEST
      c.weightx = 2
    })

    p.add(new JLabel("Coordinates"), new GridBagConstraints <| { c =>
      c.gridx = 0
      c.gridy = 2
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 2, 0, 5)
    })

    p.add(coords, new GridBagConstraints <| { c =>
      c.gridx = 1
      c.gridy = 2
      c.insets = new Insets(2, 5, 0, 2)
      c.anchor = GridBagConstraints.WEST
    })

    p.add(new JLabel("Valid At"), new GridBagConstraints <| { c =>
      c.gridx = 0
      c.gridy = 3
      c.fill = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 2, 0, 5)
    })

    p.add(valid, new GridBagConstraints <| { c =>
      c.gridx = 1
      c.gridy = 3
      c.insets = new Insets(2, 5, 0, 2)
      c.anchor = GridBagConstraints.WEST
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

  override def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {
    super .edit(obsContext, spTarget, node)
    kind  .edit(obsContext, spTarget, node)
    name  .edit(obsContext, spTarget, node)
    coords.edit(obsContext, spTarget, node)
    mags  .edit(obsContext, spTarget, node)
    props .edit(obsContext, spTarget, node)
    valid .edit(obsContext, spTarget, node)
  }


}
