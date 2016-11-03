package edu.gemini.spModel.io.impl.migration.to2017A

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core.{Coordinates, Ephemeris}
import edu.gemini.spModel.gemini.gnirs.InstGNIRS
import edu.gemini.spModel.io.impl.migration.Migration
import edu.gemini.spModel.io.impl.migration.PioSyntax._
import edu.gemini.spModel.pio.codec.ParamSetCodec
import edu.gemini.spModel.target.TargetParamCodecs._
import edu.gemini.spModel.target.TargetParamSetCodecs._

import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.pio.{ParamSet, Container, Pio, Document, Version}

import scalaz._, Scalaz._


object To2017A extends Migration {

  val version = Version.`match`("2017A-1")

  val conversions: List[Document => Unit] =
    List(updateExecutedGnirs, updateNonSiderealTargets)

  val fact = new PioXmlFactory

  // REL-2646: Updates executed GNIRS observations with a flag that tells the
  // sequence generation code to use the old, incorrect, observing wavelength
  // calculation that existed before 2017A.
  private def updateExecutedGnirs(d: Document): Unit = {
    def gnirs(obs: Container): Option[ParamSet] =
      for {
        g <- obs.findContainers(SPComponentType.INSTRUMENT_GNIRS).headOption
        d <- g.dataObject
      } yield d

    def hasGnirs(obs: Container): Boolean =
      gnirs(obs).isDefined

    for {
      o <- findObservations(d)(c => hasGnirs(c) && isExecuted(c))
      d <- gnirs(o)
    } Pio.addBooleanParam(fact, d, InstGNIRS.OVERRIDE_ACQ_OBS_WAVELENGTH_PROP.getName, false)
  }

  // REL-2971: read pre-2017A non-sidereal ephemeris data and replace it with
  // compressed ephemeris data.
  private def updateNonSiderealTargets(d: Document): Unit = {
    implicit val ephemerisElementParamSetCodec: ParamSetCodec[(Long, Coordinates)] =
      ParamSetCodec.initial((0L, Coordinates.zero))
        .withParam("time", Lens.firstLens[Long, Coordinates])
        .withParamSet("coordinates", Lens.secondLens[Long, Coordinates])

    val ephemerisElements: Ephemeris @> List[(Long, Coordinates)] =
      Ephemeris.data.xmapB(_.toList)(==>>.fromList(_))

    val OldEphemerisParamSetCodec: ParamSetCodec[Ephemeris] =
      ParamSetCodec.initial(Ephemeris.empty)
        .withParam("site", Ephemeris.site)
        .withManyParamSet("ephemeris-element", ephemerisElements)

    def convertEphemeris(ps: ParamSet): ParamSet =
      OldEphemerisParamSetCodec.decode(ps) match {
        case -\/(er) => throw new RuntimeException("Error reading pre-2017A ephemeris data: " + er)
        case \/-(ep) => EphemerisParamSetCodec.encode("ephemeris", ep)
      }

    for {
      t0 <- allTargets(d)
      t1 <- t0.paramSet("target").toList
      es <- t1.paramSet("ephemeris").toList if Option(es.getParam("data")).isEmpty
    } {
      t1.removeChild(es)
      t1.addParamSet(convertEphemeris(es))
    }
  }
}
