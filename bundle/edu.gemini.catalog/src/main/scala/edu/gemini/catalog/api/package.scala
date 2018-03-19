package edu.gemini.catalog

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{Conditions, ImageQuality, CloudCover, SkyBackground}

package object api {
  // Add adjust operation to ConstraintAdjuster typeclass instances
  implicit class ConstraintsAdjusterOps[T](val a: T) extends AnyVal {
    def adjust(mc: MagnitudeConstraints)(implicit ev: ConstraintsAdjuster[T]):MagnitudeConstraints = ev.adjust(a, mc)
  }

  // Typeclasses to adjust MagnitudeConstraints for different conditions
  implicit val SkyBackgroundAdjuster: ConstraintsAdjuster[SkyBackground] =
    ConstraintsAdjuster.fromMagnitudeAdjuster[SkyBackground]

  implicit val CloudCoverAdjuster: ConstraintsAdjuster[CloudCover] =
    ConstraintsAdjuster.fromMagnitudeAdjuster[CloudCover]

  implicit val ImageQualityAdjuster: ConstraintsAdjuster[ImageQuality] =
    ConstraintsAdjuster.fromMagnitudeAdjuster[ImageQuality]

  implicit val ConditionsAdjuster: ConstraintsAdjuster[Conditions] = new ConstraintsAdjuster[Conditions] {
    override def adjust(c: Conditions, mc: MagnitudeConstraints): MagnitudeConstraints =
      c.cc.adjust(c.iq.adjust(c.sb.adjust(mc)))
  }
}