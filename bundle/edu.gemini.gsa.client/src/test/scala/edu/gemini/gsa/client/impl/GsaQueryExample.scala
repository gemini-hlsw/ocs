package edu.gemini.gsa.client.impl

import edu.gemini.gsa.client.api.GsaResult._
import edu.gemini.model.p1.immutable._
import edu.gemini.model.p1.immutable.Band.BAND_1_2
import edu.gemini.model.p1.immutable.CoordinatesEpoch.J_2000
import edu.gemini.gsa.client.api.{GsaSiderealParams, GsaNonSiderealParams, GsaParams}
import java.util.UUID

object GsaQueryExample extends App {
  private def query(p: GsaParams) {
    GsaClientImpl.query(p) match {
      case Success(url, datasets) =>
        println("URL = " + url)
        datasets.foreach(println(_))
      case f: Failure =>
        println("failed: " + f)
    }
  }

  override def main(args: Array[String]) {
    // Query from an observation.  GsaParams extracts the information it needs
    // from the observation.
    println("NGC1407")
    val coords = HmsDms(HMS("3:40:09.42"), DMS("-18:33:37.3"))
    val target = SiderealTarget(UUID.randomUUID(), "NGC 1407", coords, J_2000, None, Nil)
    val blue   = GmosSBlueprintImaging(Nil)
    val obs    = Observation(Some(blue), None, Some(target), BAND_1_2, None)
    query(GsaParams.get(obs).get)

    // A more straightforward sidereal query.  There are no results that match
    // this query.
    println("\nNo results")
    query(GsaSiderealParams(HmsDms(HMS("4:23:57.8"), DMS("-20:23:45.7")), Instrument.GmosSouth))

    // A non-sidereal target search.  Searches by name.
    println("\nA non-sidereal target")
    query(GsaNonSiderealParams("Jupiter", Instrument.Niri))
  }
}