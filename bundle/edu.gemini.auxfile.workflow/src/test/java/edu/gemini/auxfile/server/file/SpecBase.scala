package edu.gemini.auxfile.server.file

import java.nio.file.Files
import java.time.{ZoneOffset, LocalDateTime, Instant}

import edu.gemini.auxfile.server.file.MetaDataSpec.TestEnv
import edu.gemini.spModel.core.SPProgramID
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import org.scalacheck.{Prop, Gen, Arbitrary}
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult

import scala.collection.JavaConverters._

trait SpecBase extends ScalaCheck {
  private val Min: Instant =
    LocalDateTime.of(2000, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)

  private val Max: Instant =
    LocalDateTime.of(2100, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)

  implicit val arbInstant: Arbitrary[Instant] =
    Arbitrary {
      Gen.choose(Min.toEpochMilli, Max.toEpochMilli).map(Instant.ofEpochMilli)
    }

  implicit val arbProgramId: Arbitrary[SPProgramID] =
    Arbitrary {
      Gen.choose(100, 399).map(n => SPProgramID.toProgramID(s"GS-2019A-Q-$n"))
    }

  def genOdfName(pid: SPProgramID): Gen[String] =
    Gen.choose(1, 999).map(n => f"${pid.stringValue}-$n%03d.odf")

  val genFinderName: Gen[String] =
    Gen.nonEmptyListOf(Gen.alphaChar).map(l => s"${l.mkString}.pdf")

  case class TestEnv(
    pid:         SPProgramID,
    fileName:    String,
    description: String,
    checked:     Boolean,
    lastEmailed: Option[Instant]
  )

  implicit val arbTestEnv: Arbitrary[TestEnv] =
    Arbitrary {
      for {
        pid <- arbitrary[SPProgramID]
        nam <- Gen.oneOf(genOdfName(pid), genFinderName)
        des <- Gen.alphaStr
        chk <- arbitrary[Boolean]
        lem <- arbitrary[Option[Instant]]
      } yield TestEnv(pid, nam, des, chk, lem)
    }


  def forAllMetaData(f: TestEnv => MatchResult[Any]): Prop =
    forAll { (env: TestEnv) =>

      var dir = Files.createTempDirectory(s"MetaDataSpec")

      try {
        FileManager.init(dir.toFile)
        f(env)
      } finally {
        Files.walk(dir).iterator.asScala.map(_.toFile).foreach(_.delete)
      }
    }

}
