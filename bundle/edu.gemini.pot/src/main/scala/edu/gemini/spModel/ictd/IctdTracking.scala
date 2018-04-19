package edu.gemini.spModel.ictd

import edu.gemini.spModel.ictd.Availability._
import scala.collection.immutable.Set

import scalaz._
import Scalaz._

/** Describes a mechanism for determining the availability of an instrument
  * feature such a filter or disperser. The mapping from Java enumeration to
  * ICTD values is not 1:1.  There are some enumerations (GMOS fiters) that
  * correspond to combinations of physical filters and each is tracked
  * individually in the ICTD.  Also, some enumeration values, such as a GMOS
  * "MIRROR" disperser setting aren't tracked in the ICTD at all but are simply
  * understood to always be available.
  *
  * We use a "These" object to describe what needs to happen to find the
  * availability.  A This value  means that the item is not tracked and should
  * always have the indicated availability.  A That value means that the item is
  * tracked in the ICTD under the given individual component name(s).  The
  * availability as a whole is the same as the least available individual item.
  * Finally a Both value combines these two and again the availability as a
  * whole is a combination of the untracked availability and the availability
  * of the tracked items.
  */
final case class IctdTracking(toThese: Availability \&/ NonEmptyList[String]) {

  /** Combines two IctdTracking such that the availability of the whole is the
    * least availability of the two parts.
    */
  def plus(that: IctdTracking): IctdTracking =
    // The resulting IctdTracking would resolve the same without the first two
    // cases, but then we wouldn't have a Monoid Zero.
    (this, that) match {
      case (_, IctdTracking(\&/.This(Installed))) => this
      case (IctdTracking(\&/.This(Installed)), _) => that
      case _                                      => IctdTracking(toThese.append(that.toThese))
    }

  /** Resolves the overall availability of the enum item using a function that
    * supplies the availability of its constituent parts.
    */
  def resolve(f: String => Option[Availability]): Availability =
    toThese.bifoldMap(identity) {
      _.foldMap(f(_).getOrElse(Missing))
    }

}

object IctdTracking {

  /** Creates an untracked, constant, availability. */
  def notTracked(a: Availability): IctdTracking =
    IctdTracking(\&/.This(a))

  /** Indicates that the corresponding item is always installed. */
  val installed: IctdTracking =
    notTracked(Installed)

  /** Indicates that the corresponding item is always unavailable. */
  val unavailable: IctdTracking =
    notTracked(Unavailable)

  /** Indicates that the corresponding item is tracked under the given name in
    * the ICTD database.
    */
  def track(name: String): IctdTracking =
    IctdTracking(\&/.That(NonEmptyList(name)))

  /** Indicates that the corresponding item is tracked by multiple individual
    * entries corresponding to the given names.
    */
  def trackAll(name: String, names: String*): IctdTracking =
    IctdTracking(\&/.That(NonEmptyList(name, names: _*)))

  /** Alias for `installed`. */
  def zero: IctdTracking =
    installed

  implicit val EqualIctdTracking: Equal[IctdTracking] =
    Equal.equalBy(_.toThese)

  implicit val MonoidIctdTracking: Monoid[IctdTracking] =
    new Monoid[IctdTracking] {
      def zero: IctdTracking =
        IctdTracking.zero

      def append(a0: IctdTracking, a1: => IctdTracking): IctdTracking =
        a0.plus(a1)
    }

}
