package jsky.app.ot.gemini.editor.targetComponent.details2

import java.text.NumberFormat
import java.util.Locale

import edu.gemini.spModel.core.{Parallax, Epoch, DeclinationAngularVelocity, AngularVelocity, RightAscensionAngularVelocity, ProperMotion, SiderealTarget, Redshift}

import java.awt.{GridBagConstraints, GridBagLayout, Insets}
import javax.swing.{BorderFactory, Box, JComponent, JLabel, JPanel}

import edu.gemini.pot.sp.ISPNode
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.target.SPTarget
import squants.motion.KilometersPerSecond

import scalaz._, Scalaz._

final class SiderealDetailEditor extends TargetDetailEditor {
  import NumericPropertySheet2.Prop

  // Editor Components
  val kind   = new TargetTypeEditor
  val coords = new SPTargetCoordinateEditor
  val name   = new SiderealNameEditor(mags)
  val mags   = new MagnitudeEditor2 <| { e =>
    e.getComponent.asInstanceOf[JComponent].setBorder(titleBorder("Magnitudes"))
  }

  // Some total lenses. Temporary perhaps; we can't distinguish between zero and "don't know"
  val pm: SiderealTarget @> ProperMotion = SiderealTarget.properMotion.orZero(ProperMotion.zero)
  val px: SiderealTarget @> Parallax     = SiderealTarget.parallax.orZero(Parallax.zero)

  val motion = NumericPropertySheet2[SiderealTarget](Some("Motion"), _.getTarget.asInstanceOf[SiderealTarget],
    Prop("µ RA",     "mas/year", pm >=> ProperMotion.deltaRA  >=> RightAscensionAngularVelocity.velocity >=> AngularVelocity.masPerYear),
    Prop("µ Dec",    "mas/year", pm >=> ProperMotion.deltaDec >=> DeclinationAngularVelocity.velocity    >=> AngularVelocity.masPerYear),
    Prop("Epoch",    "years",    pm >=> ProperMotion.epoch    >=> Epoch.year),
    Prop.nonNegative[SiderealTarget]("Parallax", "mas", st => px.get(st).mas, (st, mas) => px.set(st, Parallax.fromMas(mas).orZero)),
    Prop(RedshiftRepresentations.all, RedshiftRepresentations.repr, RedshiftRepresentations.RedshiftZ, RedshiftRepresentations.renderLabel, RedshiftRepresentations.renderValue, RedshiftRepresentations.editValue, RedshiftRepresentations.formatter)
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

  add(motion, new GridBagConstraints <| { c =>
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
    motion.edit(obsContext, spTarget, node)
  }

}


sealed trait RedshiftRepresentations {
  def formatter: NumberFormat
}
object RedshiftRepresentations {

  // A total lens. Temporary perhaps; we can't distinguish between zero and "don't know"
  val rs: SiderealTarget @> Redshift = SiderealTarget.redshift.orZero(Redshift.zero)

  case object RadialVelocity extends RedshiftRepresentations {
    val formatter = NumberFormat.getInstance(Locale.US) <| {_.setGroupingUsed(false)}
  }
  case object RedshiftZ extends RedshiftRepresentations {
    val formatter = NumberFormat.getInstance(Locale.US) <| {_.setGroupingUsed(false)} <| {_.setMaximumFractionDigits(10)}
  }
  case object ApparentRadialVelocity extends RedshiftRepresentations {
    val formatter = NumberFormat.getInstance(Locale.US) <| {_.setGroupingUsed(false)}
  }

  val all: List[RedshiftRepresentations] =
    List(RadialVelocity, RedshiftZ, ApparentRadialVelocity)

  val repr: Map[RedshiftRepresentations, String] =
    Map(
      RadialVelocity         -> "km/s",
      RedshiftZ              -> "",
      ApparentRadialVelocity -> "km/s"
    )

  val renderLabel: RedshiftRepresentations => String = {
    case RadialVelocity         => "RV"
    case RedshiftZ              => "z"
    case ApparentRadialVelocity => "cz"
  }

  val renderValue: (SiderealTarget, RedshiftRepresentations) => Double = {
    case (t, RadialVelocity)         => rs.get(t).toRadialVelocity.toKilometersPerSecond
    case (t, RedshiftZ)              => rs.get(t).z
    case (t, ApparentRadialVelocity) => rs.get(t).toApparentRadialVelocity.toKilometersPerSecond
  }

  val editValue: (SiderealTarget, RedshiftRepresentations, Double) => SiderealTarget = (t, v, d) => v match {
    case RadialVelocity         => rs.set(t, Redshift.fromRadialVelocity(KilometersPerSecond(d)))
    case RedshiftZ              => rs.set(t, Redshift(d))
    case ApparentRadialVelocity => rs.set(t, Redshift.fromApparentRadialVelocity(KilometersPerSecond(d)))
  }

  def formatter: RedshiftRepresentations => NumberFormat =
    _.formatter

}