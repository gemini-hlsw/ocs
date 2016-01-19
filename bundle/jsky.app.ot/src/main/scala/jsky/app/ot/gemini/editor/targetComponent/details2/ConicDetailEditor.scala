package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt._
import javax.swing._

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.{ConicTarget, ITarget}
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
    p.setBorder(BorderFactory.createCompoundBorder(titleBorder("General"),
                BorderFactory.createEmptyBorder(1, 2, 1, 2)))

    p.add(new JLabel("Type"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 0
      c.insets = new Insets(0, 0, 0, 5)
    })

    p.add(kind, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 0
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(0, 0, 0, 0)
    })

    p.add(new JLabel("Name"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 1
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(name.name, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 1
      c.insets = new Insets(2, 0, 0, 0)
      c.fill   = GridBagConstraints.HORIZONTAL
    })

    p.add(name.search, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 2
      c.gridy  = 1
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(name.hid, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 2
      c.insets = new Insets(1, 0, 0, 0)
    })

    p.add(new JLabel("RA"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 3
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(coords.ra, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 3
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(new JLabel("Dec"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 4
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(coords.dec, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 4
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(new JLabel("J2000"), new GridBagConstraints <| { c =>
      c.anchor     = GridBagConstraints.WEST
      c.gridx      = 2
      c.gridy      = 3
      c.gridheight = 2
      c.insets     = new Insets(2, 5, 0, 0)
    })

    p.add(new JLabel("Valid"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 5
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(valid.dateTimePanel, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 5
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(new JLabel("UTC"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 2
      c.gridy  = 5
      c.insets = new Insets(2, 5, 0, 0)
    })

    p.add(valid.controlsPanel, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 6
      c.insets = new Insets(2, 0, 0, 0)
    })


    p.add(Box.createHorizontalGlue(), new GridBagConstraints <| { c =>
      c.gridx   = 4
      c.gridy   = 0
      c.weightx = 1
      c.fill    = GridBagConstraints.HORIZONTAL
    })

  }

  add(general, new GridBagConstraints <| { c =>
    c.gridx   = 0
    c.gridy   = 0
    c.fill    = GridBagConstraints.HORIZONTAL
  })

  add(props, new GridBagConstraints <| { c =>
    c.gridx   = 0
    c.gridy   = 1
    c.fill    = GridBagConstraints.HORIZONTAL
  })

  add(mags.getComponent, new GridBagConstraints <| { c =>
    c.gridx      = 1
    c.gridy      = 0
    c.gridheight = 2
    c.weightx    = 1.0
    c.weighty    = 1.0
    c.fill       = GridBagConstraints.BOTH
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
