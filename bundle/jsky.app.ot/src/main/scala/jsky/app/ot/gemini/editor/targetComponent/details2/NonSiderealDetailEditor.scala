package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt._
import javax.swing._

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget

import scalaz.syntax.id._

final class NonSiderealDetailEditor extends TargetDetailEditor {

  // Editors

  val kind  = new TargetTypeEditor
  val name  = new NonSiderealNameEditor
  val ephem = new EphemerisEditor

  val mags = new MagnitudeEditor2 {
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

    p.add(new JLabel("Query"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 3
      c.insets = new Insets(1, 0, 0, 5)
    })

    p.add(name.hid, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 3
      c.insets = new Insets(1, 1, 0, 0)
    })

    p.add(new JLabel("Ephemeris"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 4
      c.insets = new Insets(6, 0, 0, 5)
    })

    p.add(ephem.start, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 4
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(6, 1, 0, 0)
    })

    p.add(ephem.end, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 5
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 1, 0, 0)
    })

    p.add(ephem.size, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 6
      c.insets = new Insets(2, 1, 0, 0)
    })

  }

  add(general, new GridBagConstraints <| { c =>
    c.gridx   = 0
    c.gridy   = 0
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
    super.edit(obsContext, spTarget, node)
    kind .edit(obsContext, spTarget, node)
    name .edit(obsContext, spTarget, node)
    ephem.edit(obsContext, spTarget, node)
    mags .edit(obsContext, spTarget, node)
  }


}
