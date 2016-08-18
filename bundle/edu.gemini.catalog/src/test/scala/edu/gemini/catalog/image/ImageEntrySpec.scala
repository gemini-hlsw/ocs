package edu.gemini.catalog.image

import java.io.File

import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers, OptionValues}

class ImageEntrySpec extends FlatSpec with Matchers with PropertyChecks with ImageCatalogArbitraries with OptionValues {
  "ImageEntry" should
    "return a generated index with production files on production mode" in {
      forAll { (query: ImageSearchQuery) =>
        val suffix = ".fits.gz"
        ImageEntry.entryFromFile(new File(query.fileName(suffix))).value.query == query
      }
    }
    it should "ignore bad names" in {
      forAll { (query: ImageSearchQuery) =>
        val suffix = ".fits.gz"
        ImageEntry.entryFromFile(new File("A" + query.fileName(suffix))) shouldBe None
      }
    }
}
