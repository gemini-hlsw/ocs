package edu.gemini.catalog

import edu.gemini.spModel.core.{MagnitudeBand, Magnitude}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{Conditions, ImageQuality, CloudCover, SkyBackground}

package object api {
  // Add adjust operation to ConstraintAdjuster typeclass instances
  implicit class ConstraintsAdjusterOps[T](val a: T) extends AnyVal {
    def adjust(mc: MagnitudeConstraints)(implicit ev: ConstraintsAdjuster[T]):MagnitudeConstraints = ev.adjust(a, mc)
  }

  // Typeclasses to adjust MagnitudeConstraints for different conditions
  implicit val SkyBackgroundAdjuster = new ConstraintsAdjuster[SkyBackground] {
    // ImageQuality adjust for the R-bands list
    override def adjust(sb: SkyBackground, mc: MagnitudeConstraints) = (sb, mc.searchBands) match {
      case (SkyBackground.PERCENT_80, RBandsList) => mc.adjust(_ - 0.3)
      case (SkyBackground.ANY, RBandsList)        => mc.adjust(_ - 0.5)
      case _                                      => mc
    }
  }

  implicit val CloudCoverAdjuster = new ConstraintsAdjuster[CloudCover] {
    // CloudCover adjusts in all bands
    override def adjust(cc: CloudCover, mc: MagnitudeConstraints) = cc match {
      case CloudCover.PERCENT_70                  => mc.adjust(_ - 0.3)
      case CloudCover.PERCENT_80                  => mc.adjust(_ - 1.0)
      case CloudCover.PERCENT_90 | CloudCover.ANY => mc.adjust(_ - 3.0)
      case _                                      => mc
    }
  }

  implicit val ImageQualityAdjuster = new ConstraintsAdjuster[ImageQuality] {
    // ImageQuality adjust for the R-bands list
    override def adjust(iq: ImageQuality, mc: MagnitudeConstraints) = (iq, mc.searchBands) match {
      case (ImageQuality.PERCENT_20, RBandsList) => mc.adjust(_ + 0.5)
      case (ImageQuality.PERCENT_85, RBandsList) => mc.adjust(_ - 0.5)
      case (ImageQuality.ANY, RBandsList)        => mc.adjust(_ - 1.0)
      case _                                     => mc
    }
  }

  implicit val ConditionsAdjuster = new ConstraintsAdjuster[Conditions] {
    override def adjust(c: Conditions, mc: MagnitudeConstraints) =
      c.cc.adjust(c.iq.adjust(c.sb.adjust(mc)))
  }

}