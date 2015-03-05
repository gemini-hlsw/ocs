package edu.gemini.ags.impl

import edu.gemini.shared.skyobject
import edu.gemini.skycalc
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.system.HmsDegTarget
import org.specs2.ScalaCheck
import org.scalacheck.Prop._
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps

import scalaz._
import Scalaz._

class ConversionsSpec extends Specification with ScalaCheck with Arbitraries {
  "implicit conversions of model classes" should {
    "convert new bands to old bands" in {
      forAll { (b: MagnitudeBand) =>
        val mag = b.toOldModel
        skyobject.Magnitude.Band.values().toList should contain(mag)
      }
    }
    "convert old bands to new bands" in {
      for {
        m <- skyobject.Magnitude.Band.values().toList
        b = m.toNewModel
      } yield MagnitudeBand.all should contain(b)
    }
    "convert new magnitudes to old" in {
      forAll { (m: Magnitude) =>
        val mag = m.toOldModel
        mag.getBrightness should beEqualTo(m.value)
      }
    }
    "convert old magnitudes to new" in {
      val mag = new skyobject.Magnitude(skyobject.Magnitude.Band.J, 10)
      mag.toNewModel.value should beEqualTo(10)
      mag.toNewModel.band should beEqualTo(MagnitudeBand.J)
    }
    "convert old Angles to new" in {
      forAll { (a: Angle) =>
        val oldAngle = skycalc.Angle.degrees(a.toDegrees)
        oldAngle.toNewModel ~= a
      }
    }
    "convert new Angles to old" in {
      forAll { (a: Angle) =>
        a.toOldModel.toDegrees.getMagnitude should beCloseTo(a.toDegrees, 0.001)
      }
    }
    "convert old Coordinates to new" in {
      forAll { (c: Coordinates) =>
        val ra = skycalc.Angle.degrees(c.ra.toAngle.toDegrees)
        val dec = skycalc.Angle.degrees(c.dec.toAngle.toDegrees)
        val oldCoordinates = new skycalc.Coordinates(ra, dec)
        oldCoordinates.toNewModel ~= c
      }
    }
    "convert SiderealTarget to SkyObject" in {
      forAll { (c: SiderealTarget) =>
        val so = c.toOldModel
        so.getName shouldEqual c.name
        so.getCoordinates.toHmsDeg(0).getRa.toDegrees.getMagnitude should beCloseTo(c.coordinates.ra.toAngle.toDegrees, 0.001)
        so.getCoordinates.toHmsDeg(0).getDec.toDegrees.getMagnitude should beCloseTo(c.coordinates.dec.toAngle.toDegrees, 0.001)
        c.properMotion.map { pm =>
          so.getHmsDegCoordinates.getPmDec.toDegrees.getMagnitude should beCloseTo(pm.deltaDec.toDegrees, 0.001)
          so.getHmsDegCoordinates.getPmRa.toDegrees.getMagnitude should beCloseTo(pm.deltaRA.toDegrees, 0.001)
        }
        so.getMagnitudes.size() should beEqualTo(c.magnitudes.size)
      }
    }
    "convert SkyObject to SiderealTarget" in {
      forAll { (c: Coordinates, mag: Magnitude, properMotion: Option[ProperMotion]) =>
        (mag.band != MagnitudeBand.G && mag.band != MagnitudeBand.Z) ==> {
          val coord = properMotion.map { pm => new skyobject.coords.HmsDegCoordinates.Builder(c.ra.toAngle.toOldModel, c.dec.toAngle.toOldModel).pmDec(pm.deltaDec.toOldModel).pmRa(pm.deltaRA.toOldModel).build() }
            .getOrElse(new skyobject.coords.HmsDegCoordinates.Builder(c.ra.toAngle.toOldModel, c.dec.toAngle.toOldModel).build())
          val so = new skyobject.SkyObject.Builder("name", coord).magnitudes(mag.toOldModel).build()
          val t = so.toNewModel
          t.name shouldEqual "name"
          t.coordinates ~= c
          (t.properMotion |@| properMotion)(_ ~= _).getOrElse {properMotion should beNone}
          t.magnitudeIn(mag.band) should beSome(mag.copy(error = None, system = MagnitudeSystem.VEGA))
        }
      }
    }
    "convert SPTarget to SiderealTarget" in {
      forAll { (c: Coordinates, mag: Magnitude, properMotion: Option[ProperMotion]) =>
        (mag.band != MagnitudeBand.G && mag.band != MagnitudeBand.Z) ==> {
          val coord = properMotion.map { pm => new skyobject.coords.HmsDegCoordinates.Builder(c.ra.toAngle.toOldModel, c.dec.toAngle.toOldModel).pmDec(pm.deltaDec.toOldModel).pmRa(pm.deltaRA.toOldModel).build() }
            .getOrElse(new skyobject.coords.HmsDegCoordinates.Builder(c.ra.toAngle.toOldModel, c.dec.toAngle.toOldModel).build())
          val so = new skyobject.SkyObject.Builder("name", coord).magnitudes(mag.toOldModel).build()
          val target = new SPTarget(HmsDegTarget.fromSkyObject(so))
          val t = target.toNewModel
          t.name shouldEqual "name"
          t.coordinates ~= c
          (t.properMotion |@| properMotion)(_ ~= _).getOrElse {properMotion should beNone}
          t.magnitudeIn(mag.band) should beSome(mag.copy(error = None, system = MagnitudeSystem.VEGA))
        }
      }
    }
  }
}
