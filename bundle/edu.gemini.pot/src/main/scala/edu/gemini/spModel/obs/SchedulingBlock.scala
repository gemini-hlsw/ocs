package edu.gemini.spModel.obs

import edu.gemini.skycalc.ObservingNight
import edu.gemini.spModel.core.Site

import scalaz._, Scalaz._

/**
 * A reference time for planning observations and for computing mean parallactic angle. The duration
 * of a scheduling block can be unstated, explicitly specified by the user, or computed based on
 * remaining unexecuted steps.
 */
final case class SchedulingBlock(start: Long, duration: SchedulingBlock.Duration) {

  /** Observing night of the scheduling block start, at the given site. */
  def observingNight(site: Site): ObservingNight =
    new ObservingNight(site, start)

  /** True if these blocks fall on the same observing night. */
  def sameObservingNightAs(other: SchedulingBlock): Boolean =
    observingNight(Site.GN) == other.observingNight(Site.GN) // site is arbitrary

}

object SchedulingBlock {

  /** Construct a SchedulingBlock with an unstated duration. */
  def apply(start: Long): SchedulingBlock =
    apply(start, Duration.Unstated)

  val start:    SchedulingBlock @> Long     = Lens.lensu((a, b) => a.copy(start = b), _.start)
  val duration: SchedulingBlock @> Duration = Lens.lensu((a, b) => a.copy(duration = b), _.duration)

  sealed abstract class Duration extends Product with Serializable {
    import Duration._

    def fold[A](u: => A)(e: Long => A)(c: Long => A): A =
      this match {
        case Unstated     => u
        case Explicit(ms) => e(ms)
        case Computed(ms) => c(ms)
      }

    def toOption: Option[Long] =
      fold(none[Long])(some)(some)

    def isExplicit: Boolean =
      fold(false)(_ => true)(_ => false)

  }

  object Duration {

    final case object Unstated extends Duration

    final case class Explicit(ms: Long) extends Duration
    object Explicit extends (Long => Explicit) {
      val ms: Explicit @> Long = Lens.lensu((a, b) => a.copy(ms = b), _.ms)
    }

    final case class Computed(ms: Long) extends Duration
    object Computed extends (Long => Computed) {
      val ms: Computed @> Long = Lens.lensu((a, b) => a.copy(ms = b), _.ms)
    }

  }

}
