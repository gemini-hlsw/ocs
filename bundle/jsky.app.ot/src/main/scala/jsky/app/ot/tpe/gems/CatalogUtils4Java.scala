package jsky.app.ot.tpe.gems

import edu.gemini.spModel.core.{MagnitudeBand, SiderealTarget}
import edu.gemini.shared.util.immutable.ImList
import jsky.coords.{DMS, HMS}
import scala.collection.JavaConverters._

/**
 * Utility functions for tpe that reach into the scala world from Java
 */
object CatalogUtils4Java {
  /**
   * Build a row of data items from a Sidereal Target from UCAC4 (Containing r' and UC)
   */
  def makeUCAC4Row(
      siderealTarget: SiderealTarget,
      unusedBands:    ImList[MagnitudeBand]
  ): java.util.List[AnyRef] =
    makeRow(siderealTarget, List(MagnitudeBand._r, MagnitudeBand.UC), unusedBands)

  /**
   * Build a row of data items from a Sidereal Target
   */
  def makeRow(
    siderealTarget: SiderealTarget,
    unusedBands:    ImList[MagnitudeBand]
  ): java.util.List[AnyRef] =
    makeRow(siderealTarget, List(MagnitudeBand.R), unusedBands)

  private def makeRow(
    siderealTarget: SiderealTarget,
    rBands:         List[MagnitudeBand],
    unusedBands:    ImList[MagnitudeBand]
  ): java.util.List[AnyRef] = {

    def bandToValue(b: MagnitudeBand): java.lang.Double =
      siderealTarget.magnitudeIn(b).map(v => Double.box(v.value)).orNull

    val a = List[AnyRef](Boolean.box(true), siderealTarget.name)
    val b = rBands.map(bandToValue)
    val c = List(
              siderealTarget.coordinates.ra.toAngle.formatHMS,
              siderealTarget.coordinates.dec.formatDMS
            )
    val d = unusedBands.toList.asScala.toList.map(bandToValue)

    (a ++ b ++ c ++ d).asJava
  }

}
