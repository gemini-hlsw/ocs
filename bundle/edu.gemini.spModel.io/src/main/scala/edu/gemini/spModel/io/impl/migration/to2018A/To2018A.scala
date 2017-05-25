package edu.gemini.spModel.io.impl.migration.to2018A

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.{Coordinates, Ephemeris}
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import edu.gemini.spModel.io.PioSyntax
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.pio.{Container, Document, ParamSet, Pio, Version}
import edu.gemini.spModel.pio.codec.ParamSetCodec
import edu.gemini.spModel.pio.codec.CodecSyntax._
import edu.gemini.spModel.target.TargetParamCodecs._
import edu.gemini.spModel.target.TargetParamSetCodecs._
import edu.gemini.spModel.pio.xml.{PioXmlFactory, PioXmlUtil}
import edu.gemini.spModel.pio.{Container, Document, ParamSet, Pio, Version}
import edu.gemini.spModel.target.SPTargetPio
import edu.gemini.spModel.target.env.Asterism

import scalaz._
import Scalaz._

object To2018A extends Migration {

  val version = Version.`match`("2018A-1")

  val conversions: List[Document => Unit] =
    List(targetToAsterism)

  val fact = new PioXmlFactory

  // Convert base positions to single-target asterisms
  private def targetToAsterism(d: Document): Unit = {

    // SPTarget paramset to Asterism paramSet
    def convertToAsterism(ps: ParamSet): ParamSet =
      Asterism.single(SPTargetPio.fromParamSet(ps)).encode("asterism")

    envAndBases(d).foreach { case (obs, base) =>
      obs.removeChild(base)
      obs.addParamSet(convertToAsterism(base))
    }

  }

}
