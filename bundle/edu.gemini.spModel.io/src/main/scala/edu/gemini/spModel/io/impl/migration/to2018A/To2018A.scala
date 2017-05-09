package edu.gemini.spModel.io.impl.migration.to2018A

import edu.gemini.spModel.io.PioSyntax._
import edu.gemini.spModel.io.impl.SpIOTags
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.pio.xml.PioXmlFactory

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
