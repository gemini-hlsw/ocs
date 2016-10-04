package edu.gemini.catalog.image

import java.io.File

import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers, OptionValues}
import org.scalatest.prop.PropertyChecks

import scalaz._
import Scalaz._

class StoredImagesCacheSpec extends FlatSpec with Matchers with PropertyChecks
  with ImageCatalogArbitraries with OptionValues with BeforeAndAfter {
  before {
    // Clean the cache before each test
    StoredImagesCache.clean.unsafePerformSync
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
    it should "find entries" in {
      forAll { (e: ImageInFile) =>
        val entry = (StoredImagesCache.add(e) >> StoredImagesCache.find(e.query)).unsafePerformSyncAttempt
        entry.isRight shouldBe true
        entry.getOrElse(fail("Should not happen")).value shouldBe e
      }
    }
    it should "find itself as closest" in {
      forAll { (e: ImageInFile) =>
        val entry = (StoredImagesCache.add(e) *> StoredImagesCache.get.map(_.closestImage(e.query))).unsafePerformSyncAttempt
        entry.isRight shouldBe true
        entry.getOrElse(fail("Should not happen")).map(_.query).value shouldBe e.query
      }
    }
    it should "find itself inside" in {
      forAll { (e: ImageInFile) =>
        val entry = (StoredImagesCache.add(e) *> StoredImagesCache.get.map(_.inside(e.query))).unsafePerformSyncAttempt
        entry.isRight shouldBe true
        entry.getOrElse(fail("Should not happen")).map(_.query).value shouldBe e.query
      }
    }
    it should "find itself as closest among several" in {
      forAll { (e: List[ImageInFile]) =>
        whenever(e.nonEmpty) {
          val head = e.head
          val entry = (e.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.closestImage(head.query))).unsafePerformSyncAttempt
          entry.isRight shouldBe true
          entry.getOrElse(fail("Should not happen")).map(_.query.coordinates).value shouldBe head.query.coordinates
        }
      }
    }
    it should "find itself inside among several" in {
      forAll { (e: List[ImageInFile]) =>
        whenever(e.nonEmpty) {
          val head = e.head
          val entry = (e.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.inside(head.query))).unsafePerformSyncAttempt
          entry.isRight shouldBe true
          entry.getOrElse(fail("Should not happen")).map(_.query.coordinates).value shouldBe head.query.coordinates
        }
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
    it should "find inside" in {
      val size = AngularSize(Angle.fromArcmin(8.5), Angle.fromArcmin(10))
      val a = ImageSearchQuery(DssGemini, Coordinates.zero, size)
      val b = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(RightAscension.fromAngle(Angle.fromArcmin(8.5 / 2) - DssGemini.overlapGap)), size), new File("b").toPath, 0)
      val c = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(RightAscension.fromAngle(Angle.fromArcmin(6))), size), new File("c").toPath, 0)

      val e = List(b, c)
      val entry = (e.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.inside(a))).unsafePerformSyncAttempt

      entry.isRight shouldBe true
      entry.getOrElse(fail("Should not happen")).value shouldBe b
    }
    it should "find net fit inside due to the overlap" in {
      val size = AngularSize(Angle.fromArcmin(8.5), Angle.fromArcmin(10))
      val a = ImageSearchQuery(DssGemini, Coordinates.zero, size)
      val b = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(RightAscension.fromAngle(Angle.fromArcmin(8.5 / 2))), size), new File("b").toPath, 0)
      val c = ImageInFile(ImageSearchQuery(DssGemini, Coordinates.zero.copy(RightAscension.fromAngle(Angle.fromArcmin(6))), size), new File("c").toPath, 0)

      val e = List(b, c)
      val entry = (e.map(StoredImagesCache.add).sequenceU >> StoredImagesCache.get.map(_.inside(a))).unsafePerformSyncAttempt

      entry.isRight shouldBe true
      entry.getOrElse(fail("Should not happen")) shouldBe None
    }
    it should "find non if nearest is farther than max distance" in {
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
