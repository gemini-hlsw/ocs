package edu.gemini.spModel.core

/** Algebraic type of orbital parameters. */
sealed abstract class ConicParameter(val units: Units) {
  val value: Double
}

/** Module of constructors for conic parameters. */
object ConicParameter {
  final case class EpochOfElevation(value: Double) extends ConicParameter(Units.Years)
  final case class EpochOfPerihelion(value: Double) extends ConicParameter(Units.Years)
  final case class Inclination(value: Double) extends ConicParameter(Units.Degrees)
  final case class Longitude(value: Double) extends ConicParameter(Units.Degrees)
  final case class LongitudeOfAscendingNode(value: Double) extends ConicParameter(Units.Degrees)
  final case class MeanAnomaly(value: Double) extends ConicParameter(Units.Degrees)
  final case class MeanDailyMotion(value: Double) extends ConicParameter(Units.DegreesPerDay)
  final case class MeanDistance(value: Double) extends ConicParameter(Units.AU)
  final case class Perihelion(value: Double) extends ConicParameter(Units.Degrees)
  final case class PerihelionDistance(value: Double) extends ConicParameter(Units.AU)
}

sealed abstract class Units(val name: String)

object Units {
  // N.B. the names here are meaningful to the TCC and should not be changed
  case object Angstroms extends Units("angstroms")
  case object Arcsecs extends Units("arcsecs")
  case object ArcsecsPerYear extends Units("arcsecs/year")
  case object AU extends Units("au")
  case object Degrees extends Units("degrees")
  case object DegreesPerDay extends Units("degrees/day")
  case object HMS extends Units("hours/minutes/seconds")
  case object KmPerSec extends Units("km/sec")
  case object Microns extends Units("microns")
  case object Radians extends Units("radians")
  case object SecsPerYear extends Units("seconds/year")
  case object Years extends Units("years")
  case object MilliArcSecsPerYear extends Units("milli-arcsecs/year")
}

