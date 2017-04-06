package edu.gemini.spModel.target.env

import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.{Coordinates => SCoordinates}


import java.time.Instant

import scalaz.NonEmptyList

/** Collection of stars that make up the science target(s) for an observation,
  * along with any configuration details unique to the instrument in use.
  */
trait Asterism {

  /** All targets that comprise the asterism.  There must be at least one target.
    */
  def targets: NonEmptyList[SPTarget]

  /** Slew coordinates and AGS calculation base position.  By default this
    * will be a computation that finds the coordinate minimizing the maximum
    * distance to any target in the asterism.
    */
  def basePosition(time: Instant): Option[Coordinates] =
    ???  // we should supply the "smallest-circle" implementation here

  //
  // "Base position" convenience methods already in use extensively throughout
  // the codebase.  Defined in terms of the base position, these are methods
  // on SPTarget that are used directly in calls like
  // targets.getBase.getRaDegrees(when)
  //

  type GOLong   = GOption[java.lang.Long]
  type GODouble = GOption[java.lang.Double]

  private def gcoords[A](time: GOLong)(f: Coordinates => A): GOption[A] =
    (for {
      t <- time.asScalaOpt
      c <- basePosition(Instant.ofEpochMilli(t))
    } yield f(c)).asGeminiOpt

  def getRaHours(time: GOLong): GODouble =
    gcoords(time)(_.ra.toHours)

  def getRaDegrees(time: GOLong): GODouble =
    gcoords(time)(_.ra.toDegrees)

  def getRaString(time: GOLong): GOption[String] =
    gcoords(time)(_.ra.toAngle.formatHMS)

  def getDecDegrees(time: GOLong): GODouble =
    gcoords(time)(_.dec.toDegrees)

  def getDecString(time: GOLong): GOption[String] =
    gcoords(time)(_.dec.formatDMS)

  def getSkycalcCoordinates(time: GOLong): GOption[SCoordinates] =
    gcoords(time)(cs => new SCoordinates(cs.ra.toDegrees, cs.dec.toDegrees))
}
