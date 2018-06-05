package jsky.app.ot.gemini.editor.targetComponent.details2

import java.awt.{BorderLayout, GridBagConstraints, GridBagLayout, Insets}
import javax.swing.{BorderFactory, Box, JLabel, JPanel}

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPCoordinates
import jsky.app.ot.gemini.editor.targetComponent.TelescopePosEditor

import scalaz._
import Scalaz._

class SPCoordinatesEditorPanel extends JPanel with TelescopePosEditor[SPCoordinates] {
  setLayout(new BorderLayout)

  // Editor Components
  val coordsEditor = new SPCoordinateEditor

  // This doodad will ensure that any change event coming from the SPTarget will get turned into
  // a call to `edit`, so we don't have to worry about that case everywhere. Everything from here
  // on down only needs to care about implementing `edit`.
  val tpw = new ForwardingTelescopePosWatcher(this, () => new SPCoordinates())

  override def edit(ctx: GOption[ObsContext], spCoordinates: SPCoordinates, node: ISPNode): Unit = {
    require(ctx           != null, "obsContext should never be null")
    require(spCoordinates != null, "spCoordinates should never be null")
    tpw.         edit(ctx, spCoordinates, node)
    coordsEditor.edit(ctx, spCoordinates, node)
  }

  val panel = new JPanel <| { p =>
    p.setLayout(new GridBagLayout)
    p.setBorder(BorderFactory.createCompoundBorder(titleBorder("Coordinates"),
                BorderFactory.createEmptyBorder(1, 2, 1, 2)))

    p.add(new JLabel("RA"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 0
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(coordsEditor.ra, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 0
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(new JLabel("Dec"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 1
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(coordsEditor.dec, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 1
      c.gridy  = 1
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(new JLabel("J2000"), new GridBagConstraints <| { c =>
      c.anchor     = GridBagConstraints.WEST
      c.gridx      = 2
      c.gridy      = 0
      c.gridheight = 2
      c.insets     = new Insets(2, 5, 0, 0)
    })

    p.add(Box.createHorizontalGlue(), new GridBagConstraints <| { c =>
      c.gridx   = 2
      c.gridy   = 0
      c.weightx = 1
      c.fill    = GridBagConstraints.HORIZONTAL
    })

    p.add(Box.createVerticalGlue(), new GridBagConstraints <| { c =>
      c.gridx   = 0
      c.gridy   = 3
      c.weighty = 1
      c.fill    = GridBagConstraints.VERTICAL
      c.insets  = new Insets(233,  0, 0, 0)
    })
  }

  add(panel, BorderLayout.CENTER)
}
