package edu.gemini.catalog.api

import edu.gemini.catalog.api.MagnitudeLimits.{SaturationLimit, FaintnessLimit}
import edu.gemini.shared.util.immutable
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{ Coordinates, Magnitude, MagnitudeBand}
import org.specs2.mutable.SpecificationWithJUnit

class MagnitudeConstraintsSpec extends SpecificationWithJUnit {

  "Magnitude Constraints" should {
    "filter targets on band and faintness" in {
      val ml = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(10.0), None)

      ml.filter(SiderealTarget("name", Coordinates.zero, None, Nil, None)) should beFalse
      val mag1 = new Magnitude(4.999, MagnitudeBand.R)

      ml.filter(SiderealTarget("name", Coordinates.zero, None, List(mag1), None)) should beTrue

      val mag2 = new Magnitude(10.001, MagnitudeBand.R)
      ml.filter(SiderealTarget("name", Coordinates.zero, None, List(mag2), None)) should beFalse

      val mag3 = new Magnitude(4.999, MagnitudeBand.K)
      ml.filter(SiderealTarget("name", Coordinates.zero, None, List(mag3), None)) should beFalse

      // Case where there are more than one magnitude band
      ml.filter(SiderealTarget("name", Coordinates.zero, None, List(mag2, mag3), None)) should beFalse
      ml.filter(SiderealTarget("name", Coordinates.zero, None, List(mag1, mag3), None)) should beTrue
    }
    "filter targets on band, faintness and saturation" in {
      val ml = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(10.0), Some(SaturationConstraint(5)))

      ml.filter(SiderealTarget("name", Coordinates.zero, None, Nil, None)) should beFalse
      val mag1 = new Magnitude(4.999, MagnitudeBand.R)

      ml.filter(SiderealTarget("name", Coordinates.zero, None, List(mag1), None)) should beFalse

      val mag2 = new Magnitude(5.001, MagnitudeBand.R)
      ml.filter(SiderealTarget("name", Coordinates.zero, None, List(mag2), None)) should beTrue

      val mag3 = new Magnitude(4.999, MagnitudeBand.K)
      ml.filter(SiderealTarget("name", Coordinates.zero, None, List(mag3), None)) should beFalse
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
      val m1 = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(10.0), None)
      val m2 = MagnitudeConstraints(MagnitudeBand.J, FaintnessConstraint(10.0), None)
      // Different band
      m1.union(m2) should beNone

      val m3 = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(5.0), None)
      m1.union(m3) should beSome(MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(10.0), None))

      val m4 = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(15.0), None)
      m1.union(m4) should beSome(MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(15.0), None))

      val m5 = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(15.0), Some(SaturationConstraint(10.0)))
      m1.union(m5) should beSome(MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(15.0), None))

      val m6 = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(15.0), Some(SaturationConstraint(15.0)))
      m5.union(m6) should beSome(MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(15.0), Some(SaturationConstraint(10.0))))
    }
    /*"supports mapping, e.g. for conditions" in {
      import edu.gemini.shared.util.immutable.MapOp

      val m = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(15.0), Some(SaturationConstraint(10.0)))

      m.map(identity) should beEqualTo(m)
      // As used with conditions
      val brightnessChangeOp = new MapOp[Magnitude, Magnitude] {
        override def apply(t: Magnitude) = t.copy(value = t.value + 1)
      }

      m.map(m => brightnessChangeOp.apply(m)).faintnessConstraint should beEqualTo(FaintnessConstraint(16.0))
      m.map(m => brightnessChangeOp.apply(m)).saturationConstraint should beEqualTo(Some(SaturationConstraint(11.0)))

      val bandChangeOp = new MapOp[Magnitude, Magnitude] {
        override def apply(t: Magnitude) = t.copy(band = MagnitudeBand.K)
      }

      m.map(m => bandChangeOp.apply(m)) should beEqualTo(MagnitudeConstraints(MagnitudeBand.K, FaintnessConstraint(15.0), Some(SaturationConstraint(10.0))))
    }*/
  }
}