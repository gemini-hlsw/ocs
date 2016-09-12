package edu.gemini.pot

import edu.gemini.shared.skyobject
import edu.gemini.skycalc
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.offset.OffsetPosBase
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters.ImOptionOps
import jsky.coords.WorldCoords

import scalaz._
import Scalaz._

/**
 * This code is a conglomeration of some methods from the edu.gemini.ags package's impl/package.scala file
 * and edu.gemini.ags.gems.GemsUtils4Java. Eventually, as this code may be useful in other places, the
 * original files should be removed and these converters used instead.
 */
object ModelConverters {
  def toCoordinates(coords: skyobject.coords.SkyCoordinates): Coordinates = coords.toHmsDeg(0L).toNewModel

  def toCoordinates(coords: skycalc.Coordinates): Coordinates = coords.toNewModel

  def toCoordinates(coords: WorldCoords): Coordinates = coords.toNewModel

  def toSideralTarget(spTarget: SPTarget, when: GOption[java.lang.Long]):SiderealTarget = spTarget.toSiderealTarget(when)

  def toOffset(pos: OffsetPosBase): Offset = pos.toNewModel

  def toNewAngle(angle: skycalc.Angle): Angle = angle.toNewModel

  implicit class OldAngle2New(val angle: skycalc.Angle) extends AnyVal{
    def toNewModel: Angle = Angle.fromDegrees(angle.toDegrees.getMagnitude)
  }

  implicit class NewAngle2Old(val angle: Angle) extends AnyVal {
    def toOldModel: skycalc.Angle = skycalc.Angle.degrees(angle.toDegrees)
  }

  // We need a way to convert angles from [0,maxValOfUnit) to [-maxValOfUnit/2,maxValOfUnit/2) for a number of purposes.
  private val maxRadians = 2 * math.Pi
  private val maxDegrees = 360
  private val maxArcmins = 60 * maxDegrees
  private val maxArcsecs = 60 * maxArcmins
  implicit class AngleNormalizer(val angle: Angle) extends AnyVal {
    private def normalize(value: Double, maxValue: Double): Double = {
      val neg = value - maxValue
      if (value < math.abs(neg)) value else neg
    }

    def toNormalizedRadians:    Double = normalize(angle.toRadians, maxRadians)
    def toNormalizedDegrees:    Double = normalize(angle.toDegrees, maxDegrees)
    def toNormalizedArcmins:    Double = normalize(angle.toArcmins, maxArcmins)
    def toNormalizedArcseconds: Double = normalize(angle.toArcsecs, maxArcsecs)
  }

  implicit class ReallyOldOffset2New(val offset: OffsetPosBase) extends AnyVal {
    def toNewModel: Offset =
      Offset(offset.getXaxis.arcsecs[OffsetP], offset.getYaxis.arcsecs[OffsetQ])
  }

  implicit class OldOffset2New(val offset: skycalc.Offset) extends AnyVal {
    def toNewModel: Offset =
      Offset(offset.p().toDegrees.getMagnitude.degrees[OffsetP],
             offset.q().toDegrees.getMagnitude.degrees[OffsetQ])
  }

  implicit class NewOffset2Old(val offset: Offset) extends AnyVal {
    def toOldModel: skycalc.Offset =
      new skycalc.Offset(skycalc.Angle.degrees(offset.p.degrees),
                         skycalc.Angle.degrees(offset.q.degrees))
  }

  implicit class OldCoordinates2New(val c: skycalc.Coordinates) extends AnyVal {
    def toNewModel: Coordinates = Coordinates(RightAscension.fromAngle(c.getRa.toNewModel), Declination.fromAngle(c.getDec.toNewModel).getOrElse(Declination.zero))
  }

  implicit class WorldCoords2New(val c: WorldCoords) extends AnyVal {
    def toNewModel: Coordinates = Coordinates(RightAscension.fromAngle(Angle.fromDegrees(c.getRaDeg)), Declination.fromAngle(Angle.fromDegrees(c.getDecDeg)).getOrElse(Declination.zero))
  }

  implicit class HmsDegCoords2Coordinates(val c: skyobject.coords.HmsDegCoordinates) extends AnyVal {
    def toNewModel: Coordinates = Coordinates(RightAscension.fromAngle(c.getRa.toNewModel), Declination.fromAngle(c.getDec.toNewModel).getOrElse(Declination.zero))
  }

  implicit class SPTarget2SiderealTarget(val sp:SPTarget) extends AnyVal {

    def toSiderealTarget(when: GOption[java.lang.Long]): SiderealTarget =
       toSiderealTarget(when.asScalaOpt.map(_.longValue))

    def toSiderealTarget(when: Option[Long]): SiderealTarget =
      sp.getTarget.fold(
        too => SiderealTarget.empty.copy(name = too.name),
        sid => sid,
        ns  =>
          SiderealTarget(
            name                 = ns.name,
            coordinates          = when.flatMap(ns.coords).getOrElse(Coordinates.zero),
            properMotion         = None,
            redshift             = None,
            parallax             = None,
            magnitudes           = ns.magnitudes,
            spectralDistribution = ns.spectralDistribution,
            spatialProfile       = ns.spatialProfile
          )
      )

  }
}
