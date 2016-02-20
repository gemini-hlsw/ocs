package jsky.app.ot.gemini.editor.targetComponent.details

import java.awt._
import javax.swing.{BorderFactory, Box, JComponent, JLabel, JPanel}

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.ITarget.Tag
import edu.gemini.spModel.target.system.NamedTarget
import jsky.app.ot.gemini.editor.targetComponent.MagnitudeEditor
import jsky.util.gui.{DropDownListBoxWidget, DropDownListBoxWidgetWatcher, TextBoxWidget, TextBoxWidgetWatcher}

import scalaz._, Scalaz._

final class NamedDetailEditor extends TargetDetailEditor(Tag.NAMED) with ReentrancyHack {
  private[this] var spt = new SPTarget // never null

  private def nt: NamedTarget = spt.getTarget.asInstanceOf[NamedTarget]

  // Editors

  val kind   = new TargetTypeEditor
  val valid  = new NamedValidAtEditor
  val coords = new CoordinateEditor

  val name = new TextBoxWidget <| { w =>
    w.setColumns(10)
    w.setMinimumSize(w.getPreferredSize)
    w.addWatcher(new TextBoxWidgetWatcher {
      def textBoxKeyPress(tbwe: TextBoxWidget): Unit =
        nonreentrant {
          spt.setName(tbwe.getValue)
        }
    })
  }

  val solarObject = new DropDownListBoxWidget[AnyRef] {
    setChoices(NamedTarget.SolarObject.values.asInstanceOf[Array[AnyRef]])
    addWatcher(new DropDownListBoxWidgetWatcher[AnyRef] {
      def dropDownListBoxAction(w: DropDownListBoxWidget[AnyRef], index: Int, value: String) {
        nonreentrant {
          val o = w.getValue.asInstanceOf[NamedTarget.SolarObject]
          nt.setSolarObject(o)
          nt.setName(o.getDisplayValue)
          name.setText(o.getDisplayValue) // hmm
          spt.notifyOfGenericUpdate()
        }
      }
    })
  }

  val mags = new MagnitudeEditor {
    getComponent.asInstanceOf[JComponent] <| { c =>
      c.setBorder(titleBorder("Magnitudes"))
      c.getMinimumSize <| { s =>
        c.setMinimumSize(new Dimension(s.width, 175))
      }
      c.setPreferredSize(c.getMinimumSize)
    }
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

    p.add(name, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 1
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(new JLabel("Object"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 2
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(solarObject, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 2
      c.insets = new Insets(2, 0, 0, 0)
      c.fill   = GridBagConstraints.HORIZONTAL
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
      c.gridx   = 3
      c.gridy   = 0
      c.weightx = 1
      c.fill    = GridBagConstraints.HORIZONTAL
    })
  }

  add(general, new GridBagConstraints <| { c =>
    c.gridx     = 0
    c.gridy     = 0
    c.fill      = GridBagConstraints.HORIZONTAL
  })


  add(mags.getComponent, new GridBagConstraints <| { c =>
    c.gridx     = 1
    c.gridy     = 0
    c.weightx   = 1.0
    c.weighty   = 1.0
    c.fill      = GridBagConstraints.BOTH
  })

  // Implementation

  override def edit(obsContext: GOption[ObsContext], spTarget: SPTarget, node: ISPNode): Unit = {
    super .edit(obsContext, spTarget, node)
    kind  .edit(obsContext, spTarget, node)
    coords.edit(obsContext, spTarget, node)
    mags  .edit(obsContext, spTarget, node)
    valid .edit(obsContext, spTarget, node)

    this.spt = spTarget
    nonreentrant {
      solarObject.setValue(spt.getTarget.asInstanceOf[NamedTarget].getSolarObject)
      name.setValue(spt.getName)
    }

  }

}
