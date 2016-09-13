package edu.gemini.catalog.image

import java.util.UUID

import argonaut.{CodecJson, DecodeResult}
import edu.gemini.catalog.image.ObservationCatalogOverrides.CatalogOverride
import edu.gemini.pot.sp.SPNodeKey
import org.scalacheck.Prop.forAll
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary._
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class OverridesJsonCodecsSpec extends FlatSpec with Matchers with PropertyChecks with ImageCatalogArbitraries {

  implicit val arbUUID: Arbitrary[UUID] = Arbitrary {
    for {
      least <- arbitrary[Long]
      most  <- arbitrary[Long]
    } yield new UUID(most, least)
  }

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
      println(c.encode(value).toString())
      c.decodeJson(c.encode(value)) shouldBe DecodeResult.ok(value)
    }

  "UUID.randomUUID()" should
    "support catalog override" in {
      roundTrip[CatalogOverride]
    }

}
