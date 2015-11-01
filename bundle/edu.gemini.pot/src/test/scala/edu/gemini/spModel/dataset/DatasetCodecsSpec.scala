package edu.gemini.spModel.dataset

import edu.gemini.spModel.dataset.DatasetCodecs._
import edu.gemini.spModel.pio.codec.{ParamSetCodec, ParamCodec}

import org.scalacheck.Prop.forAll

import org.scalacheck.Arbitrary
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import java.time.Instant
import java.util.UUID

import scalaz.\/-

object DatasetCodecsSpec extends Specification with ScalaCheck with Arbitraries {

  implicit val CodecInstant = DatasetCodecs.ParamCodecInstant
  implicit val CodecUuid    = DatasetCodecs.ParamCodecUuid

  def param[A: ParamCodec: Arbitrary](implicit mf: Manifest[A]) =
    mf.runtimeClass.getName ! forAll { (key: String, value: A) =>
      val c = ParamCodec[A]
      c.decode(c.encode(key, value)) must_== \/-(value)
    }

  "Dataset Param Codecs" >> {
    param[DatasetMd5]
    param[DatasetQaState]
    param[Instant]
    param[QaRequestStatus]
    param[UUID]
  }

  def paramSet[A: ParamSetCodec: Arbitrary](implicit mf: Manifest[A]) =
    mf.runtimeClass.getName ! forAll { (key: String, value: A) =>
      val c = ParamSetCodec[A]
      c.decode(c.encode(key, value)) must_== \/-(value)
    }

  "Dataset ParamSet Codecs" >> {
    paramSet[Dataset]
    paramSet[DatasetExecRecord]
    paramSet[DatasetGsaState]
    paramSet[SummitState]
  }
}
