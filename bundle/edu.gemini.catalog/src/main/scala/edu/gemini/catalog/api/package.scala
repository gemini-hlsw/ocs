package edu.gemini.catalog

import edu.gemini.spModel.core.{MagnitudeBand, Magnitude}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{Conditions, ImageQuality, CloudCover, SkyBackground}

package object api {
  // Function that can extract a magnitude out of a target
  type MagnitudeExtractor = SiderealTarget => Option[Magnitude]

  // Function that can extract a magnitude out of a target
  def defaultMagnitudeExtractor(band: MagnitudeBand):MagnitudeExtractor = (t: SiderealTarget) => t.magnitudeIn(band)

  // Add adjust operation to ConstraintAdjuster typeclass instances
  implicit class ConstraintsAdjusterOps[T](val a: T) extends AnyVal {
    def adjust(mc: MagnitudeConstraints)(implicit ev: ConstraintsAdjuster[T]):MagnitudeConstraints = ev.adjust(a, mc)
  }

  // Typeclasses to adjust MagnitudeConstraints for different conditions
  implicit val SkyBackgroundAdjuster = new ConstraintsAdjuster[SkyBackground] {
    // Sky Background only adjust for band R and on 80% and ANY
    override def adjust(sb: SkyBackground, mc: MagnitudeConstraints) = mc.referenceBand match {
      case SkyBackground.BAND_TO_ADJUST => mc.adjust(_ + sb.magAdjustment())
      case _                            => mc
    }
  }

  implicit val CloudCoverAdjuster = new ConstraintsAdjuster[CloudCover] {
    // CloudCover adjusts in all bands
    override def adjust(cc: CloudCover, mc: MagnitudeConstraints) = (cc, mc) match {
      case _  => mc.adjust(_ + cc.magAdjustment())
    }
  }

  implicit val ImageQualityAdjuster = new ConstraintsAdjuster[ImageQuality] {
    // ImageQuality adjust for band R
    override def adjust(iq: ImageQuality, mc: MagnitudeConstraints) = mc.referenceBand match {
      case ImageQuality.BAND_TO_ADJUST => mc.adjust(_ + iq.magAdjustment())
      case _                            => mc
    }
  }

  implicit val ConditionsAdjuster = new ConstraintsAdjuster[Conditions] {
    override def adjust(c: Conditions, mc: MagnitudeConstraints) =
      c.cc.adjust(c.iq.adjust(c.sb.adjust(mc)))
  }

}