package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.{ImList, Option => GemOption}
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.pio.{PioFactory, ParamSet}
import edu.gemini.spModel.target.SPTarget

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

import GuideEnvironment._

/**
 *
 */
final case class GuideEnvironment(guideEnv: GuideEnv) extends TargetContainer with OptionsList[GuideGroup] {

  def getReferencedGuiders: java.util.SortedSet[GuideProbe] =
    toSortedSet(guideEnv.referencedGuiders)

  def getPrimaryReferencedGuiders: java.util.SortedSet[GuideProbe] =
    toSortedSet(guideEnv.primaryReferencedGuiders)

  override def containsTarget(target: SPTarget): Boolean =
    guideEnv.groups.exists { _.containsTarget(target) }

  /** Removes the target from any manual groups in which it is found. */
  override def removeTarget(target: SPTarget): GuideEnvironment = {
    /*
    def rmFromZipper(z: Zipper[SPTarget]): Option[Zipper[SPTarget]] =
      if (target == z.focus) z.deleteRight
      else z.findZ(_ == target).fold(some(z)) {
        _.delete.fold(some(z)) { _.findZ(_ == z.focus) }
      }

    def rmFromGroup(m: ManualGroup): ManualGroup = {
      val m0 = (Map.empty[GuideProbe, Zipper[SPTarget]]/:m.targetMap) { case (res, (gp, zip)) =>
        rmFromZipper(zip).fold(res)(z0 => res + (gp -> z0))
      }
      m.copy(targetMap = m0)
    }

    val manual0 = guideEnv.manual.bimap(_.map(rmFromGroup), _.map(rmFromGroup))
    GuideEnvironment(guideEnv.copy(manual = manual0))
    */
    ???
  }

  def removeGroup(grp: GuideGroup): GuideEnvironment =
    ???

  override def cloneTargets(): GuideEnvironment =
    ???

  override def getTargets: ImList[SPTarget] =
    ???

  override def getOptions: ImList[GuideGroup] =
    ???

  override def update(primaryIndex: GemOption[Integer], list: ImList[GuideGroup]): GuideEnvironment =
    ???

  override def update(op: OptionsList.Op[GuideGroup]): GuideEnvironment =
    ???

  override def setOptions(newList: ImList[GuideGroup]): GuideEnvironment =
    ???

  override def getPrimary: GemOption[GuideGroup] =
    ???

  override def setPrimary(primary: GuideGroup): GuideEnvironment =
    ???

  override def mkString(prefix: String, sep: String, suffix: String): String =
    ???

  override def getPrimaryIndex: GemOption[java.lang.Integer] =
    ???

  override def setPrimaryIndex(primary: GemOption[Integer]): GuideEnvironment =
    ???

  override def setPrimaryIndex(primary: Int): GuideEnvironment =
    ???

  override def selectPrimary(primary: GemOption[GuideGroup]): GuideEnvironment =
    ???

  override def selectPrimary(primary: GuideGroup): GuideEnvironment =
    ???

  override def iterator(): java.util.Iterator[GuideGroup] =
    ???

  def putGuideProbeTargets(grp: GuideGroup, gtp: GuideProbeTargets): GuideEnvironment =
    ???

  def getParamSet(f: PioFactory): ParamSet =
    ???
}

object GuideEnvironment {
  val ParamSetName = "guideEnv"
  val Initial: GuideEnvironment = GuideEnvironment(GuideEnv.initial)

  def create(guideGroups: OptionsList[GuideGroup]): GuideEnvironment =
    ???

  def fromParamSet(parent: ParamSet): GuideEnvironment =
    ???

  private def toSortedSet(s: Set[GuideProbe]): java.util.SortedSet[GuideProbe] =
    new java.util.TreeSet(GuideProbe.KeyComparator.instance) <|
      (_.addAll(s.asJavaCollection))
}
