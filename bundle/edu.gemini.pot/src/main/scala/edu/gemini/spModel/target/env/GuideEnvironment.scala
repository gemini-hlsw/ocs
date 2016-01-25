package edu.gemini.spModel.target.env

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

/**
 *
 */
final case class GuideEnvironment(guideEnv: GuideEnv) extends TargetContainer {

  def getReferencedGuiders: java.util.SortedSet[GuideProbe] =
    toSortedSet(guideEnv.referencedGuiders)

  def getPrimaryReferencedGuiders: java.util.SortedSet[GuideProbe] =
    toSortedSet(guideEnv.primaryReferencedGuiders)

  override def cloneTargets(): GuideEnvironment =
    Env.mod(_.cloneTargets, this)

  override def containsTarget(target: SPTarget): Boolean =
    guideEnv.containsTarget(target)

  /** Removes the target from any manual groups in which it is found. */
  override def removeTarget(target: SPTarget): GuideEnvironment =
    Env.mod(_.removeTarget(target), this)

  def removeGroup(group: GuideGroup): GuideEnvironment =
    group.grp match {
      case m: ManualGroup => Manual.mod(_.flatMap { _.delete(m) }, this)
      case _              => this
    }

  override def getTargets: ImList[SPTarget] =
    guideEnv.targetList.asImList

  def getOptions: ImList[GuideGroup] =
    guideEnv.groups.map(GuideGroup).asImList

  // TODO: only used by BagsManager so can be removed when BagsManager is updated
  def update(op: OptionsList.Op[GuideGroup]): GuideEnvironment =
    ???

  /** Roughly replaces all the groups with the given groups and keeps the index
    * of the selected group the same, if possible.  If the `newList` contains
    * multiple `AutomaticGroup` instances, only the first is kept.
    *
    * @deprecated This method will have surprising behavior if there are
    *             multiple automatic guide groups or if the `newList` is not a
    *             slightly modified version of the existing list.
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

    // Select the primary group as in the old Java GuideEnvironment impl...  It
    // should be the same as the existing primary group if it has not been
    // modified in `newList`.  Otherwise, pick the group at the same index if
    // there is one.  Otherwise, pick the last one.

    val all        = auto :: (manualList: List[GuideGrp])
    val allSize    = all.size
    val oldPrimary = guideEnv.primaryGroup

    // If there are multiple groups matching the oldPrimary we'll want the
    // closest one to oldIndex
    val oldIndex0  = guideEnv.primaryIndex
    val oldIndex   = (oldIndex0 < allSize) ? oldIndex0 | (allSize - 1)
    val newIndex   = all.zipWithIndex.filter(_._1 === oldPrimary).unzip._2 match {
      case Nil => oldIndex
      case ps  => ps.minBy { i => (oldIndex - i).abs }
    }

    val manual =
      if (newIndex === 0) manualList.headOption.map { h => OptsList.unfocused(h, manualList.tail) }
      else manualList.splitAt(newIndex - 1) match {
        case (Nil, Nil)               => None
        case (h :: t, Nil)            => Some(OptsList.unfocused(h, t))
        case (lefts, focus :: rights) => Some(OptsList.focused(lefts, focus, rights))
      }

    GuideEnvironment(GuideEnv(auto, manual))
  }

  // TODO: primary is always defined, remove the option wrapper
  def getPrimary: GemOption[GuideGroup] =
    ImOption.apply(GuideGroup(guideEnv.primaryGroup))

  /** Roughly, replaces the primary group with the given guide group, if there
    * is a primary group.  Otherwise it appends the given guide group and makes
    * it the primary.  If the given group already exists in the environment,
    * it is simply selected as primary.
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
          // If m exists in opts, select it as primary.  Otherwise, append it
          // to the options list and select it as primary.
          opts.toList.span(_ != m) match {
            case (_, Nil)    =>
              // m doesn't exist in opts.  If there is no primary, append m
              // and select it as primary.  If there is a primary, replace it
              // with m.
              opts.toDisjunction match {
                case -\/(l) => OptsList.focused(l.toList, m, Nil)
                case \/-(z) => OptsList(z.update(m).right)
              }
            case (l, _ :: r) =>
              // m exists in opts, so just select it
              OptsList.focused(l, m, r)
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

  def selectPrimary(primary: GuideGroup): GuideEnvironment =
    guideEnv.selectPrimary(primary.grp).map(GuideEnvironment(_)).getOrElse(this)

  def iterator(): java.util.Iterator[GuideGroup] =
    guideEnv.groups.map(GuideGroup).asJava.iterator

  /** Roughly, calls `put` on the given guide group (see `GuideGroup.put`) to
    * obtain an updated `GuideGroup`.  Replaces the `current` guide group in
    * the environment if it exists without changing the selected group.  If the
    * `current` group is not already in this guide environment, the updated
    * group is added and made the primary group.  The way that the updated
    * group is added depends on its type.  If automatic, it replaces the
    * current automatic group.  If manual, it is appended to the end of the
    * manual options.
    *
    * @deprecated Confusing, surprising behavior.  Avoid.
    */
  def putGuideProbeTargets(current: GuideGroup, gpt: GuideProbeTargets): GuideEnvironment = {
    val updated = current.put(gpt)

    updated.grp match {
      case a: AutomaticGroup =>
        Auto.set(this, a)

      case m: ManualGroup    =>
        val opts2 = guideEnv.manual.fold(OptsList.focused(m)) { opts =>
          // Remember the focus and try to find and replace the current group in
          // the list of options.  If not found, append the updated group and
          // mark it as having focus.
          val lst = opts.withFocus.toList.span { case (mg, _) => (mg: GuideGrp) =/= current.grp } match {
            case (lefts, (_, f) :: rights) => lefts ::: ((m, f) :: rights)
            case (lefts, Nil)              => lefts.unzip._1.zip(Stream.continually(false)) :+ ((m, true))
          }

          // Look for the focused element, if any. Create the appropriate type
          // OptsList depending on whether there is a focus.
          lst.span(_._2 === false) match {
            case (lefts, Nil)              => OptsList.unfocused(lefts.unzip._1).get
            case (lefts, (g, _) :: rights) => OptsList.focused(lefts.unzip._1, g, rights.unzip._1)
          }
        }
        Manual.set(this, Some(opts2))
    }
  }

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
  val Initial: GuideEnvironment = GuideEnvironment(GuideEnv.initial)

  val Env: GuideEnvironment @> GuideEnv =
    Lens.lensu((a,b) => a.copy(b), _.guideEnv)

  val Auto: GuideEnvironment @> AutomaticGroup =
    Env >=> GuideEnv.Auto

  val Manual: GuideEnvironment @> Option[OptsList[ManualGroup]] =
    Env >=> GuideEnv.Manual

  def create(guideGroups: OptionsList[GuideGroup]): GuideEnvironment = {
    val primary = guideGroups.getPrimaryIndex.getOrElse(0)
    Initial.setOptions(guideGroups.getOptions).setPrimaryIndex(primary)
  }

  def fromParamSet(parent: ParamSet): GuideEnvironment = {
    val primary = Pio.getIntValue(parent, "primary", 0)
    val groups  = parent.getParamSets.asScala.toList.map { ps =>
      GuideGroup.fromParamSet(ps)
    }
    Initial.setOptions(groups.asImList).setPrimaryIndex(primary)
  }

  private def toSortedSet(s: Set[GuideProbe]): java.util.SortedSet[GuideProbe] =
    new java.util.TreeSet(GuideProbe.KeyComparator.instance) <|
      (_.addAll(s.asJavaCollection))

  implicit val EqualGuideEnvironment: Equal[GuideEnvironment] = Equal.equalBy(_.guideEnv)

  implicit val TargetCollectionGuideEnvironment: TargetCollection[GuideEnvironment] =
    TargetCollection.wrapping(Env)
}
