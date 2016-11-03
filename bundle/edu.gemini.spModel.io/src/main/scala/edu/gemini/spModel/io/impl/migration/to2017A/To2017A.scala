package edu.gemini.spModel.io.impl.migration.to2017A

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.io.impl.migration.PioSyntax._

import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.pio.{ParamSet, Container, Pio, Document, Version}

object To2017A extends Migration {

  val version = Version.`match`("2017A-1")

  val conversions: List[Document => Unit] = List(updateExecutedGnirs)

  val fact = new PioXmlFactory

  // REL-2646: Updates executed GNIRS observations with a flag that tells the
  // sequence generation code to use the old, incorrect, observing wavelength
  // calculation that existed before 2017A.
  private def updateExecutedGnirs(d: Document): Unit =
    for {
      o <- findObservations(d)(c => hasGnirs(c) && isExecuted(c))
      d <- gnirs(o)
    } Pio.addBooleanParam(fact, d, InstGNIRS.OVERRIDE_ACQ_OBS_WAVELENGTH_PROP.getName, false)

  private def gnirs(obs: Container): Option[ParamSet] =
    for {
      g <- obs.findContainers(SPComponentType.INSTRUMENT_GNIRS).headOption
      d <- g.dataObject
    } yield d

  private def hasGnirs(obs: Container): Boolean =
    gnirs(obs).isDefined

}
