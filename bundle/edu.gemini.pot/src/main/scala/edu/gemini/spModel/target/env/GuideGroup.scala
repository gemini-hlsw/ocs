package edu.gemini.spModel.target.env

import edu.gemini.spModel.core.Angle
import edu.gemini.spModel.target.env.AutomaticGroup.{Active, Disabled, Initial}
import edu.gemini.spModel.target.env.GuideGroup.AutoDisabledTag
import edu.gemini.spModel.target.env.TargetCollection._
import edu.gemini.shared.util.immutable.{Option => GemOption, ImOption, ImList}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.guide.GuideProbe
import edu.gemini.spModel.pio.{Pio, PioFactory, ParamSet}
import edu.gemini.spModel.target.SPTarget

import scala.annotation.varargs
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/** The old Java codebase compatible wrapper around the model concept of a
  * guide group.  See `GuideGrp`.
  */
case class GuideGroup(grp: GuideGrp) extends java.lang.Iterable[GuideProbeTargets] with TargetContainer {

  // getName / setName are a bit wacky but the API is being kept compatible
  // with the old Java-based GuideGroup

  /**
   * Gets the name if this is a manual guide group, otherwise None.
   */
  def getName: GemOption[String] =
    GuideGroup.name.get(this).asGeminiOpt

  /**
   * Sets the name (if defined) and returns the updated group if this is a
   * manual guide group, otherwise returns this group.
   */
  def setName(name: GemOption[String]): GuideGroup =
    GuideGroup.name.setOr(this, name.getOrElse(""), this)

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
      case Initial | Disabled => false
      case Active(ts, _)      => ts.member(gp)
      case ManualGroup(_, ts) => ts.member(gp)
    }

  private def gpt(gp: GuideProbe): Option[GuideProbeTargets] = {
    val gpt = grp match {
      case ManualGroup(_, opts) =>
        opts.lookup(gp).map { o => (o.focus, o.toList) }

      case Active(ts, _)        =>
        ts.lookup(gp).map { t => (some(t), List(t)) }

      case Initial | Disabled   =>
        None
    }

    gpt.map { case (primary, all) =>
      GuideProbeTargets.create(gp, all.asImList).selectPrimary(primary.asGeminiOpt)
    }
  }

  /** Constructs a `GuideProbeTargets` structure to describe the guide stars
    * associated with the given guider, if any.
    */
  def get(gp: GuideProbe): GemOption[GuideProbeTargets] =
    gpt(gp).asGeminiOpt

  private def mod(f: GuideGrp => GuideGrp): GuideGroup =
    GuideGroup.grp.mod(f, this)

  /** Sets the guide stars associated with a guider according to the given
    * `GuideProbeTargets` and returns the updated `GuideGroup`.  The updates
    * performed depend on the type of group. (WARNING: wacky behavior here
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

    mod {
      case mg@ManualGroup(_, m) =>
        val targets = gpt.getOptions.asScalaList.toNel

        targets.fold(mg.copy(targetMap = m - probe)) { nel =>
          def noPrimary = OptsList.unfocused(nel)

          val opts = primary.fold(noPrimary) { t =>
            val (lefts, focusRight) = nel.toList.span(_ =/= t)
            focusRight.headOption.fold(noPrimary) { _ =>
              OptsList.focused(lefts, t, focusRight.drop(1))
            }
          }
          mg.copy(targetMap = m + (probe -> opts))
        }

      case a@Active(ts, _)      =>
        a.copy(targetMap = primary.fold(ts - probe) { t => ts + (probe -> t)})

      case Initial | Disabled   =>
        // It isn't really cool to just stick in 0 degrees for the position
        // angle here but the "put" method is required by the old
        // GuideEnvironment API and should only be used for manual guide groups.
        // BAGS will be updating the automatic group via the underlying Scala
        // GuideGrp and will supply the correct position angle.
        primary.fold(grp) { t => Active(==>>.singleton(probe, t), Angle.zero) }
    }
  }

  def remove(probe: GuideProbe): GuideGroup =
    mod {
      case Initial         => Initial
      case Disabled        => Disabled
      case a: Active       => a.copy(targetMap = a.targetMap - probe)
      case mg: ManualGroup => mg.copy(targetMap = mg.targetMap - probe)
    }

  def clear(): GuideGroup =
    mod {
      case Initial         => Initial
      case Disabled        => Disabled
      case a: Active       => a.copy(targetMap  = ==>>.empty)
      case mg: ManualGroup => mg.copy(targetMap = ==>>.empty)
    }

  private def sortedKeys: List[GuideProbe] =
    (grp match {
      case Initial | Disabled => Set.empty[GuideProbe]
      case Active(ts, _)      => ts.keySet // already sorted
      case ManualGroup(_, ts) => ts.keySet // already sorted
    }).toList

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
    (this/:ts.asScalaList) { _ put _ }

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
    grp match {
      case a: AutomaticGroup => a.containsTarget(t)
      case m: ManualGroup    => m.containsTarget(t)
    }

  /** Gets a list of SPTarget sorted by their associated `GuideProbe`. */
  override def getTargets: ImList[SPTarget] = {
    val m = grp match {
      case a: AutomaticGroup => a.targets
      case m: ManualGroup    => m.targets
    }
    m.values.flatMap(_.toList).asImList
  }

  override def removeTarget(t: SPTarget): GuideGroup =
    mod {
      case a: AutomaticGroup => a.removeTarget(t)
      case m: ManualGroup    => m.removeTarget(t)
    }

  override def cloneTargets: GuideGroup =
    mod {
      case a: AutomaticGroup => a.cloneTargets
      case m: ManualGroup    => m.cloneTargets
    }

  /** Gets a Java `Iterator` that can step through targets sorted by their
    * associated `GuideProbe`.
    */
  def iterateAllTargets: java.util.Iterator[SPTarget] =
    getTargets.toList.iterator

  def getParamSet(f: PioFactory): ParamSet = {
    import GuideGroup.{grp => _, _}

    val ps = f.createParamSet(ParamSetName)

    getName.asScalaOpt.foreach { Pio.addParam(f, ps, NameParam, _) }

    val (tag, posAngle) = grp match {
      case Initial           => (AutoInitialTag, None)
      case Disabled          => (AutoDisabledTag, None)
      case Active(_, pa)     => (AutoActiveTag, Some(pa))
      case ManualGroup(_, _) => (ManualTag, None)
    }
    Pio.addParam(f, ps, TagParam, tag.toString)

    posAngle.foreach { pa =>
      Pio.addDoubleParam(f, ps, PosAngleParam, pa.toDegrees)
    }

    all.foreach { gpt => ps.addParamSet(gpt.getParamSet(f)) }

    ps
  }

  def isAutomatic: Boolean =
    grp.isAutomatic

  def isManual: Boolean =
    grp.isManual

  def toManualGroup: GuideGroup =
    GuideGroup(grp.cloneTargets.toManualGroup)
}

object GuideGroup extends ((GuideGrp) => GuideGroup) {
  /** An initial automatic `GuideGroup`. */
  val AutomaticInitial  = GuideGroup(Initial)
  val AutomaticDisabled = GuideGroup(Disabled)

  /** An empty manual group. */
  val ManualEmpty      = GuideGroup(ManualGroup("Manual Group", ==>>.empty))

  val grp: GuideGroup @> GuideGrp =
    Lens.lensu((jGrp, sGrp) => jGrp.copy(grp = sGrp), _.grp)

  val name: GuideGroup @?> String =
    grp.partial >=> GuideGrp.name

  val ParamSetName  = "guideGroup"
  val NameParam     = "name"
  val TagParam      = "tag"
  val PosAngleParam = "posAngle"

  sealed trait GroupTypeTag
  case object AutoInitialTag  extends GroupTypeTag
  case object AutoDisabledTag extends GroupTypeTag
  case object AutoActiveTag   extends GroupTypeTag
  case object ManualTag       extends GroupTypeTag

  private val AllTags = List(AutoInitialTag, AutoDisabledTag, AutoActiveTag, ManualTag)

  def fromParamSet(ps: ParamSet): GuideGroup = {
    val name    = Pio.getValue(ps, NameParam, "Manual Group")
    val targets = ps.getParamSets.asScala.toList.map { GuideProbeTargets.fromParamSet }.asImList

    val typeTag = for {
      s <- Option(Pio.getValue(ps, TagParam))
      t <- AllTags.find(_.toString == s)
    } yield t

    typeTag.fold(createManual(name, targets)) {
      case AutoInitialTag  =>
        AutomaticInitial

      case AutoDisabledTag =>
        AutomaticDisabled

      case AutoActiveTag   =>
        val posAngle = Angle.fromDegrees(Pio.getDoubleValue(ps, PosAngleParam, 0.0))
        createActive(targets, posAngle)

      case ManualTag       =>
        createManual(name, targets)
    }
  }

  @varargs
  def create(name: String, targets: GuideProbeTargets*): GuideGroup =
    createManual(name, targets.toList.asImList)

  def create(name: String, targets: ImList[GuideProbeTargets]): GuideGroup =
    createManual(name, targets)

  def create(name: GemOption[String], targets: ImList[GuideProbeTargets]): GuideGroup =
    createManual(name.getOrElse("Manual Group"), targets)

  def createManual(name: String, targets: ImList[GuideProbeTargets]): GuideGroup = {
    val m = ==>>.fromList(targets.asScalaList.flatMap { gpt =>
      val lst    = gpt.getTargets.asScalaList
      val zipOpt = gpt.getPrimaryIndex.asScalaOpt.map(i => lst.splitAt(i)).collect {
        case (lefts, focus :: rights) => OptsList.focused(lefts, focus, rights)
      }
      zipOpt.orElse(lst.toNel.map(nel => OptsList.unfocused(nel))).strengthL(gpt.getGuider)
    })

    GuideGroup(ManualGroup(name, m))
  }

  def createActive(targets: ImList[GuideProbeTargets], posAngle: Angle): GuideGroup = {
    val m = ==>>.fromList(targets.asScalaList.flatMap { gpt =>
      gpt.getPrimary.asScalaOpt.strengthL(gpt.getGuider)
    })

    GuideGroup(Active(m, posAngle))
  }

  implicit val EqualGuideGroup: Equal[GuideGroup] = Equal.equalBy(_.grp)

  implicit val TargetCollectionGuideGroup: TargetCollection[GuideGroup] =
    TargetCollection.wrapping(grp)
}
