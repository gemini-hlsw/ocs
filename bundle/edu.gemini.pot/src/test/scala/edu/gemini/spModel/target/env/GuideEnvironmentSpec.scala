package edu.gemini.spModel.target.env

import edu.gemini.spModel.core.AlmostEqual.AlmostEqualOps

import edu.gemini.shared.util.immutable.{ImList, ImOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.guide.{GuideProbeMap, GuideProbe}
import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.target.SPTarget
import org.apache.commons.io.output.ByteArrayOutputStream

import org.scalacheck.Prop._

import org.specs2.ScalaCheck

import org.specs2.mutable.Specification

import java.io.{ByteArrayInputStream, ObjectInputStream, ObjectOutputStream}

import scala.collection.JavaConverters._
import scalaz._, Scalaz._


class GuideEnvironmentSpec extends Specification with ScalaCheck with Arbitraries with Almosts {

  "GuideEnvironment" should {
    "be PIO Externalizable" in
      forAll { (g: GuideEnvironment) =>
        g ~= GuideEnvironment.fromParamSet(g.getParamSet(new PioXmlFactory()))
      }


    "be Serializable" in
      forAll { (g: GuideEnvironment) =>
        val bao = new ByteArrayOutputStream()
        val oos = new ObjectOutputStream(bao)
        oos.writeObject(g)
        oos.close()

        val ois = new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray))
        ois.readObject() match {
          case g2: GuideEnvironment => g ~= g2
          case _                    => false
        }
      }
  }


}
