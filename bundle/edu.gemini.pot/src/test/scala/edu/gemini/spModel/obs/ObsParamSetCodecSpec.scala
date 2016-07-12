package edu.gemini.spModel.obs

import edu.gemini.spModel.obs.SchedulingBlock.Duration
import edu.gemini.spModel.pio.codec._

import scalaz._
import Scalaz._

import org.scalacheck.{ Properties, Gen, Arbitrary }
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

object ObsParamSetCodecsSpec extends Specification with ScalaCheck with Arbitraries {
  import ObsParamSetCodecs._

  def exact[A: ParamSetCodec: Arbitrary](implicit mf: Manifest[A]) =
    mf.runtimeClass.getName ! forAll { (key: String, value: A) =>
      val c = ParamSetCodec[A]
      c.decode(c.encode(key, value)) must_== \/-(value)
    }

  "Obs ParamSet Codecs" >> {
    exact[Duration]
    exact[SchedulingBlock]
  }

}



