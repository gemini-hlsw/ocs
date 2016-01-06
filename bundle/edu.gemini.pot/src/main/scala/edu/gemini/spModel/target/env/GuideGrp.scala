package edu.gemini.spModel.target.env

import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPTarget

import scalaz._
import Scalaz._


sealed trait GuideGrp extends Serializable {
  /** Returns the set of guide probes with at least one associated guide star
    * in this environment.
    */
  def referencedGuiders: Set[GuideProbe] =
    this match {
      case AutomaticGroup.Initial    => Set.empty[GuideProbe]
      case AutomaticGroup.Active(ts) => ts.keySet
      case ManualGroup(_, ts)        => ts.keySet
    }

  /** Returns the set of guide probes with at least one associated primary
    * guide star in this environment.
    */
  def primaryReferencedGuiders: Set[GuideProbe] =
    this match {
      case AutomaticGroup.Initial    => Set.empty[GuideProbe]
      case AutomaticGroup.Active(ts) => ts.keySet
      case ManualGroup(_, ts)        => ts.filter(_._2.hasFocus).keySet
    }

  def containsTarget(t: SPTarget): Boolean =
    this match {
      case AutomaticGroup.Initial    => false
      case AutomaticGroup.Active(ts) => ts.exists { case (_, t0) => t == t0 }
      case ManualGroup(_, ts)        => ts.values.exists(opts => opts.any(_ == t))
    }

  def targets: Map[GuideProbe, List[SPTarget]] =
    this match {
      case AutomaticGroup.Initial    => Map.empty
      case AutomaticGroup.Active(ts) => ts.mapValues(_ :: Nil)
      case ManualGroup(_, ts)        => ts.mapValues(_.toList)
    }

  def removeTarget(t: SPTarget): GuideGrp =
    this match {
      case AutomaticGroup.Initial    =>
        this

      case AutomaticGroup.Active(ts) =>
        AutomaticGroup.Active(ts.filterNot(_._2 == t))

      case ManualGroup(n, ts)        =>
        ManualGroup(n, ts.mapValues(_.delete(t)).collect {
          case (probe, Some(opts)) => (probe, opts)
        })
    }

  def cloneTargets: GuideGrp =
    this match {
      case AutomaticGroup.Initial    =>
        this

      case AutomaticGroup.Active(ts) =>
        AutomaticGroup.Active(ts.mapValues(_.clone()))

      case ManualGroup(n, ts)        =>
        ManualGroup(n, ts.mapValues { opts =>
          OptsList(opts.toDisjunction.bimap(_.map(_.clone()), _.map(_.clone())))
        })
    }
}

/** A manual group has a name and a mapping from guide probe to a non-empty list
  * of targets, of which exactly one is selected. Should it also have a UUID so
  * we can distinguish [temporarily] identical ones?
 */
final case class ManualGroup(name: String, targetMap: Map[GuideProbe, OptsList[SPTarget]]) extends GuideGrp

object ManualGroup {
  val Name: ManualGroup @> String                                 =
    Lens.lensu((mg, n) => mg.copy(name = n), _.name)

  val TargetMap: ManualGroup @> Map[GuideProbe, OptsList[SPTarget]] =
    Lens.lensu((mg,m) => mg.copy(targetMap = m), _.targetMap)
}


sealed trait AutomaticGroup extends GuideGrp

object AutomaticGroup {

  /** The initial automatic group is a marker indicating that the target
    * environment is "new".
    */
  case object Initial extends AutomaticGroup

  /** An active bags group provides a 1:1 mapping from probe to target. If the
    * map is empty this is ok; it just means bags did not find any targets.
    */
  case class Active(targetMap: Map[GuideProbe, SPTarget]) extends AutomaticGroup

  val TargetMap: Active @> Map[GuideProbe, SPTarget] =
    Lens.lensu((a,m) => a.copy(targetMap = m), _.targetMap)
}

object GuideGrp {

  val Name: GuideGrp @?> String =
    PLens.plensgf({
      case mg: ManualGroup => n => mg.copy(name = n)
    }, {
      case mg: ManualGroup => mg.name
    })
}
