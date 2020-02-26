package edu.gemini.spModel.dataset

import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

import java.time.Instant
import java.util.UUID

trait Arbitraries {
  implicit val arbitraryDataset: Arbitrary[Dataset] =
    Arbitrary {
      for {
        label <- arbitrary[DatasetLabel]
        file  <- arbitrary[String]
        time  <- posNum[Long]
      } yield new Dataset(label, file, time)
    }

  implicit val arbDatasetExecRecord: Arbitrary[DatasetExecRecord] =
    Arbitrary {
      for {
        dataset <- arbitrary[Dataset]
        summit  <- arbitrary[SummitState]
        archive <- arbitrary[Option[DatasetGsaState]]
      } yield DatasetExecRecord(dataset, summit, archive)
    }

  implicit val arbDatasetGsaState: Arbitrary[DatasetGsaState] =
    Arbitrary {
      for {
        qa        <- arbitrary[DatasetQaState]
        timestamp <- arbitrary[Instant]
        md5       <- arbitrary[DatasetMd5]
      } yield DatasetGsaState(qa, timestamp, md5)
    }

  implicit val arbDatasetLabel: Arbitrary[DatasetLabel] =
    Arbitrary {
      for {
        name <- listOfN(4, alphaUpperChar).map(_.mkString)
        obs  <- posNum[Int]
        ds   <- posNum[Int]
      } yield new DatasetLabel(s"$name-$obs-$ds")
    }

  implicit val arbDatasetMd5: Arbitrary[DatasetMd5] =
    Arbitrary {
      listOfN(16, arbitrary[Byte]).map(a => new DatasetMd5(a.toArray))
    }

  implicit val arbDatasetQaState: Arbitrary[DatasetQaState] =
    Arbitrary  { oneOf(DatasetQaState.values()) }

  implicit val arbInstant: Arbitrary[Instant] =
    Arbitrary { arbitrary[Long].map(Instant.ofEpochMilli) }

  implicit val arbFailedRequestStatus: Arbitrary[QaRequestStatus.Failed] =
    Arbitrary { arbitrary[String].map(QaRequestStatus.Failed) }

  implicit val arbSummitMissing: Arbitrary[SummitState.Missing] =
    Arbitrary { arbitrary[DatasetQaState].map(SummitState.Missing.apply) }

  implicit val arbSummitIdle: Arbitrary[SummitState.Idle] =
    Arbitrary { arbitrary[DatasetGsaState].map(SummitState.Idle.apply) }

  implicit val arbSummitActive: Arbitrary[SummitState.ActiveRequest] =
    Arbitrary {
      for {
        gsa    <- arbitrary[DatasetGsaState]
        req    <- arbitrary[DatasetQaState]
        id     <- arbitrary[UUID]
        status <- arbitrary[QaRequestStatus]
        when   <- arbitrary[Instant]
        retry  <- arbitrary[Int]
      } yield SummitState.ActiveRequest(gsa, req, id, status, when, retry)
    }

  implicit val arbSummitState: Arbitrary[SummitState] =
    Arbitrary {
      Gen.oneOf(arbitrary[SummitState.Missing], arbitrary[SummitState.Idle], arbitrary[SummitState.ActiveRequest])
    }

  implicit val arbQaRequestStatus: Arbitrary[QaRequestStatus] = {
    import QaRequestStatus._
    Arbitrary {
      Gen.oneOf[QaRequestStatus](PendingPost, ProcessingPost, arbitrary[Failed], Accepted)
    }
  }

}
