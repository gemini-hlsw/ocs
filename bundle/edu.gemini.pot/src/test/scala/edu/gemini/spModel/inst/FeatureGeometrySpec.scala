package edu.gemini.spModel.inst

import edu.gemini.shared.util.immutable.ImPolygon
import edu.gemini.spModel.inst.FeatureGeometry.approximateArea
import org.specs2.mutable.Specification
import java.awt.geom._

import scala.math.Pi

object FeatureGeometrySpec extends Specification {
  val delta = 0.000001

  val rotate27 = AffineTransform.getRotateInstance(Math.toRadians(27))

  val triangle = ImPolygon(List((0.0, 0.0), (0.0, 10.0), (5.0, 5.0)))
  val square   = new Rectangle2D.Double(0.0, 0.0, 10.0, 10.0)
  val circle   = new Ellipse2D.Double(0, 0, 10, 10)

  val crossingTriangles = ImPolygon(List(
    (0.0, 0.0), (0.0, 10.0), (10.0, 0.0), (10.0, 10.0)
  ))


  "approximateArea" should {
    "handle regular polygons" in {
      approximateArea(square) must beCloseTo(100.0, delta)
    }

    "handle irregular polygons" in {
      val a = new Area(square)
      a.subtract(new Area(triangle))
      approximateArea(a) must beCloseTo(75.0, delta)
    }

    "handle rotated shapes" in {
      approximateArea(rotate27.createTransformedShape(square)) must beCloseTo(100.0, delta)
    }

    "handle disjoint unions" in {
      val farAwayTriangle = AffineTransform.getTranslateInstance(100.0, 100.0).createTransformedShape(triangle)
      val a = new Area(square)
      a.add(new Area(farAwayTriangle))
      approximateArea(a) must beCloseTo(125.0, delta)
    }

    "handle crossing edges" in {
      approximateArea(crossingTriangles) must beCloseTo(50.0, delta)
    }

    "handle approximate curves" in {
      approximateArea(circle) must beCloseTo(Pi * 25.0, 5.0)
    }

    "handle empty shapes" in {
      val gp = new GeneralPath()
      gp.moveTo(0,0)
      approximateArea(gp) must beCloseTo(0.0, delta)
    }

    "handle lines" in {
      val gp = new GeneralPath()
      gp.moveTo(0,0)
      gp.lineTo(10,10)
      approximateArea(gp) must beCloseTo(0.0, delta)
    }

    "handle open shapes" in {
      val gp = new GeneralPath()
      gp.moveTo(0,0)
      gp.lineTo(10,10)
      gp.lineTo(20, 0)
      approximateArea(gp) must beCloseTo(100.0, delta)
    }
  }

}
