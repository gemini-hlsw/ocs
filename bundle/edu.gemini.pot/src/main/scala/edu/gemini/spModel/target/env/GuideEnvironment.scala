package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.{ImList, Option => GemOption}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.pio.{PioFactory, ParamSet}
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
      case m: ManualGroup =>
        (Env andThen GuideEnv.Manual).mod(_.flatMap { _.delete(m) }, this)
      case _              =>
        this
    }

  override def getTargets: ImList[SPTarget] =
    guideEnv.targets.toList.sortBy(_._1).flatMap(_._2.toList).asImList

  def getOptions: ImList[GuideGroup] =
    guideEnv.groups.map(GuideGroup).asImList

  // TODO: REFERENCE
  // TODO: only used by BagsManager so can be removed when BagsManager is updated
  def update(op: OptionsList.Op[GuideGroup]): GuideEnvironment =
    ???

  def setOptions(newList: ImList[GuideGroup]): GuideEnvironment = {
    // This is an awkward, wacky method that updates the available guide groups
    // and tries to keep the selected group the "same".  It predates the new
    // model and has no concept of a single, always-present automatic group.

    // The newList should contain exactly one automatic guide group, which
    // should be the first in the list.  The new model won't support anything
    // else.  We therefore will have to massage the list accordingly if it
    // does not conform.

    val empty    = (List.empty[AutomaticGroup], List.empty[ManualGroup])
    val (as, ms) = (newList.asScalaList:\empty) { case (gg, (as0, ms0)) =>
      gg.grp match {
        case a: AutomaticGroup => (a :: as0, ms0)
        case m: ManualGroup    => (as0, m :: ms0)
      }
    }

    val auto       = as.headOption.getOrElse(AutomaticGroup.Initial)
    val all        = auto :: ms
    val oldPrimary = guideEnv.primaryGroup
    val newPrimary = all.contains(oldPrimary) ? oldPrimary | {
      // Select the group at the same index, as in the old Java GuideEnvironment
      val i = guideEnv.primaryIndex
      all((i < all.length) ? i | all.length - 1)
    }

    val manual = ms.span(_ != newPrimary) match {
      case (Nil, Nil)               => None
      case (h :: t, Nil)            => Some(OptsList(NonEmptyList.nel(h, t).left))
      case (lefts, focus :: rights) => Some(OptsList(Zipper(lefts.reverse.toStream, focus, rights.toStream).right))
    }

    GuideEnvironment(GuideEnv(auto, manual))
  }

  // TODO: primary is always defined, remove the option wrapper
  def getPrimary: GemOption[GuideGroup] =
    ???

  // TODO: REFERENCE
  def setPrimary(primary: GuideGroup): GuideEnvironment =
    ???

  def mkString(prefix: String, sep: String, suffix: String): String =
    ???

  def getPrimaryIndex: java.lang.Integer =
    ???

  def setPrimaryIndex(primary: Int): GuideEnvironment =
    ???

  // TODO: REFERENCE
  def selectPrimary(primary: GuideGroup): GuideEnvironment =
    ???

  def iterator(): java.util.Iterator[GuideGroup] =
    ???

  // TODO: REFERENCE
  def putGuideProbeTargets(grp: GuideGroup, gpt: GuideProbeTargets): GuideEnvironment =
    ???

  def getParamSet(f: PioFactory): ParamSet =
    ???
}

object GuideEnvironment {
  val ParamSetName = "guideEnv"
  val Initial: GuideEnvironment = GuideEnvironment(GuideEnv.initial)

  val Env: GuideEnvironment @> GuideEnv =
    Lens.lensu((a,b) => a.copy(b), _.guideEnv)

  def create(guideGroups: OptionsList[GuideGroup]): GuideEnvironment =
    ???

  def fromParamSet(parent: ParamSet): GuideEnvironment =
    ???

  private def toSortedSet(s: Set[GuideProbe]): java.util.SortedSet[GuideProbe] =
    new java.util.TreeSet(GuideProbe.KeyComparator.instance) <|
      (_.addAll(s.asJavaCollection))
}
