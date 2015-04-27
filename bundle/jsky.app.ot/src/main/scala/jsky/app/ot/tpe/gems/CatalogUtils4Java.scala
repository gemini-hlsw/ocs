package jsky.app.ot.tpe.gems

import edu.gemini.spModel.core.{MagnitudeBand, Target}
import jsky.coords.{DMS, HMS}
import scala.collection.JavaConverters._

/**
 * Utility functions for tpe that reach into the scala world from Java
 */
object CatalogUtils4Java {
  /**
   * Build a row of data items from a Sidereal Target
   */
  def makeRow(siderealTarget: Target.SiderealTarget, nirBand: String, unusedBands: Array[String]): java.util.Vector[AnyRef] = {
    new java.util.Vector[AnyRef](Vector[AnyRef](
      Boolean.box(x = true),
      siderealTarget.name,
      siderealTarget.magnitudeIn(MagnitudeBand._r).map(v => Double.box(v.value)).orNull,
      siderealTarget.magnitudeIn(MagnitudeBand.R).map(v => Double.box(v.value)).orNull,
      siderealTarget.magnitudeIn(MagnitudeBand.UC).map(v => Double.box(v.value)).orNull,
      MagnitudeBand.all.find(_.name == nirBand).flatMap(siderealTarget.magnitudeIn).map(v => Double.box(v.value)).orNull,
      new HMS(siderealTarget.coordinates.ra.toAngle.formatHMS).toString,
      new DMS(siderealTarget.coordinates.dec.formatDMS).toString,
      MagnitudeBand.all.find(_.name == unusedBands(0)).flatMap(siderealTarget.magnitudeIn).map(v => Double.box(v.value)).orNull,
      MagnitudeBand.all.find(_.name == unusedBands(1)).flatMap(siderealTarget.magnitudeIn).map(v => Double.box(v.value)).orNull).asJava)
  }
}
