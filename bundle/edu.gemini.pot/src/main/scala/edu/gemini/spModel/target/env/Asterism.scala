package edu.gemini.spModel.target.env

import edu.gemini.spModel.core._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.shared.util.immutable.{DefaultImList, ImList, Option => GOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.skycalc.{Coordinates => SCoordinates}
import java.time.Instant

import scalaz._
import Scalaz._

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

  /** An Asterism is considered sidereal if and only if all targets are sidereal. */
  def isSidereal: Boolean =
    targets.all(_.isSidereal)

  /** An Asterism is considered non-sidereal if any targets is non-sidereal. */
  def isNonSidereal: Boolean =
    targets.any(_.isNonSidereal)

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

  /** Module of partial methods that apply only to single-target asterisms. Accessors return empty
    * if this is a GHOST asterism.
    */
  object ifSingle {

    def getBase: Option[SPTarget] =
      Some(Asterism.this) collect {
        case Asterism.Single(t) => t
      }

    def getSpectralDistribution: Option[SpectralDistribution] =
      getBase.flatMap(_.getSpectralDistribution)

    def getSpatialProfile: Option[SpatialProfile] =
      getBase.flatMap(_.getSpatialProfile)

    def getSiderealTarget: Option[SiderealTarget] =
      getBase.flatMap(_.getSiderealTarget)

    def getMagnitude(band: MagnitudeBand): Option[Magnitude] =
      getBase.flatMap(_.getMagnitude(band))

    def getMagnitudesJava: ImList[Magnitude] =
      getBase.map(_.getMagnitudesJava).getOrElse(DefaultImList.create[Magnitude]())

    def getNonSiderealTarget: Option[NonSiderealTarget] =
      getBase.flatMap(_.getNonSiderealTarget)

  }

  def ifSingleJava: ifSingle.type =
    ifSingle

}

object Asterism {

  final case class Single(t: SPTarget) extends Asterism {
    val targets = NonEmptyList(t)
    override def basePosition(time: Instant) = t.getCoordinates(Option(time.toEpochMilli))
  }

  /** Construct a single-target Asterism by wrapping the given SPTarget. */
  def single(t: SPTarget): Asterism = Single(t)

}