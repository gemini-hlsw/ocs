package edu.gemini.spModel.target.env

import edu.gemini.spModel.target.env.Indexable._

import edu.gemini.shared.util.immutable.{Option => GemOption, ImOption, ImList}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.pio.{Pio, PioFactory, ParamSet}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetCollection._

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

import GuideEnvironment._

/** The old Java codebase compatible wrapper around the concept of a guide
  * environment.  See `GuideEnv`.
  */
final case class GuideEnvironment(guideEnv: GuideEnv) extends TargetContainer {

  def getReferencedGuiders: java.util.SortedSet[GuideProbe] =
    toSortedSet(guideEnv.referencedGuiders)

  def getPrimaryReferencedGuiders: java.util.SortedSet[GuideProbe] =
    toSortedSet(guideEnv.primaryReferencedGuiders)

  override def cloneTargets(): GuideEnvironment =
    env.mod(_.cloneTargets, this)

  override def containsTarget(target: SPTarget): Boolean =
    guideEnv.containsTarget(target)

  /** Removes the target from any manual groups in which it is found. */
  override def removeTarget(target: SPTarget): GuideEnvironment =
    env.mod(_.removeTarget(target), this)

  /** Removes the manual `GuideGroup` at the associated index, if possible,
    * returning a new updated environment. The automatic guide group (index 0)
    * cannot be removed and otherwise the index must be in range or else nothing
    * is different in the returned `GuideEnvironment`.
    */
  def removeGroup(index: Int): GuideEnvironment =
    index match {
      case 0 => this // can't remove the automatic group
      case i => manual.mod(_.flatMap { _.deleteAt(i-1) }, this)
    }

  /** Gets the automatic group. */
  def automaticGroup: GuideGroup =
    GuideGroup(guideEnv.auto)

  /** Gets the manual groups. */
  def manualGroups: List[GuideGroup] =
    guideEnv.manual.map(_.toList.map(GuideGroup)) | Nil

  /** Gets the `GuideGroup` at the given index, if any.  If the index is out
    * of range, `None` is returned.  If the index is 0, the result will be the
    * automatic group, otherwise one of the manual groups.
    */
  def getGroup(index: Int): GemOption[GuideGroup] =
    index match {
      case 0 => ImOption.apply(GuideGroup(guideEnv.auto))
      case i => guideEnv.manual.flatMap(_.getAt(i-1)).map(GuideGroup).asGeminiOpt
    }

  /** Sets the `GuideGroup` at the given index to the provided value.  If the
    * index is 0, the `GuideGroup` must be an automatic group, otherwise it must
    * be a manual group.  If not, or if the index is out of range, nothing is
    * changed.
    *
    * This method is intended by be given a `GuideGroup` obtained from
    * `getGroup` at the same index, or a guide group produced by manipulating
    * the `GuideGroup.AutomaticInitial` or `GuideGroup.ManualEmpty`.
    */
  def setGroup(index: Int, grp: GuideGroup): GuideEnvironment =
    (index, grp.grp) match {
      case (0, a: AutomaticGroup) => auto.set(this, a)
      case (i, m: ManualGroup)    => manual.mod(_.map(o => o.setAt(i-1, m).getOrElse(o)), this)
      case _                      => this
    }

  /**
    * Convenience method to set the automatic group, which is always in position 0.
    * Note that as per `setGroup` above, this must be an automatic group.
    */
  def setAutomaticGroup(grp: GuideGroup): GuideEnvironment =
    setGroup(0, grp)

  def modifyGroup(index: Int, f: GuideGroup => GuideGroup): GuideEnvironment =
    getGroup(index).asScalaOpt.fold(this) { g => setGroup(index, f(g)) }

  override def getTargets: ImList[SPTarget] =
    guideEnv.targetList.asImList

  /** Gets all the `GuideGroup` options in the environment, starting with the
    * automatic group and followed by any manual group options.
    */
  def getOptions: ImList[GuideGroup] =
    guideEnv.groups.map(GuideGroup).asImList

  // TODO: only used by BagsManager so can be removed when BagsManager is updated
  def update(op: OptionsList.Op[GuideGroup]): GuideEnvironment =
    ???

  /** Roughly replaces all the groups with the given groups and keeps the index
    * of the selected group the same, if possible.  If the `newList` contains
    * multiple `AutomaticGroup` instances, only the first is kept.
    *
    * This method may have surprising behavior if there are multiple automatic
    * guide groups or if the `newList` is not a slightly modified version of the
    * existing list.
    */
  def setOptions(newList: ImList[GuideGroup]): GuideEnvironment = {
    // This is an awkward, wacky method that updates the available guide groups
    // and tries to keep the selected group the "same".  It predates the new
    // model and has no concept of a single, always-present automatic group.

    // The newList should contain exactly one automatic guide group, which
    // should be the first in the list.  The new model won't support anything
    // else.  We therefore will have to massage the list accordingly if it
    // does not conform.

    // We need the initial automatic group, which should be the first element in
    // the list.  If there is no first element or if it is manual, we'll use a
    // new AutomaticGroup.Initial.
    val autoOpt = newList.headOption.asScalaOpt.map(_.grp).collect {
      case a: AutomaticGroup => a
    }
    val auto    = autoOpt | AutomaticGroup.Initial

    // Everything in `newList` except for the first element (if it is automatic)
    // should be manual, so convert to manual if necessary
    val manualList = autoOpt.fold(newList)(_ => newList.tail).asScalaList.map { g => g.grp match {
      case a: AutomaticGroup => a.toManualGroup
      case m: ManualGroup    => m
    }}

    // Compute the manual guide group Option[OptsList[ManualGroup]] to reflect
    // the correct selection.  We will try to keep the primary index the same
    // but if removing GuideGroups may have to resort to the last element.

    val all      = auto :: (manualList: List[GuideGrp])
    val allSize  = all.size
    val oldIndex = guideEnv.primaryIndex
    val newIndex = (oldIndex < allSize) ? oldIndex | (allSize - 1)

    val manual =
      if (newIndex === 0)
        manualList.headOption.map { h => OptsList.unfocused(h, manualList.tail) }
      else
        manualList.splitAt(newIndex - 1) match { // - 1 to account for the auto group
          case (Nil, Nil)               => None
          case (h :: t, Nil)            => Some(OptsList.unfocused(h, t))
          case (lefts, focus :: rights) => Some(OptsList.focused(lefts, focus, rights))
        }

    GuideEnvironment(GuideEnv(auto, manual))
  }

  def getPrimary: GuideGroup =
    GuideGroup(guideEnv.primaryGroup)

  /** Roughly, replaces the primary group with the given guide group, if there
    * is a primary group.  Otherwise it appends the given guide group and makes
    * it the primary.
    *
    * On the other hand, if the primary is a manual group and an automatic is
    * given, the automatic group is replaced and made primary.  If the primary
    * is automatic and a manual group provided, the manual group is selected/
    * added and made primary.
    *
    * @deprecated This is such a hopelessly confusing method that it is best
    *             avoided altogether.  See the more straightforward
    *             `selectPrimary` instead.
    */
  def setPrimary(primary: GuideGroup): GuideEnvironment = {
    val (auto, manual) = primary.grp match {
      case a: AutomaticGroup =>
        (a, guideEnv.manual.map(_.clearFocus))

      case m: ManualGroup =>
        val opts0 = guideEnv.manual.fold(OptsList.focused(m)) { opts =>
          opts.focusIndex.fold(OptsList.focused(opts.toList, m, Nil)) { i =>
            opts.setAt(i, m).getOrElse(opts)
          }
        }
        (guideEnv.auto, some(opts0))
    }
    GuideEnvironment(GuideEnv(auto, manual))
  }

  def getPrimaryIndex: java.lang.Integer =
    guideEnv.primaryIndex

  def setPrimaryIndex(primary: Int): GuideEnvironment =
    guideEnv.selectPrimaryIndex(primary).map(GuideEnvironment(_)).getOrElse(this)

  def iterator(): java.util.Iterator[GuideGroup] =
    guideEnv.groups.map(GuideGroup).asJava.iterator

  /** Roughly, calls `put` on the given guide group (see `GuideGroup.put`) to
    * obtain an updated `GuideGroup`.  Replaces the indicated guide group with
    * the updated version.
    */
  def putGuideProbeTargets(index: Int, gpt: GuideProbeTargets): GuideEnvironment =
    modifyGroup(index, _.put(gpt))

  def getParamSet(f: PioFactory): ParamSet = {
    val ps = f.createParamSet(ParamSetName)
    Pio.addIntParam(f, ps, "primary", guideEnv.primaryIndex)
    guideEnv.groups.map(GuideGroup).foreach { g =>
      ps.addParamSet(g.getParamSet(f))
    }
    ps
  }
}

object GuideEnvironment {
  val ParamSetName = "guideEnv"
  val Initial: GuideEnvironment  = GuideEnvironment(GuideEnv.Initial)
  val Disabled: GuideEnvironment = GuideEnvironment(GuideEnv.Disabled)

  val env: GuideEnvironment @> GuideEnv =
    Lens.lensu((a,b) => a.copy(b), _.guideEnv)

  val auto: GuideEnvironment @> AutomaticGroup =
    env >=> GuideEnv.auto

  val manual: GuideEnvironment @> Option[OptsList[ManualGroup]] =
    env >=> GuideEnv.manual

  def create(guideGroups: OptionsList[GuideGroup]): GuideEnvironment = {
    val opts0    = guideGroups.getOptions.asScalaList
    val primary0 = guideGroups.getPrimaryIndex.getOrElse(0).intValue
    val (opts, primary) =
      if (opts0.headOption.exists(_.grp.isAutomatic)) (opts0, primary0)
      else (GuideGroup.AutomaticInitial :: opts0, primary0 + 1)

    Initial.setOptions(opts.asImList).setPrimaryIndex(primary)
  }

  def fromParamSet(parent: ParamSet): GuideEnvironment = {
    val groups = parent.getParamSets.asScala.toList.map(GuideGroup.fromParamSet)
    Initial.setOptions(groups.asImList).setPrimaryIndex(Pio.getIntValue(parent, "primary", 0))
  }

  private def toSortedSet(s: Set[GuideProbe]): java.util.SortedSet[GuideProbe] =
    new java.util.TreeSet(GuideProbe.KeyComparator.instance) <|
      (_.addAll(s.asJavaCollection))

  implicit val EqualGuideEnvironment: Equal[GuideEnvironment] = Equal.equalBy(_.guideEnv)

  implicit val TargetCollectionGuideEnvironment: TargetCollection[GuideEnvironment] =
    TargetCollection.wrapping(env)
}
