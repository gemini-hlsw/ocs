package jsky.app.ot.tpe.gems

import edu.gemini.spModel.core.{MagnitudeBand, Target}
import jsky.coords.{DMS, HMS}
import scala.collection.JavaConverters._

object CatalogUtils4Java {
  def makeRow(siderealTarget: Target.SiderealTarget, nirBand: String, unusedBands: Array[String]): java.util.Vector[AnyRef] = {
    new java.util.Vector[AnyRef](Vector[AnyRef](
      Boolean.box(true),
      siderealTarget.name,
      siderealTarget.magnitudeIn(MagnitudeBand.R).orNull,
      MagnitudeBand.all.find(_.name == nirBand).map(siderealTarget.magnitudeIn).flatten.orNull,
      new HMS(siderealTarget.coordinates.ra.toAngle.toHMS.hours).toString,
      new DMS(siderealTarget.coordinates.dec.toAngle.toDegrees).toString,
        MagnitudeBand.all.find(_.name == unusedBands(0)).map(siderealTarget.magnitudeIn).flatten.orNull,
        MagnitudeBand.all.find(_.name == unusedBands(1)).map(siderealTarget.magnitudeIn).flatten.orNull).asJava)
  }
}
