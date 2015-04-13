package edu.gemini.ags.gems

import edu.gemini.ags.impl._
import edu.gemini.catalog.api._
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw
import edu.gemini.spModel.gems.GemsGuideStarType
import org.specs2.mutable.Specification
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

class GemsUtils4JavaSpec extends Specification {
  "GemsUtils4" should {
    "sort targets by R magnitude" in {
      import scala.collection.JavaConverters._
      val st1 = SiderealTarget("n", Coordinates.zero, None, List(new Magnitude(10.0, MagnitudeBand.J)), None)
      GemsUtils4Java.sortTargetsByBrightness(List(st1).asJava).get(0) should beEqualTo(st1)

      val st2 = SiderealTarget("n", Coordinates.zero, None, List(new Magnitude(15.0, MagnitudeBand.J)), None)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2).asJava).get(0) should beEqualTo(st1)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2).asJava).get(1) should beEqualTo(st2)

      val st3 = SiderealTarget("n", Coordinates.zero, None, List(new Magnitude(15.0, MagnitudeBand.R)), None)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3).asJava).get(0) should beEqualTo(st3)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3).asJava).get(1) should beEqualTo(st1)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3).asJava).get(2) should beEqualTo(st2)

      val st4 = SiderealTarget("n", Coordinates.zero, None, List(new Magnitude(9.0, MagnitudeBand.R)), None)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4).asJava).get(0) should beEqualTo(st4)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4).asJava).get(1) should beEqualTo(st3)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4).asJava).get(2) should beEqualTo(st1)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4).asJava).get(3) should beEqualTo(st2)

      val st5 = SiderealTarget("n", Coordinates.zero, None, List(new Magnitude(19.0, MagnitudeBand.R)), None)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4, st5).asJava).get(0) should beEqualTo(st4)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4, st5).asJava).get(1) should beEqualTo(st3)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4, st5).asJava).get(2) should beEqualTo(st5)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4, st5).asJava).get(3) should beEqualTo(st1)
      GemsUtils4Java.sortTargetsByBrightness(List(st1, st2, st3, st4, st5).asJava).get(4) should beEqualTo(st2)
    }
  }
}
