package jsky.app.ot.gemini.editor.targetComponent.details

import edu.gemini.spModel.core.Redshift
import edu.gemini.spModel.target.system.CoordinateParam.Units
import edu.gemini.spModel.target.system.CoordinateTypes.{Epoch, Parallax}

import java.awt.{GridBagConstraints, GridBagLayout, Insets}
import javax.swing.{BorderFactory, Box, JComponent, JLabel, JPanel}

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.{HmsDegTarget, ITarget}
import jsky.app.ot.gemini.editor.targetComponent.MagnitudeEditor
import squants.motion.KilometersPerSecond

import scalaz.syntax.id._

final class SiderealDetailEditor extends TargetDetailEditor(ITarget.Tag.SIDEREAL) {
  import NumericPropertySheet.Prop

  // Editor Components

  val kind   = new TargetTypeEditor
  val coords = new CoordinateEditor

  val mags   = new MagnitudeEditor <| { e =>
    e.getComponent.asInstanceOf[JComponent].setBorder(titleBorder("Magnitudes"))
  }
  val name   = new SiderealNameEditor(mags)

  sealed trait RedshiftRepresentations
  case object RadialVelocity extends RedshiftRepresentations
  case object RedshiftZ extends RedshiftRepresentations
  case object ApparentRadialVelocity extends RedshiftRepresentations

  object RedshiftRepresentations {
    val all: List[RedshiftRepresentations] = List(RadialVelocity, RedshiftZ, ApparentRadialVelocity)
    val repr: Map[RedshiftRepresentations, String] = Map(RadialVelocity -> "km/sec", RedshiftZ -> "", ApparentRadialVelocity -> "km/s")

    val renderLabel: RedshiftRepresentations => String = {
      case RadialVelocity         => "RV"
      case RedshiftZ              => "z"
      case ApparentRadialVelocity => "cz"
    }

    val renderValue: (HmsDegTarget, RedshiftRepresentations) => Double = (t, v) => v match {
      case RadialVelocity         =>
        t.getRedshift.toRadialVelocity.toKilometersPerSecond
      case RedshiftZ              =>
        t.getRedshift.redshift
      case ApparentRadialVelocity =>
        t.getRedshift.toApparentRadialVelocity.toKilometersPerSecond
    }
    val editValue: (HmsDegTarget, RedshiftRepresentations, Double) => Unit = (t, v, d) => v match {
      case RadialVelocity         =>
        t.setRedshift(Redshift.fromRadialVelocity(KilometersPerSecond(d)))
      case RedshiftZ              =>
        t.setRedshift(Redshift(d))
      case ApparentRadialVelocity =>
        t.setRedshift(Redshift.fromApparentRadialVelocity(KilometersPerSecond(d)))
    }
  }

  val props = NumericPropertySheet[HmsDegTarget](Some("Motion"), _.getTarget.asInstanceOf[HmsDegTarget],
    Prop("µ RA",     "mas/year", _.getPM1),
    Prop("µ Dec",    "mas/year", _.getPM2),
    Prop("Epoch",    "years",    _.getEpoch.getValue,    (t, d) => t.setEpoch(new Epoch(d, Units.YEARS))),
    Prop("Parallax", "mas",      _.getParallax.mas,      (t, d) => t.setParallax(new Parallax(d, Units.MILLI_ARCSECS))),
    Prop(RedshiftRepresentations.all, RedshiftRepresentations.repr, RedshiftZ, RedshiftRepresentations.renderLabel, RedshiftRepresentations.renderValue, RedshiftRepresentations.editValue)
  )

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
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(name.search, new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 2
      c.gridy  = 1
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(new JLabel("RA"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 2
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(coords.ra, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 2
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(new JLabel("Dec"), new GridBagConstraints <| { c =>
      c.anchor = GridBagConstraints.WEST
      c.gridx  = 0
      c.gridy  = 3
      c.insets = new Insets(2, 0, 0, 5)
    })

    p.add(coords.dec, new GridBagConstraints <| { c =>
      c.gridx  = 1
      c.gridy  = 3
      c.fill   = GridBagConstraints.HORIZONTAL
      c.insets = new Insets(2, 0, 0, 0)
    })

    p.add(new JLabel("J2000"), new GridBagConstraints <| { c =>
      c.anchor     = GridBagConstraints.WEST
      c.gridx      = 2
      c.gridy      = 2
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
    })
  }

  add(general, new GridBagConstraints <| { c =>
    c.gridx     = 0
    c.gridy     = 0
    c.weightx   = 0
    c.fill      = GridBagConstraints.HORIZONTAL
  })

  add(mags.getComponent, new GridBagConstraints <| { c =>
    c.gridx      = 1
    c.gridy      = 0
    c.gridheight = 2
    c.weightx    = 1.0
    c.weighty    = 1.0
    c.fill       = GridBagConstraints.BOTH
  })

  add(props, new GridBagConstraints <| { c =>
    c.anchor  = GridBagConstraints.WEST
    c.gridx   = 0
    c.gridy   = 1
    c.fill    = GridBagConstraints.HORIZONTAL
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
