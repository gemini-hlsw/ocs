package edu.gemini.spModel.core

/** Algebraic type for targets of observation. */
sealed trait Target {

  /** A human-readable name for this `Target`. */
  def name: String

  /** Coordinates for this target at the specified time, if known. */
  def coords(time: Long): Option[Coordinates]
  
  /** Epoch for this target. */
  def epoch: CoordinatesEpoch

}

/** A target of opportunity, with no coordinates. */
case class TooTarget(name: String) extends Target {
  def epoch = CoordinatesEpoch.J2000
  def coords(time: Long) = None
}

/** A sidereal target. */
case class SiderealTarget (
  name: String,
  coordinates: Coordinates,
  epoch: CoordinatesEpoch,
  properMotion: Option[ProperMotion],
  magnitudes: List[Magnitude]) extends Target {

  // TODO: interpolate proper motion
  def coords(date: Long) = Some(coordinates)

}

case class NonSiderealTarget(
  name: String,
  ephemeris: List[EphemerisElement],
  epoch: CoordinatesEpoch) extends Target {

  def coords(date: Long) = 
    for {
      (a, b, f) <- find(date, ephemeris)
      (cA, cB)   = (a.coords, b.coords)
      ra         = RightAscension fromAngle Angle.fromDegrees(f(cA.ra. toAngle.toDegrees, cB.ra. toAngle.toDegrees))
      dec       <- Declination    fromAngle Angle.fromDegrees(f(cA.dec.toAngle.toDegrees, cB.dec.toAngle.toDegrees))
    } yield Coordinates(ra, dec)

  /** Magnitude for this target at the specified time, if known. */
  def magnitude(date: Long): Option[Double] = 
    for {
      (a, b, f) <- find(date, ephemeris)
      mA        <- a.magnitude
      mB        <- b.magnitude
    } yield f(mA, mB)

  // @annotation.tailrec
  // private def find(date: Long, es: List[EphemerisElement]): Option[(EphemerisElement, EphemerisElement, (Double, Double) => Double)] = 
  //   es match {

  //     // If the date lies between consecutive elements, we're done
  //     case a :: b :: _ if a.validAt <= date && date <= b.validAt =>
  //       val factor = (date.doubleValue - a.validAt) / (b.validAt - a.validAt) // between 0 and 1
  //       def interp(a: Double, b: Double) = a + (b - a) * factor
  //       Some((a, b, interp))

  //     // Otherwise examine the tail, if any
  //     case _ :: es => find(date, es)
  //     case Nil     => None

  //   }

  private def find(date: Long, es: List[EphemerisElement]): Option[(EphemerisElement, EphemerisElement, (Double, Double) => Double)] = 
    es match {
      case Nil       => None
      case _ :: tail => 
        es.zip(tail).collectFirst { case (a, b) if a.validAt <= date && date <= b.validAt =>
          val factor = (date.doubleValue - a.validAt) / (b.validAt - a.validAt) // between 0 and 1
          def interp(a: Double, b: Double) = a + (b - a) * factor
          (a, b, interp)
        }
    }

}

// TODO: ConicTarget w/ orbital elements

