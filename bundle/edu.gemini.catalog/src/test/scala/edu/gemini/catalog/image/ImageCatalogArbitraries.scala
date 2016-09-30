package edu.gemini.catalog.image

import java.io.File
import java.nio.file.Path

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

  implicit val arbPath: Arbitrary[Path] = Arbitrary {
    // Use UUID to ensure the filename is valid
    Gen.uuid.map(u => new File(u.toString).toPath)
  }

  implicit val arbImageEntry: Arbitrary[ImageInFile] = Arbitrary {
    for {
      query <- arbitrary[ImageSearchQuery]
      file  <- arbitrary[Path]
      size  <- arbitrary[Long]
    } yield ImageInFile(query, file, size)
  }
}
