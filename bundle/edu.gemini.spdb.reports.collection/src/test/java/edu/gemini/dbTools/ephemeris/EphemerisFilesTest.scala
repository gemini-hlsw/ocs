package edu.gemini.dbTools.ephemeris

import edu.gemini.spModel.core.AlmostEqual._

import edu.gemini.spModel.core.HorizonsDesignation
import edu.gemini.spModel.core.HorizonsDesignation.MajorBody

import java.nio.file.{Files, Path}
import java.util.logging.{Level, Logger}

import org.scalacheck.Prop
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scala.collection.JavaConverters._

import scalaz._, Scalaz._

object EphemerisFilesTest extends Specification with ScalaCheck with Arbitraries {
  val log = Logger.getLogger(EphemerisFilesTest.getClass.getName)

  def delete(f: Path): Unit = {
    if (Files.isDirectory(f)) {
      Files.list(f).iterator.asScala.foreach(delete)
    }
    Files.delete(f)
  }

  def execTest[T,A](makeAction: EphemerisFiles => TryExport[T])(processResult: (ExportError \/ T) => A): A = {
    val dir = Files.createTempDirectory("EphemerisFilesTest-")
    val ef  = EphemerisFiles(dir)

    try {
      processResult(makeAction(ef).run.unsafePerformIO())
    } finally {
      delete(dir)
    }
  }

  def testLeft[T](makeAction: EphemerisFiles => TryExport[T]): ExportError =
    execTest(makeAction) {
      case -\/(err) => err
      case \/-(t)   => throw new RuntimeException("Expected error")
    }

  def testRight[T](makeAction: EphemerisFiles => TryExport[T]): T =
    execTest(makeAction) {
      case -\/(err) =>
        val (msg, ex) = err.report
        log.log(Level.WARNING, msg, ex.orNull)
        err.log(log)
        throw ex.getOrElse(new RuntimeException())

      case \/-(t) =>
        t
    }

  "EphemerisFiles.list" should {
    "include every ephemeris file with a parseable horizons id" in {
      Prop.forAll { (ems: List[EphemerisMap]) =>
        val tups     = ems.zipWithIndex.map { case (em, i) => (MajorBody(i): HorizonsDesignation, em) }
        val expected = ISet.fromList(tups.unzip._1)

        val actual   = testRight { ef =>
          for {
            _ <- tups.traverseU { case (hid, em) => ef.write(hid, em) }
            l <- ef.list
          } yield l
        }

        expected == actual
      }
    }

    "skip ephemeris files without parseable horizons ids" in {
      Prop.forAll { (em: EphemerisMap, ems: List[EphemerisMap]) =>
        val tups     = (em :: ems).zipWithIndex.map { case (em0, i) => (MajorBody(i): HorizonsDesignation, em0) }
        val expected = ISet.fromList(tups.unzip._1.tail) // we will rename MajorBody(0) to xxx.eph

        val actual   = testRight { ef =>
          val rename0: TryExport[Unit] =
            TryExport.fromTryCatch(ex => ExportError.FileError("couldn't rename MajorBody(0)", Some(MajorBody(0)), Some(ex))) {
              val p0 = ef.path(MajorBody(0))
              val p1 = p0.getParent.resolve("xxx.eph")
              Files.move(p0, p1)
            }

          for {
            _ <- tups.traverseU { case (hid, em) => ef.write(hid, em) }
            _ <- rename0
            l <- ef.list
          } yield l
        }

        expected == actual
      }
    }
  }

  "EphemerisFiles.delete" should {
    "return false if the ephemeris file doesn't exist" in {
      !testRight { _.delete(MajorBody(0)) }
    }

    "delete the file and return true if it does exist" in {
      val hid = MajorBody(0)
      val em  = arbEphemerisMap.arbitrary.sample.get

      val (l0, b, l1) = testRight { ef =>
        for {
          _  <- ef.write(hid, em)
          l0 <- ef.list
          b  <- ef.delete(hid)
          l1 <- ef.list
        } yield (l0, b, l1)
      }

      (ISet.singleton(hid) == l0) && b && (ISet.empty == l1)
    }
  }

  "EphemerisFiles.deleteAll" should {
    "return false if none of the ephemeris files exist" in {
      val hids = ISet.fromList(List[HorizonsDesignation](MajorBody(0), MajorBody(1)))
      !testRight { _.deleteAll(hids) }
    }

    "delete any named files that exist and return true if anything was deleted" in {
      val hid  = MajorBody(0)
      val hids = ISet.fromList(List[HorizonsDesignation](hid, MajorBody(1)))
      val em   = arbEphemerisMap.arbitrary.sample.get

      val (l0, b, l1) = testRight { ef =>
        for {
          _  <- ef.write(hid, em)
          l0 <- ef.list
          b  <- ef.deleteAll(hids)
          l1 <- ef.list
        } yield (l0, b, l1)
      }

      (ISet.singleton(hid) == l0) && b && (ISet.empty == l1)
    }
  }

  def testMissingFile[T](method: (EphemerisFiles, HorizonsDesignation) => TryExport[T]): Boolean = {
    val hid = MajorBody(0)

    testLeft { ef => method(ef, hid) } match {
      case ExportError.FileError(_, Some(`hid`), _) => true
      case _                                        => false
    }
  }

  "EphemerisFile.read" should {
    "generate an error if the file doesn't exist" in {
      testMissingFile(_.read(_))
    }

    "produce the file contents if the file exists" in {
      Prop.forAll { (em: EphemerisMap) =>
        val hid = MajorBody(0)

        val content = testRight { ef =>
          for {
            _ <- ef.write(hid, em)
            s <- ef.read(hid)
          } yield s
        }

        content.trim == EphemerisFileFormat.format(em).trim
      }
    }
  }

  def testCannotParseFile[T](method: (EphemerisFiles, HorizonsDesignation) => TryExport[T]): Boolean = {
    val hid = MajorBody(0)

    testLeft { ef =>
      def writeJunk(p: Path): TryExport[Unit] =
        TryExport.fromTryCatch(ex => ExportError.FileError("Couldn't write MajorBody(0)", Some(MajorBody(0)), Some(ex))) {
          val lines = List("make", "gemini", "great", "again").asJava
          Files.write(ef.path(hid), lines)
        }

      for {
        _ <- writeJunk(ef.path(hid))
        _ <- method(ef, hid)
      } yield ()
    } match {
      case ExportError.FileError(msg, Some(`hid`), None) =>
        msg.startsWith("Could not parse")
      case _                                             =>
        true
    }
  }

  "EphemerisFile.parse" should {
    "generate an error if the file doesn't exist" in {
      testMissingFile(_.parse(_))
    }

    "generate an error if the file isn't parseable" in {
      testCannotParseFile(_.parse(_))
    }

    "parse the content if valid" in {
      Prop.forAll { (em1: EphemerisMap) =>
        val hid = MajorBody(0)

        val em2 = testRight { ef =>
          for {
            _  <- ef.write(hid, em1)
            em <- ef.parse(hid)
          } yield em
        }

        em1 ~= em2
      }
    }
  }

  "EphemerisFile.parseTimes" should {
    "generate an error if the file doesn't exist" in {
      testMissingFile(_.parseTimes(_))
    }

    "generate an error if the file isn't parseable" in {
      testCannotParseFile(_.parseTimes(_))
    }

    "parse the content if valid" in {
      Prop.forAll { (em: EphemerisMap) =>
        val hid = MajorBody(0)

        val times = testRight { ef =>
          for {
            _  <- ef.write(hid, em)
            ts <- ef.parseTimes(hid)
          } yield ts
        }

        times == em.keySet
      }
    }
  }
}
