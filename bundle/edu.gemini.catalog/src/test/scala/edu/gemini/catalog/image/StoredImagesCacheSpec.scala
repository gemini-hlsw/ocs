package edu.gemini.catalog.image

import java.io.File

import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers, OptionValues}
import org.scalatest.prop.PropertyChecks
import edu.gemini.spModel.core.AlmostEqual
import edu.gemini.spModel.core.AlmostEqual._

import scalaz._
import Scalaz._

class StoredImagesCacheSpec extends FlatSpec with Matchers with PropertyChecks
  with ImageCatalogArbitraries with OptionValues with BeforeAndAfterEach {

  // Do more tests to uncover edge cases
  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(minSize = 5000, minSuccessful = 5000)

  override def beforeEach {
    // Clean the cache before each test
    StoredImagesCache.clean.unsafePerformSync
  }

  implicit val AngularSizeAlmostEqual =
    new AlmostEqual[AngularSize] {
      def almostEqual(a: AngularSize, b: AngularSize) =
        (a.ra ~= b.ra) && (a.dec ~= b.dec)
    }

  implicit val ImageSearchQueryAlmostEqual =
    new AlmostEqual[ImageSearchQuery] {
      def almostEqual(a: ImageSearchQuery, b: ImageSearchQuery) =
        (a.coordinates ~= b.coordinates) && (a.size ~= b.size) && (a.catalog == b.catalog)
    }

  implicit val ImageInFileAlmostEqual =
    new AlmostEqual[ImageInFile] {
      def almostEqual(a: ImageInFile, b: ImageInFile) =
        (a.file == b.file) && (a.fileSize == b.fileSize) && (a.query ~= b.query)
    }

  "StoredImagesCache" should
    "start empty" in {
      val cache = StoredImagesCache.get.unsafePerformSyncAttempt
      cache.isRight shouldBe true
      cache.getOrElse(fail("Should not happen")) shouldBe StoredImages.zero
    }
    it should "add entries" in {
      forAll { (e: ImageInFile) =>
        val cache = StoredImagesCache.add(e).unsafePerformSyncAttempt
        cache.isRight shouldBe true
        cache.getOrElse(fail("Should not happen")).images should contain(e)
      }
    }
    it should "delete entries" in {
      forAll { (e: ImageInFile) =>
        val cache = (StoredImagesCache.add(e) *> StoredImagesCache.remove(e)).unsafePerformSyncAttempt
        cache.isRight shouldBe true
        cache.getOrElse(fail("Should not happen")).images should not contain e
      }
    }
    ignore should "find entries" in {
      forAll { (e: ImageInFile) =>
        val entry = (StoredImagesCache.add(e) *> StoredImagesCache.find(e.query)).unsafePerformSyncAttempt
        entry.isRight shouldBe true
        entry.getOrElse(fail("Should not happen")).value.query shouldBe e.query
      }
    }
    it should "find itself as closest" in {
      forAll { (e: ImageInFile) =>
        val entry = (StoredImagesCache.add(e) *> StoredImagesCache.get.map(_.closestImage(e.query))).unsafePerformSyncAttempt
        entry.isRight shouldBe true
        entry.getOrElse(fail("Should not happen")).map(_.query).value shouldBe e.query
      }
    }
    ignore should "find itself inside" in {
      forAll { (e: ImageInFile) =>
        val entry = (StoredImagesCache.add(e) *> StoredImagesCache.get.map(_.inside(e.query))).unsafePerformSyncAttempt
        entry.isRight shouldBe true
        entry.getOrElse(fail("Should not happen")).map(_.query).value shouldBe e.query
      }
    }
    ignore should "find itself as closest among several" in {
      forAll { (h: ImageInFile, e: List[ImageInFile]) =>
        val minSize = e.map(_.query.size).minimum.getOrElse(AngularSize.zero)
        val ref = h.copy(query = h.query.copy(size = minSize)) // Set size to the minimum available to make the test viable
        val images = ref :: e
        val entry = (images.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.closestImage(ref.query))).unsafePerformSyncAttempt
        entry.isRight shouldBe true
        entry.getOrElse(fail("Should not happen")).map(_ ~= ref).value shouldBe true
      }
    }
    ignore should "find itself inside among several" in {
      forAll { (h: ImageInFile, e: List[ImageInFile]) =>
        val minSize = e.map(_.query.size).minimum.getOrElse(AngularSize.zero)
        val ref = h.copy(query = h.query.copy(size = minSize)) // Set size to the minimum available to make the test viable
        val images = ref :: e
        val entry = (images.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.inside(ref.query))).unsafePerformSyncAttempt
        entry.isRight shouldBe true
        entry.getOrElse(fail("Should not happen")).map(_ ~= ref).value shouldBe true
      }
    }
    it should "find nearest" in {
      val size = AngularSize(Angle.fromArcmin(8.5), Angle.fromArcmin(10))
      val a = ImageSearchQuery(DssGemini, Coordinates.zero, size)
      val b = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(RightAscension.fromAngle(Angle.fromArcmin(4.5))), size), new File("b").toPath, 0)
      val c = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(RightAscension.fromAngle(Angle.fromArcmin(6))), size), new File("c").toPath, 0)

      val e = List(b, c)
      val entry = (e.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.closestImage(a))).unsafePerformSyncAttempt

      entry.isRight shouldBe true
      entry.getOrElse(fail("Should not happen")).value shouldBe b
    }
    ignore should "find no one inside if they don't overlap" in {
      forAll { (c: Coordinates) =>
        val delta = Angle.fromArcsecs(0.01)
        val raWidth = Angle.fromArcmin(8.5)
        val decHeight = Angle.fromArcmin(10)
        val gap = Angle.zero
        val size = AngularSize(raWidth, decHeight)
        val ref = ImageSearchQuery(DssGemini, c, size)
        // too far in Ra
        val ra1 = c.copy(ra = c.ra.offset(raWidth + delta))
        val ra2 = c.copy(ra = RightAscension.zero.max(c.ra.offset(Angle.zero - raWidth - delta)))
        // too far in dec
        val dec1 = c.copy(dec = c.dec.offset(decHeight + delta)._1)
        val dec2 = c.copy(dec = c.dec.offset(Angle.zero - decHeight - delta)._1)
        val nearEntries = List(ra1, ra2, dec1, dec2).map(c => ImageInFile(ImageSearchQuery(DssGemini, c, size), new File(c.toString).toPath, 0))
        val entry = (nearEntries.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.inside(ref))).unsafePerformSyncAttempt

        entry.isRight shouldBe true
        entry.toOption.value shouldBe None
      }
    }
    ignore should "find image inside" in {
      val raWidth = Angle.fromArcmin(8.5)
      val decWidth = Angle.fromArcmin(10)
      val gap = DssGemini.adjacentOverlap
      val size = AngularSize(Angle.fromArcmin(8.5), Angle.fromArcmin(10))
      val a = ImageSearchQuery(DssGemini, Coordinates.zero, size)
      val b = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(RightAscension.fromAngle((raWidth / 2).getOrElse(Angle.zero) - gap)), size), new File("b").toPath, 0)
      val c = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(RightAscension.fromAngle(Angle.fromArcmin(6))), size), new File("c").toPath, 0)

      val e = List(b, c)
      val entry = (e.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.inside(a))).unsafePerformSyncAttempt

      entry.isRight shouldBe true
      entry.getOrElse(fail("Should not happen")).value shouldBe b
    }
    ignore should "find no fit inside due to the overlap" in {
      val size = AngularSize(Angle.fromArcmin(8.5), Angle.fromArcmin(10))
      val a = ImageSearchQuery(DssGemini, Coordinates.zero, size)
      val b = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(RightAscension.fromAngle(Angle.fromArcmin(8.5 / 2))), size), new File("b").toPath, 0)
      val c = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(RightAscension.fromAngle(Angle.fromArcmin(6))), size), new File("c").toPath, 0)

      val e = List(b, c)
      val entry = (e.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.inside(a))).unsafePerformSyncAttempt

      entry.isRight shouldBe true
      entry.getOrElse(fail("Should not happen")) shouldBe None
    }
    it should "find none if the nearest is farther than max distance" in {
      val size = AngularSize(Angle.fromArcmin(8.5), Angle.fromArcmin(10))
      val a = ImageSearchQuery(DssGemini, Coordinates.zero, size)
      val b = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(dec = Declination.fromAngle(Angle.fromArcmin(5)).getOrElse(Declination.zero)), size), new File("b").toPath, 0)
      val c = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(dec = Declination.fromAngle(Angle.fromArcmin(15)).getOrElse(Declination.zero)), size), new File("b").toPath, 0)

      val e = List(b, c)
      val entry = (e.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.closestImage(a))).unsafePerformSyncAttempt

      entry.isRight shouldBe true
      entry.toOption.value shouldBe None
    }
}
