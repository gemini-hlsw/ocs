package edu.gemini.catalog.image

import org.scalatest.{FlatSpec, Matchers, OptionValues}
import org.scalatest.prop.PropertyChecks

import scalaz._
import Scalaz._

class StoredImagesCacheSpec extends FlatSpec with Matchers with PropertyChecks with ImageCatalogArbitraries with OptionValues {
  "StoredImagesCache" should
    "start empty" in {
      val cache = StoredImagesCache.get.unsafePerformSyncAttempt
      cache.isRight shouldBe true
      cache.getOrElse(fail("Should not happen")) shouldBe StoredImages.zero
    }
    it should "add entries" in {
      forAll { (e: ImageEntry) =>
        val cache = StoredImagesCache.add(e).unsafePerformSyncAttempt
        cache.isRight shouldBe true
        cache.getOrElse(fail("Should not happen")).images should contain(e)
      }
    }
    it should "delete entries" in {
      forAll { (e: ImageEntry) =>
        val cache = (StoredImagesCache.add(e) *> StoredImagesCache.remove(e)).unsafePerformSyncAttempt
        cache.isRight shouldBe true
        cache.getOrElse(fail("Should not happen")).images should not contain e
      }
    }
    it should "find entries and clean" in {
      forAll { (e: ImageEntry) =>
        val entry = (StoredImagesCache.add(e) >> StoredImagesCache.find(e.query)).unsafePerformSyncAttempt
        entry.isRight shouldBe true
        entry.getOrElse(fail("Should not happen")).value shouldBe e
      }
    }
}
