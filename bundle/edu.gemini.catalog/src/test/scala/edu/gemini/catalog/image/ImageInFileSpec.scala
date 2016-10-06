package edu.gemini.catalog.image

import java.io.File

import edu.gemini.spModel.core.{Angle, Coordinates, RightAscension, Declination}
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers, OptionValues}

class ImageInFileSpec extends FlatSpec with Matchers with PropertyChecks with ImageCatalogArbitraries with OptionValues {
  // Do more tests to uncover edge cases
  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(minSize = 10000, minSuccessful = 10000)

  val delta = Angle.fromArcsecs(0.01)
  val raWidth = Angle.fromArcmin(8.5)
  val raHalfWidth = (raWidth / 2).getOrElse(Angle.zero)
  val decHeight = Angle.fromArcmin(10)
  val decHalfHeight = (decHeight / 2).getOrElse(Angle.zero)
  val gap = Angle.zero
  val imageSize = AngularSize(raWidth, decHeight)

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
    ignore should "find near on RA decrease" in {
      forAll { (c: Coordinates) =>
        // too far in Ra plus
        val raUpd = c.copy(ra = c.ra.offset(raHalfWidth - delta))
        val ref = ImageSearchQuery(DssGemini, raUpd, imageSize)
        val img = ImageInFile(ref, new File("").toPath, 0)
        img.contains(c) shouldBe true
      }
    }
    ignore should "not find near on RA increase" in {
      forAll { (c: Coordinates) =>
        // too far in Ra plus
        val raUpd = c.copy(ra = c.ra.offset(raHalfWidth + delta))
        val ref = ImageSearchQuery(DssGemini, raUpd, imageSize)
        val img = ImageInFile(ref, new File("").toPath, 0)
        img.contains(c) shouldBe false
      }
    }
    ignore should "not find near on RA decrease" in {
      forAll { (c: Coordinates) =>
        val size = AngularSize(raWidth, decHeight)
        // too far in Ra minus
        val raUpd = c.copy(ra = c.ra.offset(raWidth - delta))
        val ref = ImageSearchQuery(DssGemini, raUpd, imageSize)
        val img = ImageInFile(ref, new File("").toPath, 0)
        img.contains(c) shouldBe false
      }
    }
    ignore should "not find near on Dec increase" in {
      forAll { (c: Coordinates) =>
        // too far in dec plus
        val decUpd = c.copy(dec = c.dec.offset(decHeight + delta)._1)
        val ref = ImageSearchQuery(DssGemini, decUpd, imageSize)
        val img = ImageInFile(ref, new File("").toPath, 0)
        img.contains(c) shouldBe false
      }
    }
    ignore should "not find near on Dec decrease" in {
      forAll { (c: Coordinates) =>
        // too far in Dec minus
        val decUpd = c.copy(dec = c.dec.offset(decHeight - delta)._1)
        val ref = ImageSearchQuery(DssGemini, decUpd, imageSize)
        val img = ImageInFile(ref, new File("").toPath, 0)
        img.contains(c) shouldBe false
      }
    }
    ignore should "find inside at declination zero" in {
      forAll { (ra: RightAscension) =>
        val c0 = Coordinates.zero.copy(ra = ra)
        val q0 = ImageSearchQuery(DssGemini, c0, imageSize)
        val img0 = ImageInFile(q0, new File("").toPath, 0)
        img0.contains(c0) shouldBe true
        val c2 = c0.copy(ra = ra.offset(raHalfWidth - delta))
        img0.contains(c2) shouldBe true
        val c3 = c0.copy(ra = RightAscension.fromAngle(ra.toAngle - raHalfWidth + delta))
        img0.contains(c3) shouldBe true
      }
    }
    ignore should "find inside at declination plus" in {
      forAll { (ra: RightAscension) =>
        var ra = RightAscension.fromAngle(Angle.fromDegrees(30))
        //var ra = RightAscension.zero
        val dec = Declination.fromAngle(Angle.fromDegrees(60)).get
        val c0 = Coordinates.zero.copy(ra = ra, dec = dec)
        val q0 = ImageSearchQuery(DssGemini, c0, imageSize)
        val img0 = ImageInFile(q0, new File("").toPath, 0)
        img0.contains(c0) shouldBe true
        val c2 = c0.copy(ra = ra.offset(raHalfWidth - delta))
        img0.contains(c2) shouldBe true
        val c3 = c0.copy(ra = RightAscension.fromAngle(ra.toAngle - raHalfWidth + delta))
        img0.contains(c3) shouldBe true
      }
    }
    ignore should "find inside at declination minus" in {
      //forAll { (ra: RightAscension) =>
        var ra = RightAscension.fromAngle(Angle.fromDegrees(30))
        //var ra = RightAscension.zero
        val dec = Declination.fromAngle(Angle.fromDegrees(360 - 60)).get
        val c0 = Coordinates.zero.copy(ra = ra, dec = dec)
        val q0 = ImageSearchQuery(DssGemini, c0, imageSize)
        val img0 = ImageInFile(q0, new File("").toPath, 0)
        img0.contains(c0) shouldBe true
        val c2 = c0.copy(ra = ra.offset(raHalfWidth - delta))
        img0.contains(c2) shouldBe true
        val c3 = c0.copy(ra = RightAscension.fromAngle(ra.toAngle - raHalfWidth + delta))
        img0.contains(c3) shouldBe true
    }
}
