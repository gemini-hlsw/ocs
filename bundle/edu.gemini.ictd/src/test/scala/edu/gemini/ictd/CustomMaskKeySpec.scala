package edu.gemini.ictd

import edu.gemini.spModel.core.{ProgramType, Site, ProgramId, ProgramIdGen}

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._


object CustomMaskKeySpec extends Specification with ScalaCheck {

  implicit val arbSciencePid: Arbitrary[ProgramId.Science] =
    Arbitrary {
      ProgramIdGen.genScienceId.map { pid =>
        ProgramId.parse(pid.stringValue) match {
          case s: ProgramId.Science => s
          case _                    => sys.error("should only generate science pids")
        }
      }
    }

  implicit val arbCustomMaskKey: Arbitrary[CustomMaskKey] =
    Arbitrary {
      for {
        pid <- arbitrary[ProgramId.Science]
        run <- Gen.posNum[Int]
      } yield CustomMaskKey.unsafeFromIdAndIndex(pid, run)
    }

  "CustomMaskKey" should {

    "roundtrip" in {
      forAll { (key: CustomMaskKey) =>
        CustomMaskKey.parse(key.format) shouldEqual Some(key)
      }
    }

    "disallow negative running numbers" in {
      forAll { (pid: ProgramId.Science, index: Int) =>
        CustomMaskKey.fromIdAndIndex(pid, index) match {
          case Some(key) => index should be_>=(0)
          case None      => index should be_<(0)
        }
      }
    }

  }
}
