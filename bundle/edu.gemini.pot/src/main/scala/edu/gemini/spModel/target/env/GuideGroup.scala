package edu.gemini.spModel.target.env

import edu.gemini.spModel.target.env.AutomaticGroup.{Active, Initial}
import edu.gemini.shared.util.immutable.{Option => GemOption, ImOption, ImList}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.guide.GuideProbe
//import edu.gemini.spModel.guide.OrderGuideGroup
import edu.gemini.spModel.pio.{PioFactory, ParamSet}
import edu.gemini.spModel.target.SPTarget

import scala.annotation.varargs
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/** The old Java codebase compatible wrapper around the model concept of a
  * guide group.
  */
case class GuideGroup(grp: GuideGrp) extends java.lang.Iterable[GuideProbeTargets] with TargetContainer {

  // getName / setName are a bit wacky but the API is being kept compatible
  // with the old Java-based GuideGroup

  /**
   * Gets the name if this is a manual guide group, otherwise None.
   */
  def getName: GemOption[String] =
    GuideGroup.Name.get(this).asGeminiOpt

  /**
   * Sets the name (if defined) and returns the updated group if this is a
   * manual guide group, otherwise returns this group.
   */
  def setName(name: GemOption[String]): GuideGroup =
    name.asScalaOpt.fold(this)(GuideGroup.Name.setOr(this, _, this))

  /**
   * Sets the name and returns the updated group if this is a manual guide
   * group, otherwise returns this group.
   */
  def setName(n: String): GuideGroup =
    setName(ImOption.apply(n))

  /** Returns `true` if the group contains a target associated with the given
    * guide probe; `false` otherwise.
    */
  def contains(gp: GuideProbe): Boolean =
    grp match {
      case AutomaticGroup.Initial    => false
      case AutomaticGroup.Active(ts) => ts.contains(gp)
      case ManualGroup(_, ts)        => ts.contains(gp)
    }

  private def gpt(gp: GuideProbe): Option[GuideProbeTargets] = {
    val gpt = grp match {
      case ManualGroup(_, opts) =>
        opts.get(gp).map { o => (o.focus, o.toList) }

      case Active(ts)         =>
        ts.get(gp).map { t => (some(t), List(t)) }

      case Initial            =>
        None
    }

    gpt.map { case (primary, all) =>
      GuideProbeTargets.create(gp, primary.asGeminiOpt, all.asImList)
    }
  }

  /** Constructs a `GuideProbeTargets` structure to describe the guide stars
    * associated with the given guider, if any.
    */
  def get(gp: GuideProbe): GemOption[GuideProbeTargets] =
    gpt(gp).asGeminiOpt

  private def update(f: GuideGrp => GuideGrp): GuideGroup =
    GuideGroup(f(grp))

  /** Sets the guide stars associated with a guider according to the given
    * `GuideProbeTargets` and returns the updated `GuideGroup`.  The updates
    * performed depend on the type of group. (WARNING: wacky behavior such here
    * such as a put followed by a get not returning the same
    * `GuideProbeTargets` in all cases.)
    *
    *
    * <ul>
    *   <li>
    *     manual - if the given `GuideProbeTargets` is empty, then all guide
    *     stars associated with its guider are removed in the new manual group
    *     that is returned.  Otherwise, a new manual group is created to match
    *     the `GuideProbeTargets` stars.  If there is no primary guide star in
    *     the `GuideProbeTargets`, then there will be no primary for the guider
    *     in the new manual group.
    *   </li>
    *   <li>
    *     auto / initial - if there is no primary in the given
    *     `GuideProbeTargets`, then this group is just returned (i.e., noop).
    *     Otherwise, a new auto / active group is created and given the primary
    *     guide star as its only guide star.  Any non-primary guide stars in
    *     `GuideProbeTargets` are ignored.
    *   </li>
    *   <li>
    *     auto / active - if there is no primary then a new auto / active group
    *     is returned without any guide stars for the associated guider.  If
    *     there is a primary, a new auto / active group is returned with a
    *     mapping from the guider to the primary star.  Any non-primary guide
    *     stars in `GuideProbeTargets` are ignored.
    *   </li>
    * </ul>
    */
  def put(gpt: GuideProbeTargets): GuideGroup = {
    val probe   = gpt.getGuider
    val primary = gpt.getPrimary.asScalaOpt

    update {
      case mg@ManualGroup(_, m) =>
        val targets = gpt.getManualTargets.asScalaList.toNel

        targets.fold(mg.copy(targetMap = m - probe)) { nel =>
          def noPrimary = OptsList(nel.left[Zipper[SPTarget]])

          val opts = primary.fold(noPrimary) { t =>
            val (lefts, focusRight) = nel.toList.span(_ != t)
            focusRight.headOption.fold(noPrimary) { _ =>
              OptsList(Zipper(lefts.toStream, t, focusRight.drop(1).toStream).right)
            }
          }
          mg.copy(targetMap = m + (probe -> opts))
        }

      case a@Active(ts)         =>
        a.copy(targetMap = primary.fold(ts - probe) { t => ts + (probe -> t)})

      case Initial              =>
        primary.fold(grp) { t => Active(Map(probe -> t)) }
    }
  }

  def remove(probe: GuideProbe): GuideGroup =
    update {
      case Initial         => Initial
      case a: Active       => a.copy(targetMap = a.targetMap - probe)
      case mg: ManualGroup => mg.copy(targetMap = mg.targetMap - probe)
    }

  def clear(): GuideGroup =
    update {
      case Initial         => Initial
      case a: Active       => a.copy(targetMap = Map.empty)
      case mg: ManualGroup => mg.copy(targetMap = Map.empty)
    }

  private def sortedKeys: List[GuideProbe] =
    (grp match {
      case Initial            => Set.empty[GuideProbe]
      case Active(ts)         => ts.keySet
      case ManualGroup(_, ts) => ts.keySet
    }).toList.sorted

  private def all: List[GuideProbeTargets] =
    sortedKeys.flatMap(gpt)

  /** Gets a list of `GuideProbeTargets` sorted by `GuideProbe` key. */
  def getAll: ImList[GuideProbeTargets] =
    all.asImList

  /** Puts all the given `GuideProbeTargets` replacing any associated with the
    * same `GuideProbe` but leaving unrelated `GuideProbes` alone.  This is the
    * same as calling `put` for each `GuideProbeTargets` in turn.
    */
  def putAll(ts: ImList[GuideProbeTargets]): GuideGroup =
    (this/:ts.asScalaList) { (gg, cur) => gg.put(cur) }

  /** Creates a new group consisting of only the given `GuideProbeTargets`.
    * Any existing `GuideProbeTargets` for unrelated `GuideProbe`s are removed.
    */
  def setAll(ts: ImList[GuideProbeTargets]): GuideGroup =
    clear().putAll(ts)

  /** Obtains an iterator that steps through the contained `GuideProbeTargets`
    * in order of their `GuideProbe` key.
    */
  override def iterator: java.util.Iterator[GuideProbeTargets] =
    getAll.toList.iterator()

  def getAllContaining(t: SPTarget): ImList[GuideProbeTargets] =
    all.filter(_.containsTarget(t)).asImList

  def getAllMatching(t: GuideProbe.Type): ImList[GuideProbeTargets] =
    all.filter(_.getGuider.getType == t).asImList

  private def guiderSet(probes: Set[GuideProbe]): java.util.SortedSet[GuideProbe] =
    new java.util.TreeSet(GuideProbe.KeyComparator.instance) <| (_.addAll(probes.asJava))

  def getReferencedGuiders: java.util.SortedSet[GuideProbe] =
    guiderSet(grp.referencedGuiders)

  def getPrimaryReferencedGuiders: java.util.SortedSet[GuideProbe] =
    guiderSet(grp.primaryReferencedGuiders)

  override def containsTarget(t: SPTarget): Boolean =
    grp.containsTarget(t)

  /** Gets a list of SPTarget sorted by their associated GuideProbe. */
  override def getTargets: ImList[SPTarget] =
    grp.targets.toList.sortBy(_._1).flatMap(_._2).asImList

  override def removeTarget(t: SPTarget): GuideGroup =
    update { _.removeTarget(t) }

  override def cloneTargets: GuideGroup =
    update { _.cloneTargets }

  def iterateAllTargets: java.util.Iterator[SPTarget] = ???

  def getParamSet(f: PioFactory): ParamSet = ???
}

object GuideGroup extends Function1[GuideGrp, GuideGroup] {
  val EMPTY = GuideGroup(ManualGroup("Group", Map.empty))

  val Grp: GuideGroup @> GuideGrp =
    Lens.lensu((jGrp, sGrp) => jGrp.copy(grp = sGrp), _.grp)

  val Name: GuideGroup @?> String =
    Grp.partial >=> GuideGrp.Name

  @varargs
  def create(name: String, targets: GuideProbeTargets*): GuideGroup = ???

  def create(name: String, targets: ImList[GuideProbeTargets]): GuideGroup = ???

  def create(name: GemOption[String], targets: ImList[GuideProbeTargets]): GuideGroup = ???

  def fromParamSet(parent: ParamSet): GuideGroup = ???
}
