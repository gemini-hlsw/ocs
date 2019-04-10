package edu.gemini.ags.servlet.arb

import edu.gemini.ags.servlet.{ AgsInstrument, AgsRequest, TargetType }
import edu.gemini.skycalc.Offset
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.{CloudCover, Conditions, ImageQuality, SkyBackground, WaterVapor}

import org.scalacheck._
import org.scalacheck.Arbitrary._


trait ArbAgsRequest {

  import coordinates._
  import agsinstrument._

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

  implicit val arbOffset: Arbitrary[Offset] =
    Arbitrary {
      for {
        p <- Gen.choose(1, 10)
        q <- Gen.choose(1, 10)
      } yield new Offset(
        new edu.gemini.skycalc.Angle(p.toDouble, edu.gemini.skycalc.Angle.Unit.ARCSECS).toDegrees,
        new edu.gemini.skycalc.Angle(q.toDouble, edu.gemini.skycalc.Angle.Unit.ARCSECS).toDegrees
      )
    }

  implicit val arbAgsRequest: Arbitrary[AgsRequest] =
    Arbitrary {
      for {
        s <- arbitrary[Site]
        c <- arbitrary[Coordinates]
        t <- arbitrary[TargetType]
        n <- arbitrary[Conditions]
        d <- Gen.choose(0, 359)
        i <- arbitrary[AgsInstrument]
        o <- arbitrary[List[Offset]]
      } yield AgsRequest(s, c, t, n, Angle.fromDegrees(d.toDouble), i, o)
    }

}

object agsrequest extends ArbAgsRequest
