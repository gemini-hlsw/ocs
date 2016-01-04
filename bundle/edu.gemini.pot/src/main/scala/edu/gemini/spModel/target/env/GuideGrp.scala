package edu.gemini.spModel.target.env

import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.target.SPTarget

import scalaz._
import Scalaz._


sealed trait GuideGrp extends Serializable {
  def containsTarget(t: SPTarget): Boolean = this match {
    case ManualGroup(_, ts)        => ts.values.exists(opts => opts.any(_ == t))
    case AutomaticGroup.Initial    => false
    case AutomaticGroup.Active(ts) => ts.exists { case (_, t0) => t == t0 }
  }

  def referencedGuiders: Set[GuideProbe] = this match {
    case ManualGroup(_, ts)        => ts.keySet
    case AutomaticGroup.Initial    => Set.empty[GuideProbe]
    case AutomaticGroup.Active(ts) => ts.keySet
  }

  def targets: List[SPTarget] = this match {
    case ManualGroup(_, ts)        => ts.values.toList.flatMap(_.toList)
    case AutomaticGroup.Initial    => Nil
    case AutomaticGroup.Active(ts) => ts.values.toList
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
