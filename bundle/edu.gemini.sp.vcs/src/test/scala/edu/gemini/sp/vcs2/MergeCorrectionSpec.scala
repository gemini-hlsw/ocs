package edu.gemini.sp.vcs2

import edu.gemini.pot.sp.{Conflict, Conflicts, SPNodeKey}
import edu.gemini.pot.sp.version._
import edu.gemini.sp.vcs2.ProgramLocation.{Remote, Local}
import edu.gemini.spModel.conflict.ConflictFolder
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.gemini.gmos.InstGmosSouth
import edu.gemini.spModel.gemini.obscomp.{SPSiteQuality, SPProgram}
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obslog.{ObsQaLog, ObsExecLog}
import edu.gemini.spModel.seqcomp.SeqBase
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.template.{TemplateGroup, TemplateFolder}
import edu.gemini.spModel.util.VersionToken
import edu.gemini.shared.util.immutable.ScalaConverters._

import org.specs2.matcher.{MatchResult, Expectable, Matcher}
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._

class MergeCorrectionSpec extends Specification {
  import NodeDetail._

  val LocalOnly: Set[ProgramLocation]  = Set(Local)
  val RemoteOnly: Set[ProgramLocation] = Set(Remote)
  val Both: Set[ProgramLocation]       = Set(Local, Remote)

  val lifespanId: LifespanId = LifespanId.random

  def mergeNode(dob: ISPDataObject, obsNum: Option[Int]): MergeNode = Modified(
    new SPNodeKey(),
    EmptyNodeVersions,
    dob,
    obsNum.fold(Empty: NodeDetail) { Obs.apply },
    Conflicts.EMPTY
  )

  def nonObs(dob: ISPDataObject): MergeNode = mergeNode(dob, None)

  def conflictFolder: MergeNode =
    addConflictNote(nonObs(new ConflictFolder), new Conflict.ConflictFolder(_))

  def prog: MergeNode = nonObs(new SPProgram)

  def templateFolder: MergeNode = nonObs(new TemplateFolder)

  def templateGroup(vt: VersionToken): MergeNode =
    new TemplateGroup() <| (_.setVersionToken(vt)) |> (tg => nonObs(tg))

  def obs(num: Int): MergeNode = mergeNode(new SPObservation, Some(num))

  def incr(mn: MergeNode): MergeNode =
    mn match {
      case m: Modified => m.copy(nv = m.nv.incr(lifespanId))
      case _           => failure("trying to increment unmodified node")
    }

  def addConflictNote(mn: MergeNode, cn: SPNodeKey => Conflict.Note): MergeNode =
    mn match {
      case m: Modified => m.copy(conflicts = m.conflicts.withConflictNote(cn(mn.key)))
      case _           => failure("trying to add a conflict note to an unmodified node")
    }

  def obsTree(num: Int): Tree[MergeNode] =
    obs(num).node(
      nonObs(new TargetObsComp).leaf,
      nonObs(new SPSiteQuality).leaf,
      nonObs(new InstGmosSouth).leaf,
      nonObs(new ObsQaLog).leaf,
      nonObs(new ObsExecLog).leaf,
      nonObs(new SeqBase).leaf
    )

  def plan(t: Tree[MergeNode]): MergePlan = MergePlan(t, Set.empty)

  def treeComparisonFailureMessage(msg: String, expected: Tree[MergeNode], actual: Tree[MergeNode]): String =
    s"$msg\n\nExpected:\n${expected.drawTree}\nActual:\n${actual.drawTree}"

  def correspondTo(expected: Tree[MergeNode]) = new Matcher[Tree[MergeNode]] {
    def compare(expected: Tree[MergeNode], actual: Tree[MergeNode]): Option[String] = {
      val e = expected.rootLabel
      val a = actual.rootLabel

      (e, a) match {
        case (Modified(ek, env, edob, edet, econ), Modified(ak, anv, adob, adet, acon)) =>
          val ect = edob.getType
          val act = adob.getType

          // conflict folders are created by the validity correction so we don't
          // know what key to expect ahead of time.  match any key for conflict
          // folders
          def key: Option[String] =
            ((ect != ConflictFolder.SP_TYPE) && (ek != ak)) option s"Keys don't match. Expected: $ek, Actual: $ak"

          def nodeVersion: Option[String] =
            env != anv option s"NodeVersions don't match for $ak. Expected: $env, Actual: $anv"

          def componentType: Option[String] =
            ect != act option s"Component type doesn't match for $ak. Expected: $ect, Actual: $act"

          def detail: Option[String] =
            edet != adet option s"NodeDetail doesn't match for $ak. Expected: $edet, Actual: $adet"

          def conflict: Option[String] = {
            def notes(c: Conflicts): List[Conflict.Note] =
              c.notes.asScalaList

            def folder(ns: List[Conflict.Note]): Option[Conflict.ConflictFolder] =
              ns.collectFirst { case cf: Conflict.ConflictFolder => cf }

            def set(ns: List[Conflict.Note]): Set[Conflict.Note] =
              ns.filter {
                case _: Conflict.ConflictFolder => false
                case _                          => true
              }.toSet

            val enotes  = notes(econ)
            val anotes  = notes(acon)

            // conflict folders are created by the corrections so we don't
            // know what key to expect ahead of time.
            val efolder = folder(enotes)
            val afolder = folder(anotes)

            val differs = (econ.dataObjectConflict.isDefined != acon.dataObjectConflict.isDefined) ||
                          (efolder.isDefined != afolder.isDefined) || (set(enotes) != set(anotes))

            differs option s"Conflicts don't match for $ak.  Expected: ${econ.shows}, Actual: ${acon.shows}"
          }

          def childKeys(t: Tree[MergeNode]): String =
            t.subForest.map(_.key).mkString("{", ", ", "}")

          def childrenSize: Option[String] =
            expected.subForest.size != actual.subForest.size option s"Children differ. Expected: ${childKeys(expected)}, Actual: ${childKeys(actual)}"

          def deepChildren: Option[String] =
            (Option.empty[String]/:expected.subForest.zip(actual.subForest)) { case (opt, (echild, achild)) =>
                opt orElse compare(echild, achild)
            }

          key orElse nodeVersion orElse componentType orElse detail orElse conflict orElse childrenSize orElse deepChildren

        case (eu: Unmodified, au: Unmodified) =>
          eu.key != au.key option s"Unmodified node keys don't match. Expected: ${eu.key}, Actual: ${au.key}"

        case _                                =>
          Some(s"Modified/Unmodified mix.  Expected: $e, Actual: $a")
      }
    }

    override def apply[S <: Tree[MergeNode]](t: Expectable[S]): MatchResult[S] = {
      val actual  = t.value
      val problem = compare(expected, actual).map(treeComparisonFailureMessage(_, expected, actual))
      result(problem.isEmpty, "", problem.getOrElse("unknown"), t)
    }
  }
}
