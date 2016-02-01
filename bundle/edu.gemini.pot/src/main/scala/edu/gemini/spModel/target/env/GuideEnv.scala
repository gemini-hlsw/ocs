package edu.gemini.spModel.target.env

import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPTarget

import scalaz._
import Scalaz._

import GuideEnv.Manual

/** A pair of an `AutomaticGroup` guide group and, optionally, a non-empty list
  * of manual groups.  The `GuideEnv` maintains the concept of a "primary"
  * group.  It is the group that will be used for guiding at night. The
  * "primary" group is either the `AutomaticGroup` or else one of the various
  * `ManualGroup`s (if any).  If the `manual` group option is defined and if the
  * contained `OptsList` has a focused element, it is the "primary" group.
  * Otherwise, the automatic group is considered to be the primary.
  */
final case class GuideEnv(auto: AutomaticGroup, manual: Option[OptsList[ManualGroup]]) {

  /** All cotained `GuideGrp` starting with the `AutomaticGroup` and followed
    * by any `ManualGroup`s that might be present.
    */
  def groups: List[GuideGrp] =
    auto :: (manual.map(_.toList) | Nil)

  /** Gets the "primary" group which will be used to configure guiding at night.
    * Other `GuideGrp`s in this environment are alternative options.
    */
  def primaryGroup: GuideGrp =
    (manual.flatMap(_.focus) : Option[GuideGrp]) | auto

  /** Returns the index of the primary group in the list of all guide group
    * options contained in the environment.  The `AutomaticGroup` is always at
    * index 0 followed by the manual groups (if any) in order.
    */
  def primaryIndex: Int =
    manual.flatMap(_.focusIndex).map(_ + 1).getOrElse(0)

  /** Returns all guide probes associated with a guide star, whether or not
    * they are configured to be used in this observation.
    */
  def referencedGuiders: Set[GuideProbe] =
    groups.foldMap(_.referencedGuiders)

  /** Returns the guide probes that will actually be in use to execute the
    * observation.
    */
  def primaryReferencedGuiders: Set[GuideProbe] =
    primaryGroup.primaryReferencedGuiders

  /** Gets the total number of `GuideGrp`s contained in this environment. */
  def length: Int =
    1 + manual.map(_.length).orZero

  /** Selects the `GuideGrp` associated with the given index, if it exists.
    * If not `None` is returned.
    */
  def selectPrimaryIndex(i: Int): Option[GuideEnv] =
    if (i == 0) some(Manual.mod(_.map(_.clearFocus), this))
    else manual.flatMap { _.focusOnIndex(i-1) }.map { opts =>
      Manual.set(this, some(opts))
    }

  def toList: List[GuideGrp] =
    auto :: manual.map(_.toList).orZero

  def toNel: NonEmptyList[GuideGrp] =
    NonEmptyList.nel(auto, manual.map(_.toList).orZero)
}

object GuideEnv {

  /** Initial/default `GuideEnv`. Contains an `AutomaticGroup.Initial` and no
    * manual groups.
    */
  val initial: GuideEnv = GuideEnv(AutomaticGroup.Initial, none)

  val Auto: GuideEnv @> AutomaticGroup =
    Lens.lensu((ge,a) => ge.copy(auto = a), _.auto)

  val Manual: GuideEnv @> Option[OptsList[ManualGroup]] =
    Lens.lensu((ge,m) => ge.copy(manual = m), _.manual)

  import TargetCollection._

  implicit val TargetCollectionGuideEnv: TargetCollection[GuideEnv] = new TargetCollection[GuideEnv] {
    def mod(ge: GuideEnv)(fa: AutomaticGroup => AutomaticGroup, fm: ManualGroup => ManualGroup): GuideEnv = {
      val s: State[GuideEnv, Unit] = (Auto %== fa) *> (Manual %== (_.map(_.map(fm))))
      s.exec(ge)
    }

    override def cloneTargets(ge: GuideEnv): GuideEnv =
      mod(ge)(_.cloneTargets, _.cloneTargets)

    override def containsTarget(ge: GuideEnv, t: SPTarget): Boolean =
      ge.auto.containsTarget(t) || ge.manual.exists(_.toList.exists(_.containsTarget(t)))

    override def removeTarget(ge: GuideEnv, t: SPTarget): GuideEnv =
      mod(ge)(_.removeTarget(t), _.removeTarget(t))

    type TargetMap = GuideProbe ==>> NonEmptyList[SPTarget]

    override def targets(ge: GuideEnv): TargetMap = {
      def merge(tm0: TargetMap, tm1: TargetMap): TargetMap =
        tm0.intersectionWith(tm1)(_.append(_)) union tm0 union tm1

      (ge.auto.targets :: ge.manual.fold(List.empty[TargetMap]) {
        _.toList.map(_.targets)
      }).reduceLeft(merge)
    }
  }

  implicit val EqualGuideEnv: Equal[GuideEnv] = Equal.equal { (ge0, ge1) =>
    ge0.auto === ge1.auto && ge0.manual === ge1.manual
  }
}