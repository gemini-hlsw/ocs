package edu.gemini.spModel.target.env

import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target._
import edu.gemini.spModel.pio.codec._

import scalaz._
import Scalaz._
import org.scalacheck.{Arbitrary, Gen, Properties}
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import AlmostEqual.AlmostEqualOps
import edu.gemini.spModel.target.SPTarget

import scalaz._
import Scalaz._

class AsterismParamSetCodecSpec extends Specification with ScalaCheck with Arbitraries {

  implicit val SPTargetAlmostEqual: AlmostEqual[SPTarget] =
    AlmostEqual[Target].contramap(_.getTarget)

  implicit val SingleAsterismAlmostEqual: AlmostEqual[Asterism.Single] =
    AlmostEqual[SPTarget].contramap[Asterism.Single](_.t)

  implicit val AsterismAlmostEqual: AlmostEqual[Asterism] =
    new AlmostEqual[Asterism] {
      def almostEqual(a: Asterism, b: Asterism): Boolean =
        (a, b) match {
          case (a: Asterism.Single, b: Asterism.Single) => SingleAsterismAlmostEqual.almostEqual(a, b)
          // TODO:ASTERISM: ghost
          case _ => false
        }
    }

  def close[A: ParamSetCodec: Arbitrary: AlmostEqual](implicit mf: Manifest[A]) =
    mf.runtimeClass.getName ! forAll { (key: String, value: A) =>
      val c = ParamSetCodec[A]
      c.decode(c.encode(key, value)).map(_ ~= value) must_== \/-(true)
    }

  "Asterism ParamSet Codecs" >> {
    close[Asterism.Single]
    close[Asterism]
  }

}



