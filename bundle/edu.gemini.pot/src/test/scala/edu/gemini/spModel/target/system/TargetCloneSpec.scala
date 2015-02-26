package edu.gemini.spModel.target.system

import edu.gemini.shared.skyobject.Magnitude
import edu.gemini.spModel.target.system.CoordinateParam.Units.DEGREES
import org.specs2.mutable.Specification

class TargetCloneSpec extends Specification {

  class Checker[T <: ITarget](mk: => T) {
    // ensure that a given property is independent of the clone source, by modifying it in the
    // clone and comparing with the original. this is a sanity check
    def checkProp[A](get: T => A)(set: A => T => Unit) = {
      val a = mk
      val b = a.clone().asInstanceOf[T]
      val p = get(a)
      set(p)(b) // modify b
      (get(a) must_== p) and // must be unchanged in a
      (get(b) must_!= p)     // must be changed   in b
    }
  }

  "HmsDegTarget Clone Properties" >> {
    val c = new Checker(new HmsDegTarget)
    "Dec"                       in c.checkProp(_.getDec.getAs(DEGREES))(a => _.getDec.setAs(a + 1, DEGREES))
    "Dec"                       in c.checkProp(_.getRa.getAs(DEGREES))(a => _.getRa.setAs(a + 1, DEGREES))
    "Epoch"                     in c.checkProp(_.getEpoch.getValue)(a => _.getEpoch.setValue(a + 1))
    "Magnitudes"                in c.checkProp(_.getMagnitudes)(a => _.setMagnitudes(a.cons(new Magnitude(Magnitude.Band.N, 123))))
    "Name"                      in c.checkProp(_.getName)(a => _.setName(a + "x"))
    "Parallax"                  in c.checkProp(_.getParallax.getValue)(a => _.getParallax.setValue(a + 1))
    "PM1"                       in c.checkProp(_.getPM1.getValue)(a => _.getPM1.setValue(a + 1))
    "PM2"                       in c.checkProp(_.getPM2.getValue)(a => _.getPM2.setValue(a + 1))
    "PropMotionDec"             in c.checkProp(_.getPropMotionDec)(a => _.setPropMotionDec(a + 1))
    "PropMotionRA"              in c.checkProp(_.getPropMotionRA)(a => _.setPropMotionRA(a + 1))
    "Ra"                        in c.checkProp(_.getRa.getAs(DEGREES))(a => _.getRa.setAs(a + 1, DEGREES))
    "TrackingEpoch"             in c.checkProp(_.getTrackingEpoch)(a => _.setTrackingEpoch(a + 1))
    "TrackingParallax"          in c.checkProp(_.getTrackingParallax)(a => _.setTrackingParallax(a + 1))
    "TrackingRadialVelocity"    in c.checkProp(_.getTrackingRadialVelocity)(a => _.setTrackingRadialVelocity(a + 1))
  }

  "ConicTarget Clone Properties" >> {
    val c = new Checker(new ConicTarget())
    "ANode"                     in c.checkProp(_.getANode.getValue)(a => _.getANode.setValue(a + 1))
    "AQ"                        in c.checkProp(_.getAQ.getValue)(a => _.getAQ.setValue(a + 1))
    "DateForPosition"           in c.checkProp(_.getDateForPosition)(a => _.setDateForPosition(new java.util.Date()))
    "Dec"                       in c.checkProp(_.getRa.getAs(DEGREES))(a => _.getRa.setAs(a + 1, DEGREES))
    "E"                         in c.checkProp(_.getE)(a => _.setE(a + 1))
    "Epoch"                     in c.checkProp(_.getEpoch.getValue)(a => _.getEpoch.setValue(a + 1))
    "EpochOfPeri"               in c.checkProp(_.getEpochOfPeri.getValue)(a => _.getEpochOfPeri.setValue(a + 1))
    "HorizonsObjectId"          in c.checkProp(_.getHorizonsObjectId)(a => _.setHorizonsObjectId(10))
    "HorizonsObjectTypeOrdinal" in c.checkProp(_.getHorizonsObjectTypeOrdinal)(a => _.setHorizonsObjectTypeOrdinal(a + 1))
    "Inclination"               in c.checkProp(_.getInclination.getValue)(a => _.getInclination.setValue(a + 1))
    "LM"                        in c.checkProp(_.getLM.getValue)(a => _.getLM.setValue(a + 1))
    "Magnitudes"                in c.checkProp(_.getMagnitudes)(a => _.setMagnitudes(a.cons(new Magnitude(Magnitude.Band.N, 123))))
    "N"                         in c.checkProp(_.getN.getValue)(a => _.getN.setValue(a + 1))
    "Name"                      in c.checkProp(_.getName)(a => _.setName(a + "x"))
    "Perihelion"                in c.checkProp(_.getPerihelion.getValue)(a => _.getPerihelion.setValue(a + 1))
    "Ra"                        in c.checkProp(_.getRa.getAs(DEGREES))(a => _.getRa.setAs(a + 1, DEGREES))
  }

  "NamedTarget Clone Properties" >> {
    val c = new Checker(new NamedTarget())
    "DateForPosition"           in c.checkProp(_.getDateForPosition)(a => _.setDateForPosition(new java.util.Date()))
    "Dec"                       in c.checkProp(_.getRa.getAs(DEGREES))(a => _.getRa.setAs(a + 1, DEGREES))
    "Epoch"                     in c.checkProp(_.getEpoch.getValue)(a => _.getEpoch.setValue(a + 1))
    "HorizonsObjectId"          in c.checkProp(_.getHorizonsObjectId)(a => _.setHorizonsObjectId(10))
    "HorizonsObjectTypeOrdinal" in c.checkProp(_.getHorizonsObjectTypeOrdinal)(a => _.setHorizonsObjectTypeOrdinal(a + 1))
    "Magnitudes"                in c.checkProp(_.getMagnitudes)(a => _.setMagnitudes(a.cons(new Magnitude(Magnitude.Band.N, 123))))
    "Name"                      in c.checkProp(_.getName)(a => _.setName(a + "x"))
    "Ra"                        in c.checkProp(_.getRa.getAs(DEGREES))(a => _.getRa.setAs(a + 1, DEGREES))
    "SolarObject"               in c.checkProp(_.getSolarObject)(a => _.setSolarObject(NamedTarget.SolarObject.NEPTUNE))
  }

}
