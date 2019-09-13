package edu.gemini.ags.api

import edu.gemini.ags.api.AgsGuideQuality.Unusable
import edu.gemini.ags.api.AgsMagnitude._
import edu.gemini.pot.ModelConverters._
import edu.gemini.spModel.core.{Coordinates, Magnitude, SiderealTarget}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.ImageQuality
import edu.gemini.spModel.guide.{GuideProbe, GuideProbeGroup, GuideSpeed, GuideStarValidation, ValidatableGuideProbe}
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import scalaz._
import Scalaz._
import edu.gemini.spModel.gemini.gems.CanopusWfs

// REL-3719: As of NGS2, "Guide speed" messages no longer apply to NGS2 guide stars, i.e. Canopus and PWFS1 SFS.
sealed trait AgsGuideQuality {
  def message: String
}

object AgsGuideQuality {
  case object DeliversRequestedIq extends AgsGuideQuality {
    override val message = "Delivers requested IQ."
  }
  case object PossibleIqDegradation extends AgsGuideQuality {
    override val message = "Slower guiding required; may not deliver requested IQ."
  }
  case object IqDegradation extends AgsGuideQuality {
    override val message = "Slower guiding required; will not deliver requested IQ."
  }
  case object PossiblyUnusable extends AgsGuideQuality {
    override val message = "May not be able to guide."
  }
  case object Unusable extends AgsGuideQuality {
    override val message = "Unable to guide."
  }

  val All: List[AgsGuideQuality] =
    List(DeliversRequestedIq, PossibleIqDegradation, IqDegradation, PossiblyUnusable, Unusable)

  private val orderByIndex = All.zipWithIndex.toMap

  implicit val AgsGuideQualityOrder: Order[AgsGuideQuality] =
    Order.orderBy(orderByIndex)

  implicit val AgsGuideQualityOrdering: scala.math.Ordering[AgsGuideQuality] =
    scala.math.Ordering.by(orderByIndex)

  implicit val AgsGuideQualityShow: Show[AgsGuideQuality] =
    Show.shows {
      case DeliversRequestedIq   => "Delivers Requested IQ"
      case PossibleIqDegradation => "Possible IQ Degradation"
      case IqDegradation         => "IQ Degradation"
      case PossiblyUnusable      => "Possibly Unusable"
      case Unusable              => "Unusable"
    }
}

sealed trait AgsAnalysis {
  def quality: AgsGuideQuality = Unusable
  def message(withProbe: Boolean): String
}

object AgsAnalysis {
  def isCanopus(guideProbe: GuideProbe): Boolean = CanopusWfs.values().contains(guideProbe)

  final case class NoGuideStarForProbe(guideProbe: GuideProbe) extends AgsAnalysis {
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"${guideProbe.getKey} " else ""
      s"No ${p}guide star selected."
    }
  }

  final case class NoGuideStarForGroup(guideGroup: GuideProbeGroup) extends AgsAnalysis {
    override def message(withProbe: Boolean): String =
      s"No ${guideGroup.getKey} guide star selected."
  }

  final case class MagnitudeTooFaint(guideProbe: GuideProbe, target: SiderealTarget, showGuideSpeed: Boolean) extends AgsAnalysis {
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"use ${guideProbe.getKey}" else "guide"
      val gs = if (showGuideSpeed) ", even using the slowest guide speed" else ""
      s"Cannot $p with the star in these conditions$gs."
    }
  }

  final case class MagnitudeTooBright(guideProbe: GuideProbe, target: SiderealTarget) extends AgsAnalysis {
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"${guideProbe.getKey} g" else "G"
      s"${p}uide star is too bright to guide."
    }
  }

  final case class NotReachable(guideProbe: GuideProbe, target: SiderealTarget) extends AgsAnalysis {
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"with ${guideProbe.getKey} " else ""
      s"The star is not reachable ${p}at all positions."
    }
  }

  final case class NoMagnitudeForBand(guideProbe: GuideProbe, target: SiderealTarget) extends AgsAnalysis {
    private val probeBands = guideProbe.getBands
    override def message(withProbe: Boolean): String = {
      val p = if (withProbe) s"${guideProbe.getKey} g" else "G"
      if (probeBands.bands.length == 1) {
        s"${p}uide star ${probeBands.bands.head}-band magnitude is missing. Cannot determine guiding performance."
      } else {
        s"${p}uide star ${probeBands.bands.map(_.name).mkString(", ")}-band magnitudes are missing. Cannot determine guiding performance."
      }
    }
    override val quality: AgsGuideQuality = AgsGuideQuality.PossiblyUnusable
  }

  final case class Usable(guideProbe: GuideProbe, target: SiderealTarget, guideSpeed: Option[GuideSpeed], override val quality: AgsGuideQuality) extends AgsAnalysis {
    override def message(withProbe: Boolean): String = {
      val qualityMessage = quality match {
        case AgsGuideQuality.DeliversRequestedIq => ""
        case _                                   => s"${quality.message} "
      }
      val p = if (withProbe) s"${guideProbe.getKey} " else ""
      val gs = guideSpeed.map(gs => s"Guide Speed: ${gs.name}").getOrElse("Usable")
      s"$qualityMessage$p$gs."
    }
  }

  def guideProbe(a: AgsAnalysis): Option[GuideProbe] = a match {
    case NoGuideStarForProbe(p)     => Some(p)
    case NoGuideStarForGroup(_)     => None
    case MagnitudeTooFaint(p, _, _) => Some(p)
    case MagnitudeTooBright(p, _)   => Some(p)
    case NotReachable(p, _)         => Some(p)
    case NoMagnitudeForBand(p, _)   => Some(p)
    case Usable(p, _, _, _)         => Some(p)
  }


  /**
   * Analysis of the selected guide star (if any) in the given context.
   */
  protected [ags] def analysis(ctx: ObsContext, mt: MagnitudeTable,
                               guideProbe: ValidatableGuideProbe): Option[AgsAnalysis] = {
    def selection(ctx: ObsContext, guideProbe: GuideProbe): Option[SPTarget] =
      for {
        gpt   <- ctx.getTargets.getPrimaryGuideProbeTargets(guideProbe).asScalaOpt
        gStar <- gpt.getPrimary.asScalaOpt
      } yield gStar

    selection(ctx, guideProbe).fold(Some(NoGuideStarForProbe(guideProbe)): Option[AgsAnalysis]) { guideStar =>
      AgsAnalysis.analysis(ctx, mt, guideProbe, guideStar.toSiderealTarget(ctx.getSchedulingBlockStart))
    }
  }

  /**
   * Analysis of the given guide star in the given context, regardless of which
   * guide star is actually selected in the target environment.
   */
  def analysis(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe,
               guideStar: SiderealTarget): Option[AgsAnalysis] = {
    val spTarget = new SPTarget(SiderealTarget.empty.copy(coordinates = Coordinates(guideStar.coordinates.ra, guideStar.coordinates.dec)))

    if (guideProbe.validate(spTarget, ctx) != GuideStarValidation.VALID) Some(NotReachable(guideProbe, guideStar))
    else magnitudeAnalysis(ctx, mt, guideProbe, guideStar)
  }

  /**
   * Analysis of the suitability of the magnitude of the given guide star
   * regardless of its reachability.
   */
  def magnitudeAnalysis(ctx: ObsContext, mt: MagnitudeTable, guideProbe: ValidatableGuideProbe,
                        guideStar: SiderealTarget): Option[AgsAnalysis] = {
    import AgsGuideQuality._
    import GuideSpeed._

    val conds = ctx.getConditions

    // Handles the case where the magnitude falls outside of the acceptable ranges for any guide speed.
    // This handles Andy's 0.5 rule where we might possibly be able to guide if the star is only 0.5 too dim, and
    // otherwise returns the appropriate analysis indicating too dim or too bright.
    def outsideLimits(magCalc: MagnitudeCalc, mag: Double): AgsAnalysis = {
      val adj             = 0.5

      val faintnessLimit  = magCalc(conds, SLOW).faintnessConstraint.brightness
      val saturationLimit = magCalc(conds, FAST).saturationConstraint
      val saturated       = saturationLimit.exists(_.brightness > mag)

      def almostTooFaint: Boolean = !saturated && mag <= faintnessLimit + adj
      def tooFaint:       Boolean = mag > faintnessLimit + adj

      if (almostTooFaint) Usable(guideProbe, guideStar, SLOW.some, PossiblyUnusable)
      else if (tooFaint)  MagnitudeTooFaint(guideProbe, guideStar, true)
      else                MagnitudeTooBright(guideProbe, guideStar)
    }

    // Called when we know that a valid guide speed can be chosen for the given guide star.
    // Determine the quality and return an analysis indicating that the star is usable.
    def usable(guideSpeed: GuideSpeed): AgsAnalysis = {
      def worseOrEqual(iq: ImageQuality) = conds.iq.compareTo(iq) >= 0

      val quality = guideSpeed match {
        case FAST =>
          DeliversRequestedIq
        case MEDIUM =>
          if (worseOrEqual(ImageQuality.PERCENT_70)) DeliversRequestedIq
          else PossibleIqDegradation
        case SLOW =>
          if (worseOrEqual(ImageQuality.PERCENT_85)) DeliversRequestedIq
          else if (worseOrEqual(ImageQuality.PERCENT_70)) PossibleIqDegradation
          else IqDegradation
      }

      Usable(guideProbe, guideStar, guideSpeed.some, quality)
    }

    // Find the first band in the guide star that is on the list of possible bands
    def usableMagnitude:Option[Magnitude] = guideProbe.getBands.bands.map(guideStar.magnitudeIn).find(_.isDefined).flatten

    for {
      mc  <- mt(ctx, guideProbe)
      mag = usableMagnitude
    } yield {
      val analysisOpt = mag.map(m => fastestGuideSpeed(mc, m, conds).fold(outsideLimits(mc, m.value))(usable))
      analysisOpt.getOrElse(NoMagnitudeForBand(guideProbe, guideStar))
    }
  }
}

sealed trait GuideInFOV

object GuideInFOV {
  case object Inside  extends GuideInFOV
  case object Outside extends GuideInFOV

  implicit val order: Order[GuideInFOV] = Order.order {
    case (Inside, Outside) => Ordering.GT
    case (Outside, Inside) => Ordering.LT
    case _                 => Ordering.EQ
  }

  implicit val ordering: scala.Ordering[GuideInFOV] = order.toScalaOrdering

  val All: List[GuideInFOV] = List(Inside, Outside)
}
