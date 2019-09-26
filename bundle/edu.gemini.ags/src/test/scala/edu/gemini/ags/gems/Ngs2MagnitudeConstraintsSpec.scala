package edu.gemini.ags.gems

import edu.gemini.spModel.core.RBandsList
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{Conditions, CloudCover, ImageQuality, SkyBackground, WaterVapor}
import edu.gemini.spModel.gems.GemsGuideStarType.tiptilt

import org.specs2.mutable.Specification


final class Ngs2MagnitudeConstraintsSpec extends Specification {
  private val calc = GemsMagnitudeTable.CanopusWfsMagnitudeLimitsCalculator

  import CloudCover.{ PERCENT_50 => CC50, PERCENT_70 => CC70, PERCENT_80 => CC80, ANY => CCAny }
  import ImageQuality.{ PERCENT_20 => IQ20, PERCENT_70 => IQ70, PERCENT_85 => IQ85, ANY => IQAny }
  import SkyBackground.{ PERCENT_20 => SB20, PERCENT_50 => SB50, PERCENT_80 => SB80, ANY => SBAny }
  import WaterVapor.{ ANY => WVAny }

  private val cc50 = Map(
    ((IQ20,  SB20 ), (17.0, 10.5)),
    ((IQ20,  SB50 ), (17.0, 10.5)),
    ((IQ20,  SB80 ), (16.7, 10.2)),
    ((IQ20,  SBAny), (16.5, 10.0)),
    ((IQ70,  SB20 ), (16.5, 10.0)),
    ((IQ70,  SB50 ), (16.5, 10.0)),
    ((IQ70,  SB80 ), (16.2,  9.7)),
    ((IQ70,  SBAny), (16.0,  9.5)),
    ((IQ85,  SB20 ), (16.0,  9.5)),
    ((IQ85,  SB50 ), (16.0,  9.5)),
    ((IQ85,  SB80 ), (15.7,  9.2)),
    ((IQ85,  SBAny), (15.0,  9.0)),
    ((IQAny, SB20 ), (15.0,  9.0)),
    ((IQAny, SB50 ), (15.0,  9.0)),
    ((IQAny, SB80 ), (15.0,  9.0)),
    ((IQAny, SBAny), (14.7,  8.7))
  )

  "Ngs2 tiptilt magnitude constraints" should {

    def verify(cc: CloudCover): Boolean =
      cc50.keySet.forall { case (iq, sb) =>
        val c        = new Conditions(cc, iq, sb, WVAny)
        val (f0, s0) = cc50((iq, sb))
        val adj      = cc.getAdjustment(RBandsList)
        val (ef, es) = (f0 + adj, s0 + adj)

        val a  = calc.adjustGemsMagnitudeConstraintForJava(tiptilt, None, c)
        val af = a.faintnessConstraint.brightness
        val as = a.saturationConstraint.get.brightness

        // For commissioning (see GemsMagnitudeTable.FaintLimit):
        ((ef + 1.5) - af).abs < 0.000001 && (es - as).abs < 0.000001

        // post commissioning we will presumably know the real faintness limits
        // and update the values accordingly.
//        (ef - af).abs < 0.000001 && (es - as).abs < 0.000001
      }

    "adjust for conditions" in {
      List(CC50, CC70, CC80, CCAny).forall(verify)
    }
  }

}
