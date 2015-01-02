package edu.gemini.ags.impl

import edu.gemini.ags.impl
import edu.gemini.shared.skyobject
import edu.gemini.skycalc
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import org.specs2.ScalaCheck
import org.scalacheck.Prop._
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps

class ConversionsSpec extends Specification with ScalaCheck with Arbitraries {
  "implicit conversions of model classes" should {
    "convert new bands to old bands" in {
      forAll { (b: MagnitudeBand) =>
        val mag = impl.newMagnitudeBand2Old(b)
        skyobject.Magnitude.Band.values().toList should contain(mag)
      }
    }
    "convert old bands to new bands" in {
      for {
        m <- skyobject.Magnitude.Band.values().toList
        b = impl.oldMagnitudeBand2New(m)
      } yield MagnitudeBand.all should contain(b)
    }
    "convert new magnitudes to old" in {
      forAll { (m: Magnitude) =>
        val mag = impl.newMagnitude2Old(m)
        mag.getBrightness should beEqualTo(m.value)
      }
    }
    "convert old magnitudes to new" in {
      val mag = new skyobject.Magnitude(skyobject.Magnitude.Band.J, 10)
      impl.oldMagnitude2New(mag).value should beEqualTo(10)
      impl.oldMagnitude2New(mag).band should beEqualTo(MagnitudeBand.J)
    }
    "convert old Angles to new" in {
      forAll { (a: Angle) =>
        val oldAngle = skycalc.Angle.degrees(a.toDegrees)
        impl.oldAngle2New(oldAngle) ~= a
      }
    }
    "convert new Angles to old" in {
      forAll { (a: Angle) =>
        impl.newAngle2Old(a).toDegrees.getMagnitude should beCloseTo(a.toDegrees, 0.001)
      }
    }
    "convert old Coordinates to new" in {
      forAll { (c: Coordinates) =>
        val ra = skycalc.Angle.degrees(c.ra.toAngle.toDegrees)
        val dec = skycalc.Angle.degrees(c.dec.toAngle.toDegrees)
        val oldCoordinates = new skycalc.Coordinates(ra, dec)
        impl.oldCoordinates2New(oldCoordinates) ~= oldCoordinates
      }
    }
    "convert SiderealTarget to SkyObject" in {
      forAll { (c: SiderealTarget) =>
        val so = impl.siderealTarget2SkyObject(c)
        so.getName shouldEqual c.name
        so.getCoordinates.toHmsDeg(0).getRa.toDegrees.getMagnitude should beCloseTo(c.coordinates.ra.toAngle.toDegrees, 0.001)
        so.getCoordinates.toHmsDeg(0).getDec.toDegrees.getMagnitude should beCloseTo(c.coordinates.dec.toAngle.toDegrees, 0.001)
        so.getMagnitudes.size() should beEqualTo(c.magnitudes.size)
      }
    }
    "convert SkyObject to SiderealTarget" in {
      forAll { (c: Coordinates, mag: Magnitude) =>
        (mag.band != MagnitudeBand.G && mag.band != MagnitudeBand.Z) ==> {
          val coord = new skyobject.coords.HmsDegCoordinates.Builder(c.ra.toAngle, c.dec.toAngle).build()
          val so = new skyobject.SkyObject.Builder("name", coord).magnitudes(mag).build()
          val t = impl.skyObject2SiderealTarget(so)
          t.name shouldEqual "name"
          t.coordinates ~= c
          t.magnitudeOn(mag.band) should beSome(mag.copy(error = None, system = MagnitudeSystem.VEGA))
        }
      }
    }
  }
}
