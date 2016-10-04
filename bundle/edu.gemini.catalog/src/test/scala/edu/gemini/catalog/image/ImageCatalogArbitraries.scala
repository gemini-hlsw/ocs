package edu.gemini.catalog.image

import java.io.File
import java.nio.file.Path

import edu.gemini.spModel.core.{Angle, Arbitraries, Coordinates}
import org.scalacheck._
import org.scalacheck.Arbitrary._

trait ImageCatalogArbitraries extends Arbitraries {
  implicit val arbCatalog: Arbitrary[ImageCatalog] = Arbitrary(Gen.oneOf(ImageCatalog.all))

  implicit val arbAngularSize: Arbitrary[AngularSize] = Arbitrary {
    for {
      w <- Gen.choose(0.0, 30.0).map(Angle.fromArcmin)
      h <- Gen.choose(0.0, 30.0).map(Angle.fromArcmin)
    } yield AngularSize(w, h)
  }

  implicit val arbImageSearchQuery: Arbitrary[ImageSearchQuery] = Arbitrary {
    for {
      catalog <- arbitrary[ImageCatalog]
      coord   <- arbitrary[Coordinates]
      size    <- arbitrary[AngularSize]
    } yield ImageSearchQuery(catalog, coord, size)
  }

  implicit val arbPath: Arbitrary[Path] = Arbitrary {
    // Use UUID to ensure the filename is valid
    Gen.uuid.map(u => new File(u.toString).toPath)
  }

  implicit val arbImageInFile: Arbitrary[ImageInFile] = Arbitrary {
    for {
      query <- arbitrary[ImageSearchQuery]
      file  <- arbitrary[Path]
      size  <- arbitrary[Long]
    } yield ImageInFile(query, file, size)
  }
}
