package edu.gemini.catalog.image

import edu.gemini.spModel.core.Coordinates
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class ImageSearchQuerySpec extends FlatSpec with Matchers with PropertyChecks with ImageCatalogArbitraries {
  "ImageSearchQuery" should
    "generate an appropriate filename" in {
      forAll { (catalog: ImageCatalog, c: Coordinates) =>
        val suffix = ".fits.gz"
        ImageSearchQuery(catalog, c).fileName(suffix) should fullyMatch regex ImageEntry.fileRegex
      }
    }
}
