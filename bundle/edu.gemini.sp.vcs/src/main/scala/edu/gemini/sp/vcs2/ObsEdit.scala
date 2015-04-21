package edu.gemini.sp.vcs2

import edu.gemini.pot.sp._
import edu.gemini.pot.sp.version._
import edu.gemini.shared.util.VersionComparison
import edu.gemini.shared.util.VersionComparison.{Newer, Conflicting}
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.rich.pot.sp._

import scala.collection.breakOut
import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

/**
 * ObsEdit describes a change to a local observation vs. the remote version
 * (if any).  There are three types of edits, creation of new local
 * observations, deletion of existing remote observations, or modifications to
 * observations that exist or used to exist in the remote database.
 */
sealed trait ObsEdit {
  def key: SPNodeKey
}

object ObsEdit {
  case class Obs(status: ObservationStatus, tree: Tree[MergeNode])

  /**
   * VersionComparison for the local version of an observation vs it remote
   * version.  Non-log nodes are considered apart from the observing log nodes
   * because edits have different security check implications.
   *
   * @param obsOnly comparison of all parts of the observation except the log
   * @param logOnly comparison of just the observing log
   */
  case class Comparison(obsOnly: VersionComparison, logOnly: VersionComparison) {
    def combined: VersionComparison = obsOnly |+| logOnly
  }

  case class ObsCreate(key: SPNodeKey, local: Obs) extends ObsEdit
  case class ObsUpdate(key: SPNodeKey, local: Obs, remote: Option[Obs], comparison: Comparison) extends ObsEdit
  case class ObsDelete(key: SPNodeKey, remote: Obs) extends ObsEdit

  implicit val ObsEditShow = Show.shows[ObsEdit] {
    case ObsCreate(_, Obs(status, t))                 =>
      s"ObsCreate ($status)\n${t.drawTree}"

    case ObsDelete(_, Obs(status, t))                 =>
      s"ObsDelete ($status)\n${t.drawTree}"

    case ObsUpdate(_, Obs(lStatus, lt), remote, comp) =>
      s"""ObsUpdate: local obs is ${comp.obsOnly}, log is ${comp.logOnly}
      #
      #* Local ($lStatus)
      #${lt.drawTree}
      #* Remote ${remote.fold("deleted")(r => s"(${r.status})\n${r.tree.drawTree}")}
      """.stripMargin('#')
  }

  /**
   * Extracts all the observation edits that have been made to observations in
   * the local program.
   */
  def all(local: ISPProgram, diff: ProgramDiff): TryVcs[List[ObsEdit]] = {
    val localObsMap = new ObservationIterator(local).asScala.map { o => o.key -> o }.toMap

    def localObs(k: SPNodeKey): Obs = {
      val lo = localObsMap(k)
      Obs(ObservationStatus.computeFor(lo), MergeNode.modifiedTree(lo))
    }

    val remoteVm     = diff.plan.vm(local)
    val remoteStatus = diff.obsStatus.toMap.lift
    val remoteObsMap = diff.plan.update.foldObservations(Map.empty[SPNodeKey, Tree[MergeNode]]) {
      (mod, _, children, m) => m + (mod.key -> Tree.node(mod, children))
    }

    def remoteObs(k: SPNodeKey, s: ObservationStatus): Obs =
      Obs(s, remoteObsMap(k))

    def mapRemoteStatus(k: SPNodeKey)(f: ObservationStatus => ObsEdit): TryVcs[ObsEdit] =
      remoteStatus(k).fold(TryVcs.fail[ObsEdit](s"Missing remote status for observation $k")) { stat =>
        f(stat).right
      }

    def compare(lo: ISPObservation): Comparison = {
      val allVm   = subVersionMap(lo)
      val logKeys = Option(lo.getObsQaLog).map(_.key).toList ++ Option(lo.getObsExecLog).map(_.key).toList

      val obsOnlyVm = (allVm/:logKeys) { (vm,k) => vm - k }
      val logOnlyVm = logKeys.map(k => k -> allVm.getOrElse(k, EmptyNodeVersions)).toMap

      def compareVm(vm: VersionMap): VersionComparison =
        vm.toList.foldMap { case (k, localNv) =>
          val remoteNv = remoteVm.getOrElse(k, EmptyNodeVersions)
          localNv.compare(remoteNv)
        }

      Comparison(compareVm(obsOnlyVm), compareVm(logOnlyVm))
    }

    // Common edited observations.  Note some of these will only have been
    // edited remotely, but they will be filtered out before including them
    // in the return value.
    val commonKeys = remoteObsMap.keySet & localObsMap.keySet
    val common: List[TryVcs[ObsEdit]] = commonKeys.map { k =>
      mapRemoteStatus(k) { stat =>
        ObsUpdate(k, localObs(k), Some(remoteObs(k, stat)), compare(localObsMap(k)))
      }
    }(breakOut)

    // Local only: new created observations or else remotely deleted.  Again,
    // this may include local observations that haven't been edited.
    val localKeys = localObsMap.keySet & diff.plan.delete.map(_.key)
    val localOnly: List[ObsEdit] = localKeys.map { k =>
      val nv = remoteVm.getOrElse(k, EmptyNodeVersions)
      if (nv === EmptyNodeVersions) ObsCreate(k, localObs(k))
      else ObsUpdate(k, localObs(k), None, compare(localObsMap(k)))
    }(breakOut)

    // Deleted locally.
    val remoteKeys = remoteObsMap.keySet &~ commonKeys
    val remoteOnly: List[TryVcs[ObsEdit]] = remoteKeys.filterNot(k => local.getVersions(k) === EmptyNodeVersions).map { k =>
      mapRemoteStatus(k) { stat => ObsDelete(k, remoteObs(k, stat)) }
    }(breakOut)

    // Concatenate deleted locally, new locally, and edited.
    for {
      ro <- remoteOnly.sequenceU
      co <- common.sequenceU
    } yield
      ro ++ (localOnly ++ co).filter {
        case ObsUpdate(_, _, _, comp) =>
          comp.combined match {
            case Newer | Conflicting  => true
            case _                    => false  // Remove anything that was not edited locally.
          }
        case _                        => true
      }
  }
}
