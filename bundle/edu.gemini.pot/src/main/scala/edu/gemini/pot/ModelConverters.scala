package edu.gemini.pot

import edu.gemini.shared.skyobject
import edu.gemini.skycalc
import edu.gemini.spModel.core.Target.SiderealTarget
import edu.gemini.spModel.core._
import edu.gemini.spModel.core.AngleSyntax._
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget

/**
 * This code is a conglomeration of some methods from the edu.gemini.ags package's impl/package.scala file
 * and edu.gemini.ags.gems.GemsUtils4Java. Eventually, as this code may be useful in other places, the
 * original files should be removed and these converters used instead.
 */
object ModelConverters {
  def toCoordinates(coords: skyobject.coords.SkyCoordinates): Coordinates = {
    val c = coords.toHmsDeg(0L)
    Coordinates(RightAscension.fromAngle(c.getRa.toNewModel), Declination.fromAngle(c.getDec.toNewModel).getOrElse(Declination.zero))
  }

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

  implicit class OldMagnitudeBand2New(val band: skyobject.Magnitude.Band) extends AnyVal {
    def toNewModel: MagnitudeBand = band match {
      case edu.gemini.shared.skyobject.Magnitude.Band.u  => MagnitudeBand._u
      case edu.gemini.shared.skyobject.Magnitude.Band.g  => MagnitudeBand._g
      case edu.gemini.shared.skyobject.Magnitude.Band.r  => MagnitudeBand._r
      case edu.gemini.shared.skyobject.Magnitude.Band.i  => MagnitudeBand._i
      case edu.gemini.shared.skyobject.Magnitude.Band.z  => MagnitudeBand._z

      case edu.gemini.shared.skyobject.Magnitude.Band.U  => MagnitudeBand.U
      case edu.gemini.shared.skyobject.Magnitude.Band.B  => MagnitudeBand.B
      case edu.gemini.shared.skyobject.Magnitude.Band.V  => MagnitudeBand.V
      case edu.gemini.shared.skyobject.Magnitude.Band.UC => MagnitudeBand.UC
      case edu.gemini.shared.skyobject.Magnitude.Band.R  => MagnitudeBand.R
      case edu.gemini.shared.skyobject.Magnitude.Band.I  => MagnitudeBand.I
      case edu.gemini.shared.skyobject.Magnitude.Band.Y  => MagnitudeBand.Y
      case edu.gemini.shared.skyobject.Magnitude.Band.J  => MagnitudeBand.J
      case edu.gemini.shared.skyobject.Magnitude.Band.H  => MagnitudeBand.H
      case edu.gemini.shared.skyobject.Magnitude.Band.K  => MagnitudeBand.K
      case edu.gemini.shared.skyobject.Magnitude.Band.L  => MagnitudeBand.L
      case edu.gemini.shared.skyobject.Magnitude.Band.M  => MagnitudeBand.M
      case edu.gemini.shared.skyobject.Magnitude.Band.N  => MagnitudeBand.N
      case edu.gemini.shared.skyobject.Magnitude.Band.Q  => MagnitudeBand.Q
      case edu.gemini.shared.skyobject.Magnitude.Band.AP => MagnitudeBand.AP
    }
  }

  implicit class NewMagnitudeBand2Old(val band: MagnitudeBand) {
    def toOldModel: skyobject.Magnitude.Band = band match {
      case MagnitudeBand._u => edu.gemini.shared.skyobject.Magnitude.Band.u
      case MagnitudeBand._g => edu.gemini.shared.skyobject.Magnitude.Band.g
      case MagnitudeBand._r => edu.gemini.shared.skyobject.Magnitude.Band.r
      case MagnitudeBand._i => edu.gemini.shared.skyobject.Magnitude.Band.i
      case MagnitudeBand._z => edu.gemini.shared.skyobject.Magnitude.Band.z

      case MagnitudeBand.U  => edu.gemini.shared.skyobject.Magnitude.Band.U
      case MagnitudeBand.B  => edu.gemini.shared.skyobject.Magnitude.Band.B
      case MagnitudeBand.G  => edu.gemini.shared.skyobject.Magnitude.Band.g
      case MagnitudeBand.V  => edu.gemini.shared.skyobject.Magnitude.Band.V
      case MagnitudeBand.UC => edu.gemini.shared.skyobject.Magnitude.Band.UC
      case MagnitudeBand.R  => edu.gemini.shared.skyobject.Magnitude.Band.R
      case MagnitudeBand.I  => edu.gemini.shared.skyobject.Magnitude.Band.I
      case MagnitudeBand.Z  => edu.gemini.shared.skyobject.Magnitude.Band.z
      case MagnitudeBand.Y  => edu.gemini.shared.skyobject.Magnitude.Band.Y
      case MagnitudeBand.J  => edu.gemini.shared.skyobject.Magnitude.Band.J
      case MagnitudeBand.H  => edu.gemini.shared.skyobject.Magnitude.Band.H
      case MagnitudeBand.K  => edu.gemini.shared.skyobject.Magnitude.Band.K
      case MagnitudeBand.L  => edu.gemini.shared.skyobject.Magnitude.Band.L
      case MagnitudeBand.M  => edu.gemini.shared.skyobject.Magnitude.Band.M
      case MagnitudeBand.N  => edu.gemini.shared.skyobject.Magnitude.Band.N
      case MagnitudeBand.Q  => edu.gemini.shared.skyobject.Magnitude.Band.Q

      case MagnitudeBand.AP => edu.gemini.shared.skyobject.Magnitude.Band.AP
    }
  }

  implicit class OldMagnitude2New(val m: skyobject.Magnitude) extends AnyVal {
    def toNewModel: Magnitude = new Magnitude(m.getBrightness, m.getBand.toNewModel)
  }

  implicit class NewMagnitude2Old(val m: Magnitude) extends AnyVal {
    def toOldModel: skyobject.Magnitude = new skyobject.Magnitude(m.band.toOldModel, m.value)
  }

  implicit class HmsDegCoords2Coordinates(val c: skyobject.coords.HmsDegCoordinates) extends AnyVal {
    def toNewModel: Coordinates = Coordinates(RightAscension.fromAngle(c.getRa.toNewModel), Declination.fromAngle(c.getDec.toNewModel).getOrElse(Declination.zero))
  }

  implicit class SiderealTarget2SkyObject(val st:SiderealTarget) extends AnyVal {
    def toOldModel: skyobject.SkyObject = {
      val ra          = skycalc.Angle.degrees(st.coordinates.ra.toAngle.toDegrees)
      val dec         = skycalc.Angle.degrees(st.coordinates.dec.toAngle.toDegrees)
      val coordinates = new skyobject.coords.HmsDegCoordinates.Builder(ra, dec).build()
      val mags        = st.magnitudes.map(_.toOldModel)
      new skyobject.SkyObject.Builder(st.name, coordinates).magnitudes(mags: _*).build()
    }
  }

  implicit class SkyObject2SiderealTarget(val so:skyobject.SkyObject) extends AnyVal {
    def toNewModel:SiderealTarget = {
      import scala.collection.JavaConverters._

      val ra          = Angle.fromDegrees(so.getHmsDegCoordinates.getRa.toDegrees.getMagnitude)
      val dec         = Angle.fromDegrees(so.getHmsDegCoordinates.getDec.toDegrees.getMagnitude)
      val coordinates = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
      val mags        = so.getMagnitudes.asScala.map(_.toNewModel)
      SiderealTarget(so.getName, coordinates, None, mags.toList, None)
    }
  }

  implicit class SPTarget2SiderealTarget(val sp:SPTarget) extends AnyVal {
    def toNewModel:SiderealTarget = {
      val name        = sp.getTarget.getName
      val coords      = sp.getTarget.getSkycalcCoordinates
      val mags        = sp.getTarget.getMagnitudes.asScalaList.map(_.toNewModel)
      val ra          = Angle.fromDegrees(coords.getRaDeg)
      val dec         = Angle.fromDegrees(coords.getDecDeg)
      val coordinates = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
      SiderealTarget(name, coordinates, None, mags, None)
    }
  }
}
