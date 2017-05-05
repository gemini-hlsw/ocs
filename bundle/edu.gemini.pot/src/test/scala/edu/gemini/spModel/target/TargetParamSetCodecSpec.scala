package edu.gemini.spModel.target

import edu.gemini.spModel.core.Arbitraries
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target._
import edu.gemini.spModel.pio.codec._

import scalaz._
import Scalaz._

import org.scalacheck.{ Properties, Gen, Arbitrary }
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import AlmostEqual.AlmostEqualOps

object TargetParamSetCodecsSpec extends Specification with ScalaCheck with Arbitraries {
  import TargetParamSetCodecs._

  def close[A: ParamSetCodec: Arbitrary: AlmostEqual](implicit mf: Manifest[A]) =
    mf.runtimeClass.getName ! forAll { (key: String, value: A) =>
      val c = ParamSetCodec[A]
      c.decode(c.encode(key, value)).map(_ ~= value) must_== \/-(true)
    }

  def exact[A: ParamSetCodec: Arbitrary](implicit mf: Manifest[A]) = 
    mf.runtimeClass.getName ! forAll { (key: String, value: A) =>
      val c = ParamSetCodec[A]
      c.decode(c.encode(key, value)) must_== \/-(value)
    }

  "Target ParamSet Codecs" >> {
    close[Coordinates]
    close[ProperMotion]
    close[Magnitude]
    close[SpectralDistribution]
    close[SpatialProfile]
    close[SiderealTarget]
    exact[TooTarget]
    close[NonSiderealTarget]
    close[Target]
    close[Ephemeris]
  }

}



