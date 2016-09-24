package edu.gemini.catalog.image

import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension}
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen._
import org.scalacheck.{Arbitrary, Shrink}
import org.scalatest.enablers.CheckerAsserting
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Assertion, FlatSpec, Matchers}

import scalaz._
import Scalaz._

class ImageSearchQuerySpec extends FlatSpec with Matchers with PropertyChecks with ImageCatalogArbitraries {
  case class TestCase(catalog: ImageCatalog, c: Coordinates, delta: Angle)

  // Custom arbitrary to properly scala the delta
  def testCase(min: Angle, max: Angle): Arbitrary[TestCase] = Arbitrary {
    for {
      catalog <- arbitrary[ImageCatalog]
      coords  <- arbitrary[Coordinates]
      delta   <- choose(min.toArcmins, max.toArcmins).map(Angle.fromArcmin)
    } yield TestCase(catalog, coords, delta)
  }

  "ImageSearchQuery" should
    "generate an appropriate filename" in {
      forAll { (catalog: ImageCatalog, c: Coordinates) =>
        val suffix = ".fits.gz"
        ImageSearchQuery(catalog, c).fileName(suffix) should fullyMatch regex ImageEntry.fileRegex
      }
    }

  "Comparing on distance" should "compare to itself" in {
      forAll { (catalog: ImageCatalog, c: Coordinates) =>
        ImageSearchQuery(catalog, c).isNearby(ImageSearchQuery(catalog, c)) shouldBe true
      }
    }
    it should "be nearby close coordinates in ra" in {
      forAll { (t: TestCase) =>
        val c1 = t.c.copy(ra = t.c.ra.offset(t.delta))
        ImageSearchQuery(t.catalog, t.c).isNearby(ImageSearchQuery(t.catalog, c1)) shouldBe true
      }(implicitly[PropertyCheckConfiguration], testCase(Angle.zero, ImageSearchQuery.maxDistance), implicitly[Shrink[TestCase]], implicitly[CheckerAsserting[Assertion]])
    }
    it should "not be nearby farther coordinates in ra" in {
      forAll { (t: TestCase) =>
        val c1 = t.c.copy(ra = t.c.ra.offset(t.delta))
        ImageSearchQuery(t.catalog, t.c).isNearby(ImageSearchQuery(t.catalog, c1)) shouldBe false
      }(implicitly[PropertyCheckConfiguration], testCase(ImageSearchQuery.maxDistance, ImageSearchQuery.maxDistance * 2), implicitly[Shrink[TestCase]], implicitly[CheckerAsserting[Assertion]])
    }
    it should "be nearby close coordinates in dec" in {
      forAll { (t: TestCase) =>
        val c1 = t.c.copy(dec = t.c.dec.offset(t.delta)._1)
        ImageSearchQuery(t.catalog, t.c).isNearby(ImageSearchQuery(t.catalog, c1)) shouldBe true
      }(implicitly[PropertyCheckConfiguration], testCase(Angle.zero, ImageSearchQuery.maxDistance), implicitly[Shrink[TestCase]], implicitly[CheckerAsserting[Assertion]])
    }
    it should "not be nearby farther coordinates in dec" in {
      forAll { (t: TestCase) =>
        val c1 = t.c.copy(dec = t.c.dec.offset(t.delta)._1)
        ImageSearchQuery(t.catalog, t.c).isNearby(ImageSearchQuery(t.catalog, c1)) shouldBe false
      }(implicitly[PropertyCheckConfiguration], testCase(ImageSearchQuery.maxDistance, Angle.fromArcmin(2 * ImageSearchQuery.maxDistance.toArcmins)), implicitly[Shrink[TestCase]], implicitly[CheckerAsserting[Assertion]])
    }
    it should "be symmetric" in {
      forAll { (t: TestCase) =>
        val c1 = t.c.copy(dec = t.c.dec.offset(t.delta)._1)
        ImageSearchQuery(t.catalog, t.c).isNearby(ImageSearchQuery(t.catalog, c1)) shouldBe ImageSearchQuery(t.catalog, c1).isNearby(ImageSearchQuery(t.catalog, t.c))
      }(implicitly[PropertyCheckConfiguration], testCase(Angle.zero, Angle.fromDegrees(359.99)), implicitly[Shrink[TestCase]], implicitly[CheckerAsserting[Assertion]])
    }
    it should "work near zero" in {
      // Special case when the diff is very close to zero but negative
      val c1 = Coordinates(RightAscension.fromAngle(Angle.fromDegrees(263.94917083333326)),Declination.fromAngle(Angle.fromDegrees(329.5302805555556)).getOrElse(Declination.zero))
      val c2 = Coordinates(RightAscension.fromAngle(Angle.fromDegrees(263.94917)),Declination.fromAngle(Angle.fromDegrees(329.53027999999995)).getOrElse(Declination.zero))
      ImageSearchQuery(DssGemini, c1).isNearby(ImageSearchQuery(DssGemini, c2)) shouldBe true
      ImageSearchQuery(DssGemini, c2).isNearby(ImageSearchQuery(DssGemini, c1)) shouldBe true
    }

}
