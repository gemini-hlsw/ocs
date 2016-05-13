package edu.gemini.dbTools.ephemeris

import edu.gemini.spModel.core.AlmostEqual._
import org.scalacheck.Prop
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz._

object EphemerisFileFormatTest extends Specification with ScalaCheck with Arbitraries {

  "EphemerisFileFormat" should {
    "support round-trip" in {
      Prop.forAll { (em0: EphemerisMap) =>
        val s   = EphemerisFileFormat.format(em0)
        val em1 = EphemerisFileFormat.parse(s).getOrElse(==>>.empty)
        em0 ~= em1
      }
    }

    "permit parsing timestamps" in {
      Prop.forAll { (em0: EphemerisMap) =>
        val s  = EphemerisFileFormat.format(em0)
        val ts = EphemerisFileFormat.parseTimestamps(s).getOrElse(ISet.empty)
        em0.keySet == ts
      }
    }
  }
}
