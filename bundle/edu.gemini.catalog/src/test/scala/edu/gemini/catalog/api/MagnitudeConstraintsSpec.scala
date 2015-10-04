package edu.gemini.catalog.api

import edu.gemini.catalog.api.MagnitudeLimits.{SaturationLimit, FaintnessLimit}
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import org.specs2.mutable.SpecificationWithJUnit

class MagnitudeConstraintsSpec extends SpecificationWithJUnit {

  "Magnitude Constraints" should {
    "filter targets on band and faintness" in {
      val ml = MagnitudeConstraints(RBandsList, FaintnessConstraint(10.0), None)

      ml.filter(SiderealTarget("name", Coordinates.zero, None, None, None, None, Nil)) should beFalse
      val mag1 = new Magnitude(4.999, MagnitudeBand.R)

      ml.filter(SiderealTarget("name", Coordinates.zero, None, None, None, None, List(mag1))) should beTrue

      val mag2 = new Magnitude(10.001, MagnitudeBand.R)
      ml.filter(SiderealTarget("name", Coordinates.zero, None, None, None, None, List(mag2))) should beFalse

      val mag3 = new Magnitude(4.999, MagnitudeBand.K)
      ml.filter(SiderealTarget("name", Coordinates.zero, None, None, None, None, List(mag3))) should beFalse

      // Case where there are more than one magnitude band
      ml.filter(SiderealTarget("name", Coordinates.zero, None, None, None, None, List(mag2, mag3))) should beFalse
      ml.filter(SiderealTarget("name", Coordinates.zero, None, None, None, None, List(mag1, mag3))) should beTrue
    }
    "filter targets on band, faintness and saturation" in {
      val ml = MagnitudeConstraints(RBandsList, FaintnessConstraint(10.0), Some(SaturationConstraint(5)))

      ml.filter(SiderealTarget("name", Coordinates.zero, None, None, None, None, Nil)) should beFalse
      val mag1 = new Magnitude(4.999, MagnitudeBand.R)

      ml.filter(SiderealTarget("name", Coordinates.zero, None, None, None, None, List(mag1))) should beFalse

      val mag2 = new Magnitude(5.001, MagnitudeBand.R)
      ml.filter(SiderealTarget("name", Coordinates.zero, None, None, None, None, List(mag2))) should beTrue

      val mag3 = new Magnitude(4.999, MagnitudeBand.K)
      ml.filter(SiderealTarget("name", Coordinates.zero, None, None, None, None, List(mag3))) should beFalse
    }
    "behave like the union operation on MagnitudeLimits" in {
      import edu.gemini.shared.skyobject
      import edu.gemini.shared.util.immutable
      val m1 = new MagnitudeLimits(skyobject.Magnitude.Band.R, new FaintnessLimit(10.0), immutable.None.instance[SaturationLimit]())
      val m2 = new MagnitudeLimits(skyobject.Magnitude.Band.J, new FaintnessLimit(10.0), immutable.None.instance[SaturationLimit]())
      // Different band
      m1.union(m2) should beEqualTo(immutable.None.instance[MagnitudeLimits]())

      val m3 = new MagnitudeLimits(skyobject.Magnitude.Band.R, new FaintnessLimit(5.0), immutable.None.instance[SaturationLimit]())
      m1.union(m3).getValue should beEqualTo(new MagnitudeLimits(skyobject.Magnitude.Band.R, new FaintnessLimit(10.0), immutable.None.instance[SaturationLimit]()))

      val m4 = new MagnitudeLimits(skyobject.Magnitude.Band.R, new FaintnessLimit(15.0), immutable.None.instance[SaturationLimit]())
      m1.union(m4).getValue should beEqualTo(new MagnitudeLimits  (skyobject.Magnitude.Band.R, new FaintnessLimit(15.0), immutable.None.instance[SaturationLimit]()))

      val m5 = new MagnitudeLimits(skyobject.Magnitude.Band.R, new FaintnessLimit(15.0), new immutable.Some(new SaturationLimit(10.0)))
      m1.union(m5).getValue should beEqualTo(new MagnitudeLimits(skyobject.Magnitude.Band.R, new FaintnessLimit(15.0), immutable.None.instance[SaturationLimit]()))

      val m6 = new MagnitudeLimits(skyobject.Magnitude.Band.R, new FaintnessLimit(15.0), new immutable.Some(new SaturationLimit(15.0)))
      m5.union(m6).getValue should beEqualTo(new MagnitudeLimits(skyobject.Magnitude.Band.R, new FaintnessLimit(15.0), new immutable.Some(new SaturationLimit(10.0))))
    }
    "support the union operation" in {
      val m1 = MagnitudeConstraints(RBandsList, FaintnessConstraint(10.0), None)
      val m2 = MagnitudeConstraints(SingleBand(MagnitudeBand.J), FaintnessConstraint(10.0), None)
      // Different band
      m1.union(m2) should beNone

      val m3 = MagnitudeConstraints(RBandsList, FaintnessConstraint(5.0), None)
      m1.union(m3) should beSome(MagnitudeConstraints(RBandsList, FaintnessConstraint(10.0), None))

      val m4 = MagnitudeConstraints(RBandsList, FaintnessConstraint(15.0), None)
      m1.union(m4) should beSome(MagnitudeConstraints(RBandsList, FaintnessConstraint(15.0), None))

      val m5 = MagnitudeConstraints(RBandsList, FaintnessConstraint(15.0), Some(SaturationConstraint(10.0)))
      m1.union(m5) should beSome(MagnitudeConstraints(RBandsList, FaintnessConstraint(15.0), None))

      val m6 = MagnitudeConstraints(RBandsList, FaintnessConstraint(15.0), Some(SaturationConstraint(15.0)))
      m5.union(m6) should beSome(MagnitudeConstraints(RBandsList, FaintnessConstraint(15.0), Some(SaturationConstraint(10.0))))
    }
    "pick the first available R-band" in {
      val bs = RBandsList

      val t1 = SiderealTarget("name", Coordinates.zero, None, None, None, None, Nil)
      bs.extract(t1) should beNone
      val t2 = SiderealTarget("name", Coordinates.zero, None, None, None, None, List(new Magnitude(0.0, MagnitudeBand.J)))
      bs.extract(t2) should beNone
      val t3 = SiderealTarget("name", Coordinates.zero, None, None, None, None, List(new Magnitude(1.0, MagnitudeBand._r)))
      bs.extract(t3) should beSome(new Magnitude(1.0, MagnitudeBand._r))
      val t4 = SiderealTarget("name", Coordinates.zero, None, None, None, None, List(new Magnitude(1.0, MagnitudeBand.R)))
      bs.extract(t4) should beSome(new Magnitude(1.0, MagnitudeBand.R))
      val t5 = SiderealTarget("name", Coordinates.zero, None, None, None, None, List(new Magnitude(1.0, MagnitudeBand.UC)))
      bs.extract(t5) should beSome(new Magnitude(1.0, MagnitudeBand.UC))
      val t6 = SiderealTarget("name", Coordinates.zero, None, None, None, None, List(new Magnitude(1.0, MagnitudeBand.UC), new Magnitude(1.0, MagnitudeBand._r)))
      bs.extract(t6) should beSome(new Magnitude(1.0, MagnitudeBand._r))
      val t7 = SiderealTarget("name", Coordinates.zero, None, None, None, None, List(new Magnitude(1.0, MagnitudeBand.R), new Magnitude(1.0, MagnitudeBand._r)))
      bs.extract(t7) should beSome(new Magnitude(1.0, MagnitudeBand._r))
      val t8 = SiderealTarget("name", Coordinates.zero, None, None, None, None, List(new Magnitude(1.0, MagnitudeBand.R), new Magnitude(1.0, MagnitudeBand.UC)))
      bs.extract(t8) should beSome(new Magnitude(1.0, MagnitudeBand.R))
      val t9 = SiderealTarget("name", Coordinates.zero, None, None, None, None, List(new Magnitude(1.0, MagnitudeBand.R), new Magnitude(1.0, MagnitudeBand.UC), new Magnitude(1.0, MagnitudeBand._r) ))
      bs.extract(t9) should beSome(new Magnitude(1.0, MagnitudeBand._r))
    }
  }
}