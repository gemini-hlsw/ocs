package edu.gemini.catalog.votable

import edu.gemini.catalog.api._
import edu.gemini.spModel.core._
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mutable.SpecificationWithJUnit

class CatalogQuerySpec extends SpecificationWithJUnit {
  val a0    = Angle.fromArcsecs( 0.0)
  val a2    = Angle.fromArcsecs( 2.0)
  val a4_9  = Angle.fromArcsecs( 4.9)
  val a5    = Angle.fromArcsecs( 5.0)
  val a7    = Angle.fromArcsecs( 7.0)
  val a10   = Angle.fromArcsecs(10.0)
  val a11   = Angle.fromArcsecs(11.0)
  val a20   = Angle.fromArcsecs(20.0)

  val rad10 = RadiusConstraint.between(Angle.zero, a10)
  val rad5 = RadiusConstraint.between(Angle.zero, a5)

  val faint10 = FaintnessConstraint(10)
  val saturation0= SaturationConstraint(0)
  val mag10 = MagnitudeConstraints(RBandsList, faint10, Some(saturation0))
  val base = Coordinates.zero
  val par10_10 = CatalogQuery(base, rad10, mag10, UCAC4)
  val par5_10 = CatalogQuery(base, rad5, mag10, UCAC4)
  val ma2 = Angle.zero - Angle.fromArcsecs(2)
  val ma4_9 = Angle.zero - Angle.fromArcsecs(4.99999)
  val ma7   = Angle.zero - Angle.fromArcsecs(7.0)
  val ma10  = Angle.zero - Angle.fromArcsecs(10.0)
  val ma11  = Angle.zero - Angle.fromArcsecs(11.0)
  val ma20  = Angle.zero - Angle.fromArcsecs(20.0)

  def beSuperSetOf(cq: CatalogQuery) =  new Matcher[CatalogQuery] {
    override def apply[S <: CatalogQuery](t: Expectable[S]) = {
      val actual = t.value
      result(actual.isSuperSetOf(cq), s"$actual is a superset of $cq", s"$actual is not a proper superset of $cq", t)
    }
  }

  def beNoSuperSetOf(cq: CatalogQuery) =  new Matcher[CatalogQuery] {
    override def apply[S <: CatalogQuery](t: Expectable[S]) = {
      val actual = t.value
      result(!actual.isSuperSetOf(cq) && !cq.isSuperSetOf(actual), s"$actual is not a superset of $cq", s"$actual is a superset of $cq", t)
    }
  }

  "CatalogQuerySpec" should {
    "be a superset of itself" in {
      par10_10 should beSuperSetOf(par10_10)
    }
    "be a superset of the same base" in {
      par10_10 should beSuperSetOf(par5_10)
    }
    "be a superset within the range limits" in {
      val rad5 = RadiusConstraint.between(Angle.zero, a5)

      val coordinates = List(
          Coordinates(RightAscension.fromAngle(a2),    Declination.fromAngle(a0).getOrElse(Declination.zero)),
          Coordinates(RightAscension.fromAngle(a4_9),  Declination.fromAngle(a0).getOrElse(Declination.zero)),
          Coordinates(RightAscension.fromAngle(ma2),   Declination.fromAngle(a0).getOrElse(Declination.zero)),
          Coordinates(RightAscension.fromAngle(ma4_9), Declination.fromAngle(a0).getOrElse(Declination.zero)),
          Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(a2).getOrElse(Declination.zero)),
          Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(a4_9).getOrElse(Declination.zero)),
          Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(ma2).getOrElse(Declination.zero)),
          Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(ma4_9).getOrElse(Declination.zero)))
      coordinates.map(c =>
        par10_10 should beSuperSetOf(CatalogQuery(c, rad5, mag10, UCAC4))
      )
    }
    "be a superset at the pole" in {
      val pole = Coordinates(RightAscension.zero, Declination.fromAngle(Angle.fromDegrees(90.0)).getOrElse(Declination.zero))
      val close = Coordinates(RightAscension.zero, Declination.fromAngle(Angle.fromDegrees(90.0) - Angle.fromArcsecs(2)).getOrElse(Declination.zero))
      // 10 arcsec radius at the pole
      val poleP = CatalogQuery(pole, rad10, mag10, UCAC4)
      // 5 arcsec radius close to the pole
      val closeP = CatalogQuery(close, rad5, mag10, UCAC4)
      poleP should beSuperSetOf(closeP)
    }
    "not be a superset far of the pole" in {
      val pole = Coordinates(RightAscension.zero, Declination.fromAngle(Angle.fromDegrees(90.0)).getOrElse(Declination.zero))
      val far = Coordinates(RightAscension.zero, Declination.fromAngle(Angle.fromDegrees(90.0) - Angle.fromArcsecs(7)).getOrElse(Declination.zero))
      // 10 arcsec radius at the pole
      val poleP = CatalogQuery(pole, rad10, mag10, UCAC4)
      // 5 arcsec radius, but a bit too far from the pole
      val farP = CatalogQuery(far, rad5, mag10, UCAC4)
      poleP should beNoSuperSetOf(farP)
    }
    "not be a superset out of the range limits" in {
      val coordinates = List(
        Coordinates(RightAscension.fromAngle(a7),    Declination.fromAngle(a0).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(ma7),   Declination.fromAngle(a0).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(a10),   Declination.fromAngle(a0).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(ma10),  Declination.fromAngle(a0).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(a11),   Declination.fromAngle(a0).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(ma11),  Declination.fromAngle(a0).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(a20),   Declination.fromAngle(a0).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(ma20),  Declination.fromAngle(a0).getOrElse(Declination.zero)),

        Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(a7).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(ma7).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(a10).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(ma10).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(a11).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(ma11).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(a20).getOrElse(Declination.zero)),
        Coordinates(RightAscension.fromAngle(a0),    Declination.fromAngle(ma20).getOrElse(Declination.zero)))
      coordinates.map(c =>
        par10_10 should beNoSuperSetOf(CatalogQuery(c, rad5, mag10, UCAC4))
      )
    }
    "not be a supersef far out of range" in {
      val far = CatalogQuery(Coordinates(RightAscension.fromDegrees(180), Declination.zero), rad5, mag10, UCAC4)
      par10_10 should beNoSuperSetOf(far)
    }
  }

}
