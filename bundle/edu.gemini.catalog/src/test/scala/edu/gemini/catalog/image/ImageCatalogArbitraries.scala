package edu.gemini.catalog.image

import java.io.File

import edu.gemini.spModel.core.{Arbitraries, Coordinates}
import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ImageCatalogArbitraries extends Arbitraries {
  implicit val arbCatalog: Arbitrary[ImageCatalog] = Arbitrary(Gen.oneOf(ImageCatalog.all))

  implicit val arbImageSearchQuery: Arbitrary[ImageSearchQuery] = Arbitrary {
    for {
      catalog <- arbitrary[ImageCatalog]
      coord   <- arbitrary[Coordinates]
    } yield ImageSearchQuery(catalog, coord)
  }

  implicit val arbFile: Arbitrary[File] = Arbitrary {
    arbitrary[String].map(new File(_))
  }

  implicit val arbImageEntry: Arbitrary[ImageEntry] = Arbitrary {
    for {
      query <- arbitrary[ImageSearchQuery]
      file  <- arbitrary[File]
    } yield ImageEntry(query, file.toPath, file.length)
  }
}
