package edu.gemini.spModel.target.env

import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.guide.OrderGuideGroup
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.EqualSPTarget
import edu.gemini.spModel.target.SPTarget

import scalaz._
import Scalaz._

import AutomaticGroup.{Active, Initial, Disabled}

/** A map of guide probes and their associated guide star options.  There are
  * two main categories of guide groups, automatically vs. manually maintained.
  */
sealed trait GuideGrp extends Serializable {

  def isAutomatic: Boolean

  def isManual: Boolean = !isAutomatic

  def toManualGroup: ManualGroup

  /** Returns the set of guide probes with at least one associated guide star
    * in this environment.
    */
  def referencedGuiders: Set[GuideProbe] =
    this match {
      case Initial | Disabled  => Set.empty[GuideProbe]
      case Active(ts, _)       => ts.keySet.toSet
      case ManualGroup(_, ts)  => ts.keySet.toSet
    }

  /** Returns the set of guide probes with at least one associated primary
    * guide star in this environment.
    */
  def primaryReferencedGuiders: Set[GuideProbe] =
    this match {
      case Initial | Disabled => Set.empty[GuideProbe]
      case Active(ts, _)      => ts.keySet.toSet
      case ManualGroup(_, ts) => ts.filter(_.hasFocus).keySet.toSet
    }
}

/** A manual group has a name and a mapping from guide probe to a non-empty list
  * of targets, of which exactly one may be selected.
 */
final case class ManualGroup(name: String, targetMap: GuideProbe ==>> OptsList[SPTarget]) extends GuideGrp {
  def isAutomatic: Boolean       = false
  def toManualGroup: ManualGroup = this
}

object ManualGroup {
  val Empty: ManualGroup = ManualGroup("", ==>>.empty)

  val Name: ManualGroup @> String                                 =
    Lens.lensu((mg, n) => mg.copy(name = n), _.name)

  val TargetMap: ManualGroup @> ==>>[GuideProbe, OptsList[SPTarget]] =
    Lens.lensu((mg,m) => mg.copy(targetMap = m), _.targetMap)

  implicit val TargetCollectionManualGroup: TargetCollection[ManualGroup] = new TargetCollection[ManualGroup] {
    override def cloneTargets(m: ManualGroup): ManualGroup =
      TargetMap.mod(_.map { opts =>
        OptsList(opts.toDisjunction.bimap(_.map(_.clone()), _.map(_.clone())))
      }, m)

    override def containsTarget(m: ManualGroup, t: SPTarget): Boolean =
      m.targetMap.values.exists(opts => opts.any(_ == t))

    override def removeTarget(m: ManualGroup, t: SPTarget): ManualGroup =
      TargetMap.mod(_.mapOption(_.delete(t)), m)

    override def targets(m: ManualGroup): GuideProbe ==>> NonEmptyList[SPTarget] =
      m.targetMap.map(_.toNel)
  }

  implicit val EqualManualGroup: Equal[ManualGroup] = Equal.equal { (m0, m1) =>
    (m0.name === m1.name) && (m0.targetMap === m1.targetMap)
  }
}

/** Automatic groups map guide probes to a single guide star. */
sealed trait AutomaticGroup extends GuideGrp {
  def isAutomatic: Boolean = true

  def toManualGroup: ManualGroup =
    ManualGroup.Empty

  def targetMap: GuideProbe ==>> SPTarget =
    ==>>.empty
}

object AutomaticGroup {

  /** The initial automatic group is a marker indicating that the target
    * environment is "new".
    */
  case object Initial extends AutomaticGroup

  /** A BAGS group that indicates that automatic guide stars should not be
    * sought. This is used in particular for pre-2016B observations.
    */
  case object Disabled extends AutomaticGroup

  /** An active BAGS group provides a 1:1 mapping from probe to target. If the
    * map is empty this is ok; it just means bags did not find any targets.
    */
  case class Active(override val targetMap: GuideProbe ==>> SPTarget, posAngle: Angle) extends AutomaticGroup {
    override def toManualGroup: ManualGroup =
      ManualGroup("", targetMap.map(t => OptsList.focused(t)))
  }

  val targetMap: Active @> ==>>[GuideProbe, SPTarget] =
    Lens.lensu((a,m) => a.copy(targetMap = m), _.targetMap)

  implicit val TargetCollectionAutomaticGroup: TargetCollection[AutomaticGroup] = new TargetCollection[AutomaticGroup] {
    override def cloneTargets(a: AutomaticGroup): AutomaticGroup =
      a match {
        case Initial | Disabled => a
        case Active(ts, pa)     => Active(ts.map(_.clone()), pa)
      }

    override def containsTarget(a: AutomaticGroup, t: SPTarget): Boolean =
      a match {
        case Initial | Disabled  => false
        case Active(ts, _)       => ts.any { _ == t }
      }

    override def removeTarget(a: AutomaticGroup, t: SPTarget): AutomaticGroup =
      a match {
        case Active(m, pa) => Active(m.filter(_ != t), pa)
        case _             => a
      }

    override def targets(a: AutomaticGroup): GuideProbe ==>> NonEmptyList[SPTarget] =
      a match {
        case Initial | Disabled => ==>>.empty
        case Active(m, _)       => m.map(_.wrapNel)
      }
  }

  implicit val EqualAutomaticGroup: Equal[AutomaticGroup] = Equal.equalA
}

object GuideGrp {

  val name: GuideGrp @?> String =
    PLens.plensgf({
      case mg: ManualGroup => n => mg.copy(name = n)
    }, {
      case mg: ManualGroup => mg.name
    })

  implicit val EqualGuideGrp: Equal[GuideGrp] = Equal.equal {
    case (a0: AutomaticGroup, a1: AutomaticGroup) => a0 === a1
    case (m0: ManualGroup,    m1: ManualGroup)    => m0 === m1
    case (_, _)                                   => false
  }

  implicit val TargetCollectionGuideGroup = new TargetCollection[GuideGrp] {
    import TargetCollection.TargetCollectionSyntax

    override def cloneTargets(g: GuideGrp): GuideGrp =
      g match {
        case a: AutomaticGroup => a.cloneTargets
        case m: ManualGroup    => m.cloneTargets
      }

    override def containsTarget(g: GuideGrp, t: SPTarget): Boolean =
      g match {
        case a: AutomaticGroup => a.containsTarget(t)
        case m: ManualGroup    => m.containsTarget(t)
      }

    override def removeTarget(g: GuideGrp, t: SPTarget): GuideGrp =
      g match {
        case a: AutomaticGroup => a.removeTarget(t)
        case m: ManualGroup    => m.removeTarget(t)
      }

    override def targets(g: GuideGrp): GuideProbe ==>> NonEmptyList[SPTarget] =
      g match {
        case a: AutomaticGroup => a.targets
        case m: ManualGroup    => m.targets
      }
  }
}
