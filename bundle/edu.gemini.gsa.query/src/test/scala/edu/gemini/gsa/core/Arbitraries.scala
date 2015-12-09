package edu.gemini.gsa.core

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId}

import edu.gemini.spModel.core.{ProgramType, SPProgramID, Site}
import edu.gemini.spModel.dataset.{DatasetGsaState, DatasetLabel, DatasetQaState}
import org.scalacheck.Arbitrary._
import org.scalacheck._

import scalaz.Scalaz._
import scalaz._

trait Arbitraries extends edu.gemini.spModel.dataset.Arbitraries {
  implicit val arbGsaRecord: Arbitrary[GsaRecord] =
    Arbitrary {
      for {
        label <- Gen.frequency((95, arbDatasetLabel.arbitrary.map(some)), (5, none[DatasetLabel]))
        file  <- arbitrary[String]
        state <- arbitrary[DatasetGsaState]
      } yield GsaRecord(label, file, state)
    }

  val scienceId: Gen[SPProgramID] =
    for {
      site <- Gen.oneOf(Site.GN, Site.GS)
      year <- Gen.chooseNum[Int](2000, 3000)
      sem  <- Gen.oneOf("A", "B")
      tipe <- Gen.oneOf(ProgramType.All.filter(_.isScience))
      num  <- Gen.posNum[Int]
    } yield SPProgramID.toProgramID(s"${site.abbreviation}-$year$sem-${tipe.abbreviation}-$num")

  val dateFormatter = DateTimeFormatter.BASIC_ISO_DATE.withZone(ZoneId.of("Z"))

  val dailyId: Gen[SPProgramID] =
    for {
      site <- Gen.oneOf(Site.GN, Site.GS)
      tipe <- Gen.oneOf(ProgramType.Calibration, ProgramType.Engineering)
      year <- Gen.chooseNum[Int](2000, 3000)
      day  <- Gen.chooseNum[Int](1, 365)
      num  <- Gen.posNum[Int]
    } yield SPProgramID.toProgramID(s"${site.abbreviation}-${tipe.abbreviation}${dateFormatter.format(LocalDate.ofYearDay(year, day))}-$num")

  implicit val arbPid: Arbitrary[SPProgramID] =
    Arbitrary { Gen.oneOf(scienceId, dailyId) }

  implicit val arbQaRequest: Arbitrary[QaRequest] =
    Arbitrary {
      for {
        label <- arbitrary[DatasetLabel]
        qa    <- arbitrary[DatasetQaState]
      } yield QaRequest(label, qa)
    }

  implicit val arbQaResponse: Arbitrary[QaResponse] =
    Arbitrary {
      for {
        label <- arbitrary[DatasetLabel]
        fail  <- arbitrary[Option[String]]
      } yield QaResponse(label, fail)
    }
}
