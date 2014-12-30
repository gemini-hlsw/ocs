package edu.gemini.catalog.api

import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core.{Equinox, Coordinates, Magnitude, MagnitudeBand}
import org.specs2.mutable.SpecificationWithJUnit

class MagnitudeConstraintsSpec extends SpecificationWithJUnit {

  "Magnitude Constraints" should {
    "contain values in band and in range" in {
      val ml = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(10.0), Some(SaturationConstraint(5.0)))
      val good = List(new Magnitude(5.0, MagnitudeBand.R), new Magnitude(7.0, MagnitudeBand.R), new Magnitude(10.0, MagnitudeBand.R))
      for (m <- good) ml.contains(m) should beTrue
    }
    "not contain values out of range or band" in {
      val ml = MagnitudeConstraints(MagnitudeBand.K, FaintnessConstraint(10.0), Some(SaturationConstraint(5.0)))
      val bad = List(new Magnitude(7.0, MagnitudeBand.J), new Magnitude(4.9999, MagnitudeBand.R), new Magnitude(10.001, MagnitudeBand.R))
      for (m <- bad) ml.contains(m) should beFalse
    }
    "contain values in band and out of range if there is no saturation limit" in {
      val ml = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(10.0), None)
      ml.contains(new Magnitude(4.999, MagnitudeBand.R)) should beTrue
    }
    "contain values in band and out of range if there is no saturation limit" in {
      val ml = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(10.0), None)
      ml.contains(new Magnitude(4.999, MagnitudeBand.R)) should beTrue
    }
    "filter targets on band and faintness" in {
      val ml = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(10.0), None)

      ml.filter(SiderealTarget("name", Coordinates.zero, Equinox.J2000, None, Nil, None)) should beFalse
      val mag1 = new Magnitude(4.999, MagnitudeBand.R)

      ml.filter(SiderealTarget("name", Coordinates.zero, Equinox.J2000, None, List(mag1), None)) should beTrue

      val mag2 = new Magnitude(10.001, MagnitudeBand.R)
      ml.filter(SiderealTarget("name", Coordinates.zero, Equinox.J2000, None, List(mag2), None)) should beFalse

      val mag3 = new Magnitude(4.999, MagnitudeBand.K)
      ml.filter(SiderealTarget("name", Coordinates.zero, Equinox.J2000, None, List(mag3), None)) should beFalse

      // Case where there are more than one magnitude band
      ml.filter(SiderealTarget("name", Coordinates.zero, Equinox.J2000, None, List(mag2, mag3), None)) should beFalse
      ml.filter(SiderealTarget("name", Coordinates.zero, Equinox.J2000, None, List(mag1, mag3), None)) should beTrue
    }
    "filter targets on band, faintness and saturation" in {
      val ml = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(10.0), Some(SaturationConstraint(5)))

      ml.filter(SiderealTarget("name", Coordinates.zero, Equinox.J2000, None, Nil, None)) should beFalse
      val mag1 = new Magnitude(4.999, MagnitudeBand.R)

      ml.filter(SiderealTarget("name", Coordinates.zero, Equinox.J2000, None, List(mag1), None)) should beFalse

      val mag2 = new Magnitude(5.001, MagnitudeBand.R)
      ml.filter(SiderealTarget("name", Coordinates.zero, Equinox.J2000, None, List(mag2), None)) should beTrue

      val mag3 = new Magnitude(4.999, MagnitudeBand.K)
      ml.filter(SiderealTarget("name", Coordinates.zero, Equinox.J2000, None, List(mag3), None)) should beFalse
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
      m5.union(m6) should beSome(MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(15.0), Some(SaturationConstraint(15.0))))
    }
    "supports mapping, e.g. for conditions" in {
      import edu.gemini.shared.util.immutable.MapOp

      val m = MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(15.0), Some(SaturationConstraint(10.0)))

      m.map(identity) should beEqualTo(m)
      // As used with conditions
      val brightnessChangeOp = new MapOp[Magnitude, Magnitude] {
        override def apply(t: Magnitude) = t.copy(value = t.value + 1)
      }

      m.map(m => brightnessChangeOp.apply(m)) should beEqualTo(MagnitudeConstraints(MagnitudeBand.R, FaintnessConstraint(16.0), Some(SaturationConstraint(11.0))))

      val bandChangeOp = new MapOp[Magnitude, Magnitude] {
        override def apply(t: Magnitude) = t.copy(band = MagnitudeBand.K)
      }

      m.map(m => bandChangeOp.apply(m)) should beEqualTo(MagnitudeConstraints(MagnitudeBand.K, FaintnessConstraint(15.0), Some(SaturationConstraint(10.0))))
    }
  }
}