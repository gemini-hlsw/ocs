package edu.gemini.spModel.target.env

import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AlmostEqual.AlmostEqualOps
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.gemini.ghost.GhostParamSetCodecs._
import edu.gemini.spModel.pio.codec._

import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._


class AsterismParamSetCodecSpec extends Specification with ScalaCheck with Arbitraries with Almosts {

  def close[A: ParamSetCodec: Arbitrary: AlmostEqual](implicit mf: Manifest[A]) =
    mf.runtimeClass.getName ! forAll { (key: String, value: A) =>
      val c = ParamSetCodec[A]
      c.decode(c.encode(key, value)).map(_ ~= value) must_== \/-(true)
    }

  "Asterism ParamSet Codecs" >> {
    close[Asterism.Single]
    close[GhostAsterism.StandardResolution]
    close[GhostAsterism.HighResolution]
    close[GhostAsterism]
    close[Asterism]
  }
}