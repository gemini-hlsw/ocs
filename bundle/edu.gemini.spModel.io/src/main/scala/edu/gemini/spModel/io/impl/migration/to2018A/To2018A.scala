package edu.gemini.spModel.io.impl.migration.to2018A

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.pio.{Container, Document, ParamSet, Pio, Version}
import edu.gemini.spModel.pio.xml.{PioXmlFactory, PioXmlUtil}
import edu.gemini.spModel.target.SPTargetPio
import edu.gemini.spModel.target.TargetParamCodecs._
import edu.gemini.spModel.target.TargetParamSetCodecs._
import edu.gemini.spModel.target.env.{Asterism, UserTarget}

import scalaz._
import Scalaz._

object To2018A extends Migration {

  val version = Version.`match`("2018A-1")

  val conversions: List[Document => Unit] =
    List(
      targetToAsterism,
      typeUserTargets
    )

  val fact = new PioXmlFactory

  // Convert base positions to single-target asterisms
  private def targetToAsterism(d: Document): Unit = {

    import edu.gemini.spModel.pio.codec.CodecSyntax._

    // SPTarget paramset to Asterism paramSet
    def convertToAsterism(ps: ParamSet): ParamSet =
      Asterism.single(SPTargetPio.fromParamSet(ps)).encode("asterism")

    envAndBases(d).foreach { case (obs, base) =>
      obs.removeChild(base)
      obs.addParamSet(convertToAsterism(base))
    }

  }

  // Convert user targets to give them a type.
  private def typeUserTargets(d: Document): Unit = {

    import edu.gemini.spModel.io.PioSyntax._

    val ps = for {
      e <- targetEnvs(d)
      p <- e.allParamSets.filter(_.getName == "userTargets")
    } {
      p.paramSets("spTarget").foreach { t =>
        p.removeChild(t)

        val ut = fact.createParamSet("userTarget")
        Pio.addParam(fact, ut, "type", UserTarget.Type.other.name());
        ut.addParamSet(t)
        p.addParamSet(ut)
      }
    }
  }
}
