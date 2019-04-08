package edu.gemini.ags.servlet.arb

import edu.gemini.ags.servlet.{ AgsRequest, TargetType }
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, Conditions, ImageQuality, SkyBackground, WaterVapor}

import org.scalacheck._
import org.scalacheck.Arbitrary._


trait ArbAgsRequest {

  import coordinates._

  implicit val arbSite: Arbitrary[Site] =
    Arbitrary {
      Gen.oneOf(Site.GN, Site.GS)
    }

  implicit val arbTargetType: Arbitrary[TargetType] =
    Arbitrary {
      Gen.oneOf(TargetType.Sidereal, TargetType.NonSidereal)
    }

  implicit val arbConditions: Arbitrary[Conditions] =
    Arbitrary {
      for {
        cc <- Gen.oneOf(CloudCover.values)
        iq <- Gen.oneOf(ImageQuality.values)
        sb <- Gen.oneOf(SkyBackground.values)
      } yield new Conditions(cc, iq, sb, WaterVapor.ANY)
    }

  implicit val arbAgsRequest: Arbitrary[AgsRequest] =
    Arbitrary {
      for {
        s <- arbitrary[Site]
        c <- arbitrary[Coordinates]
        t <- arbitrary[TargetType]
        n <- arbitrary[Conditions]
      } yield AgsRequest(s, c, t, n)
    }

}

object agsrequest extends ArbAgsRequest
