package edu.gemini.ags.api

import edu.gemini.catalog.api.{SaturationConstraint, FaintnessConstraint, MagnitudeConstraints}
import edu.gemini.catalog.api._
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{Conditions, SkyBackground, CloudCover, ImageQuality}
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._

class ConditionsAdjustmentsSpec extends Specification {
  val IQAdjustments = Map(ImageQuality.PERCENT_20 -> 0.5, ImageQuality.PERCENT_70 -> 0.0, ImageQuality.PERCENT_85 -> -0.5, ImageQuality.ANY -> -1.0)
  val CCAdjustments = Map(CloudCover.PERCENT_50 -> 0.0, CloudCover.PERCENT_70 -> -0.3, CloudCover.PERCENT_80 -> -1.0, CloudCover.ANY -> -3.0)
  val SBAdjustments = Map(SkyBackground.PERCENT_20 -> 0.0, SkyBackground.PERCENT_50 -> 0.0, SkyBackground.PERCENT_80 -> -0.3, SkyBackground.ANY -> -0.5)
  val defaultConstraints = MagnitudeConstraints(MagnitudeBand.R, agsBandExtractor(MagnitudeBand.R), FaintnessConstraint(15.5), SaturationConstraint(8.0).some)
  val badConditions = Conditions.NOMINAL.iq(ImageQuality.ANY).cc(CloudCover.ANY).sb(SkyBackground.ANY)

  "Conditions Adjuster" should {
    "adjust for nominal conditions" in {
      Conditions.NOMINAL.adjust(defaultConstraints) should beEqualTo(defaultConstraints)
    }
    "adjust for really bad conditions" in {
      badConditions.adjust(defaultConstraints) should beEqualTo(MagnitudeConstraints(MagnitudeBand.R, agsBandExtractor(MagnitudeBand.R), FaintnessConstraint(11.0), SaturationConstraint(3.5).some))
    }
    "adjust for iq" in {
      IQAdjustments.foreach {
        case (iq, factor) =>
          iq.adjust(defaultConstraints) should beEqualTo(MagnitudeConstraints(MagnitudeBand.R, agsBandExtractor(MagnitudeBand.R), FaintnessConstraint(15.5 + factor), SaturationConstraint(8.0 + factor).some))
      }
    }
    "adjust for cc" in {
      CCAdjustments.foreach {
        case (cc, factor) =>
          cc.adjust(defaultConstraints) should beEqualTo(MagnitudeConstraints(MagnitudeBand.R, agsBandExtractor(MagnitudeBand.R), FaintnessConstraint(15.5 + factor), SaturationConstraint(8.0 + factor).some))
      }
    }
    "adjust for iq" in {
      SBAdjustments.foreach {
        case (sb, factor) =>
          sb.adjust(defaultConstraints) should beEqualTo(MagnitudeConstraints(MagnitudeBand.R, agsBandExtractor(MagnitudeBand.R), FaintnessConstraint(15.5 + factor), SaturationConstraint(8.0 + factor).some))
      }
    }
  }

}
