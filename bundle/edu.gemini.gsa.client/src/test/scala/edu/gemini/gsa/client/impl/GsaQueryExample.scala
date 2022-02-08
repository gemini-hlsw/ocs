package edu.gemini.gsa.client.impl

import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.immutable.Band.BAND_1_2
import edu.gemini.model.p1.immutable.CoordinatesEpoch.J_2000
import edu.gemini.gsa.client.api.{GSAInstrument, GsaSiderealParams, GsaNonSiderealParams, GsaParams}
import java.util.UUID

import edu.gemini.spModel.core.{Declination, Angle, RightAscension, Coordinates}

object GsaQueryExample extends App {
  private def query(p: GsaParams): Unit = {
    println(GsaClientImpl.query(p))
  }

  override def main(args: Array[String]): Unit = {
    // Query from an observation.  GsaParams extracts the information it needs
    // from the observation.
    println("NGC1407")
    val coords = Coordinates(RightAscension.fromAngle(Angle.parseHMS("3:40:09.42").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-18:33:37.3").getOrElse(Angle.zero)).getOrElse(Declination.zero))
    val target = SiderealTarget(UUID.randomUUID(), "NGC 1407", coords, J_2000, None, Nil)
    val blue   = GmosSBlueprintImaging(Nil)
    val obs    = Observation(Some(blue), None, Some(target), BAND_1_2, None)
    query(GsaParams.get(obs).get)

    // A more straightforward sidereal query.  There are no results that match
    // this query.
    println("\nNo results")
    query(GsaSiderealParams(Coordinates(RightAscension.fromAngle(Angle.fromDegrees(178.66)), Declination.fromAngle(Angle.fromDegrees(-13.97)).getOrElse(Declination.zero)), GSAInstrument(Instrument.GmosSouth).get))
    //query(GsaSiderealParams(Coordinates(RightAscension.fromAngle(Angle.parseHMS("4:23:57.8").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-20:23:45.7").getOrElse(Angle.zero)).getOrElse(Declination.zero)), Instrument.GmosSouth))

    // A non-sidereal target search.  Searches by name.
    println("\nA non-sidereal target")
    query(GsaNonSiderealParams("1971 UC1", GSAInstrument(Instrument.Niri).get))
  }
}
