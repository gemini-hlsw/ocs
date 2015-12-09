package edu.gemini.gsa.query

import edu.gemini.gsa.core.{QaResponse, GsaRecord, Arbitraries}
import edu.gemini.gsa.query.JsonCodecs._
import edu.gemini.spModel.dataset.{DatasetQaState, DatasetLabel, DatasetMd5, DatasetGsaState}

import argonaut.{CodecJson, DecodeResult}

import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Arbitrary}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import java.time.Instant

import scalaz.syntax.id._

object JsonCodecsSpec extends Specification with ScalaCheck with Arbitraries {

  import GsaQaUpdateQuery.EitherQaResponse

  implicit val arbArchiveResponse: Arbitrary[EitherQaResponse] =
    Arbitrary {
      Gen.oneOf(Gen.alphaStr.map(_.left), arbQaResponse.arbitrary.map(_.right))
    }

  def roundTrip[A: CodecJson: Arbitrary](implicit mf: Manifest[A]) =
    mf.runtimeClass.getName ! forAll { (value: A) =>
      val c = implicitly[CodecJson[A]]
      c.decodeJson(c.encode(value)) must_== DecodeResult.ok(value)
    }

  "JSON Codecs" >> {
    roundTrip[DatasetGsaState]
    roundTrip[DatasetLabel]
    roundTrip[DatasetMd5]
    roundTrip[DatasetQaState]
    roundTrip[GsaRecord]
    roundTrip[Instant]
    roundTrip[QaResponse]
    roundTrip[EitherQaResponse]
  }

}
