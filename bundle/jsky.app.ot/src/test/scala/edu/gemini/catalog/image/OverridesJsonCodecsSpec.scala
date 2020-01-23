package edu.gemini.catalog.image

import java.util.UUID

import argonaut.{CodecJson, DecodeResult}
import edu.gemini.catalog.ui.image.ObservationCatalogOverrides.CatalogOverride
import edu.gemini.pot.sp.SPNodeKey
import org.scalacheck.Prop.forAll
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class OverridesJsonCodecsSpec extends FlatSpec with Matchers with ScalaCheckPropertyChecks with ImageCatalogArbitraries {

  implicit val arbSPNodeKey: Arbitrary[SPNodeKey] = Arbitrary {
    for {
      key <- arbitrary[UUID]
    } yield new SPNodeKey(key)
  }

  implicit val arbCatalogOverride: Arbitrary[CatalogOverride] =
    Arbitrary {
      for {
        key <- arbitrary[SPNodeKey]
        c   <- arbitrary[ImageCatalog]
      } yield CatalogOverride(key, c)
    }

  def roundTrip[A: CodecJson: Arbitrary](implicit c: CodecJson[A]) =
    forAll { (value: A) =>
      c.decodeJson(c.encode(value)) shouldBe DecodeResult.ok(value)
    }

  "UUID.randomUUID()" should
    "support catalog override" in {
      roundTrip[CatalogOverride]
    }

}
