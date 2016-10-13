package edu.gemini.catalog.image

import java.io.File

import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers, OptionValues}

import scala.math._
import scalaz._
import Scalaz._

class ImageInFileSpec extends FlatSpec with Matchers with PropertyChecks with ImageCatalogArbitraries with OptionValues {
  // Do more tests to uncover edge cases
  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(minSize = 10000, minSuccessful = 10000)

  val delta: Angle = Angle.fromArcsecs(0.01)
  val raWidth: Angle = Angle.fromArcmin(8.5)
  val raHalfWidth: Angle= ~(raWidth / 2)
  val decHeight: Angle = Angle.fromArcmin(10)
  val decHalfHeight: Angle = ~(decHeight / 2)
  val imageSize: AngularSize = AngularSize(raWidth, decHeight)
  // Inside doesn't work properly at Zenith
  val zenithN: Declination = Declination.fromAngle(Angle.fromDegrees(90.0)).getOrElse(Declination.zero)
  val zenithS: Declination = Declination.fromAngle(Angle.fromDegrees(270.0)).getOrElse(Declination.zero)

  "ImageInFile" should
    "return a generated index with production files on production mode" in {
      forAll { (query: ImageSearchQuery) =>
        val suffix = "fits.gz"
        ImageInFile.entryFromFile(new File(query.fileName(suffix))).value.query == query
      }
    }
    it should "ignore bad names" in {
      forAll { (query: ImageSearchQuery) =>
        val suffix = "fits.gz"
        ImageInFile.entryFromFile(new File("A" + query.fileName(suffix))) shouldBe None
      }
    }
    it should "contain itself" in {
      forAll { (query: ImageSearchQuery) =>
        val suffix = "fits.gz"
        ImageInFile.entryFromFile(new File(query.fileName(suffix))).map(_.contains(query.coordinates)).value shouldBe true
      }
    }
    it should "provide a distance" in {
      forAll { (a: Angle, b: Angle) =>
        // The distance is always less than pi but higher-equal than 0
        ImageInFile.δ(a.toRadians, b.toRadians) should be <= ImageInFile.π
        ImageInFile.δ(a.toRadians, b.toRadians) should be >= 0.0
      }
    }
    it should "find inside on RA increase" in {
      forAll { (c: Coordinates) =>
        whenever (c.dec =/= zenithN && c.dec =/= zenithS) {
          // inside in Ra + width - delta
          val raUpd = c.offset(raHalfWidth - delta, Angle.zero)
          val ref = ImageSearchQuery(MassImgH, raUpd, imageSize)
          val img = ImageInFile(ref, new File("").toPath, 0)
          img.contains(c) shouldBe true
        }
      }
    }
    it should "find inside on RA decrease" in {
      forAll { (c: Coordinates) =>
        whenever (c.dec =/= zenithN && c.dec =/= zenithS) {
          // inside in Ra - (width - delta)
          val raUpd = c.offset(Angle.zero - (raHalfWidth - delta), Angle.zero)
          val ref = ImageSearchQuery(MassImgH, raUpd, imageSize)
          val img = ImageInFile(ref, new File("").toPath, 0)
          img.contains(c) shouldBe true
        }
      }
    }
    it should "not find inside on RA increase over range" in {
      forAll { (c: Coordinates) =>
        whenever (c.dec =/= zenithN && c.dec =/= zenithS) {
          // too far in Ra + 2*width
          val θ = cos(c.dec.toDegrees.toRadians)
          val raUpd = c.offset(~(raWidth / θ), Angle.zero)
          val ref = ImageSearchQuery(MassImgH, raUpd, imageSize)
          val img = ImageInFile(ref, new File("").toPath, 0)
          img.contains(c) shouldBe false
        }
      }
    }
    it should "not find inside on RA decrease over range" in {
      forAll { (c: Coordinates) =>
        whenever (c.dec =/= zenithN && c.dec =/= zenithS) {
          // too far in Ra - 2*width
          val θ = cos(c.dec.toDegrees.toRadians)
          val raUpd = c.offset(Angle.zero - ~(raWidth / θ), Angle.zero)
          val ref = ImageSearchQuery(MassImgH, raUpd, imageSize)
          val img = ImageInFile(ref, new File("").toPath, 0)
          img.contains(c) shouldBe false
        }
      }
    }
    it should "find inside on small Dec increase" in {
      forAll { (c: Coordinates) =>
        whenever (c.dec =/= zenithN && c.dec =/= zenithS) {
          // inside small dec increase
          val decUpd = c.offset(Angle.zero, decHalfHeight - delta)
          val ref = ImageSearchQuery(MassImgH, decUpd, imageSize)
          val img = ImageInFile(ref, new File("").toPath, 0)
          img.contains(c) shouldBe true
        }
      }
    }
    it should "not find inside on Dec increase over range" in {
      forAll { (c: Coordinates) =>
        whenever (c.dec =/= zenithN && c.dec =/= zenithS) {
          // too far in dec plus
          val decUpd = c.offset(Angle.zero, decHeight)
          val ref = ImageSearchQuery(MassImgH, decUpd, imageSize)
          val img = ImageInFile(ref, new File("").toPath, 0)
          img.contains(c) shouldBe false
        }
      }
    }
    it should "find inside on small Dec decrease" in {
      forAll { (c: Coordinates) =>
        whenever (c.dec =/= zenithN && c.dec =/= zenithS) {
          // inside small dec decrease
          val decUpd = c.offset(Angle.zero, Angle.zero - decHalfHeight - delta)
          val ref = ImageSearchQuery(DssGemini, decUpd, imageSize)
          val img = ImageInFile(ref, new File("").toPath, 0)
          img.contains(c) shouldBe false
        }
      }
    }
    it should "not find inside on Dec decrease over range" in {
      forAll { (c: Coordinates) =>
        whenever (c.dec =/= zenithN && c.dec =/= zenithS) {
          // too far in dec minus
          val decUpd = c.offset(Angle.zero, Angle.zero - decHeight)
          val ref = ImageSearchQuery(MassImgH, decUpd, imageSize)
          val img = ImageInFile(ref, new File("").toPath, 0)
          img.contains(c) shouldBe false
        }
      }
    }
    it should "find inside at declination zero" in {
      forAll { (ra: RightAscension) =>
        val c0 = Coordinates.zero.copy(ra = ra)
        val q0 = ImageSearchQuery(MassImgH, c0, imageSize)
        val img0 = ImageInFile(q0, new File("").toPath, 0)
        img0.contains(c0) shouldBe true
        val c2 = c0.copy(ra = ra.offset(raHalfWidth - delta))
        img0.contains(c2) shouldBe true
        val c3 = c0.copy(ra = RightAscension.fromAngle(ra.toAngle - raHalfWidth + delta))
        img0.contains(c3) shouldBe true
      }
    }
}
