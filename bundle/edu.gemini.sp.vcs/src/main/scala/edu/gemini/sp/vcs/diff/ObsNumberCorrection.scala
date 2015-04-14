package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.pot.sp.version.LifespanId
import edu.gemini.sp.vcs.diff.MergeCorrection._
import edu.gemini.sp.vcs.diff.NodeDetail.Obs
import edu.gemini.sp.vcs.diff.ProgramLocation.{Remote, Local}
import edu.gemini.sp.vcs.diff.VcsFailure.Unmergeable
import edu.gemini.spModel.obslog.ObsExecLog

import scalaz._
import Scalaz._

/** Corrects observation numbers such that all local-only observations have
  * numbers greater than the maximum remote observation number.
  *
  * There are a few implicit assumptions at work here.  First, once an
  * observation has been created its key is always remembered in the
  * `VersionMap` (that's true of any node actually).  Second, as new
  * observations are created they are given the next sequential integer.
  * That is, observation numbers aren't reused when deleted.  Finally
  * observation numbers are never modified once assigned (other than by
  * the merge plan application itself).
  *
  * This means that there cannot exist an observation known to both local and
  * remote program versions with an observation number greater than a
  * local-only observation.
  */
class ObsNumberCorrection(lifespanId: LifespanId, isKnown: (ProgramLocation, SPNodeKey) => Boolean, maxRemote: Option[Int]) extends CorrectionFunction {
  def apply(mp: MergePlan): TryVcs[MergePlan] =
    renumberedObs(mp).map { obsMap =>
      if (obsMap.isEmpty) mp // usually empty so we might as well check and save a traversal in that case
      else
        mp.copy(update = mp.update.map {
          case m@Modified(k, nv, dob, Obs(n)) =>
            val newNum = obsMap.getOrElse(k, n)
            if (newNum == n) m else m.copy(k, nv.incr(lifespanId), dob, Obs(newNum))
          case lab => lab
        })
    }

  // Obtains a Set of pairs of observation node keys that need to be
  // renumbered along with the new observation number they should have.
  private def renumberedObs(mp: MergePlan): TryVcs[Map[SPNodeKey, Int]] = {
    def isExecuted(children: Stream[Tree[MergeNode]]): Boolean =
      children.toList.exists { _.rootLabel match {
        case Modified(_, _, log: ObsExecLog, _) => !log.isEmpty
        case _                                  => false
      }}

    val localOnly0 = mp.update.foldObservations(List.empty[(SPNodeKey, Int, Boolean)]) { (mod, i, children, lst) =>
      val k = mod.key
      (isKnown(Local, k), isKnown(Remote, k)) match {
        case (true, false) => (k, i, isExecuted(children)) :: lst
        case _             => lst
      }
    }

    val executedLocalOnly = localOnly0.collect { case (_, num, true) => num }

    if (executedLocalOnly.nonEmpty)
      ObsNumberCorrection.unmergeable(executedLocalOnly)
    else {
      val localOnly = localOnly0.map { case (key, num, _) => (key, num) }
      maxRemote.foldMap { max =>
        val sortedPairs = localOnly.sortBy(_._2) match {
          case (_, i) :: _ if i > max => Nil
          case obsList                => obsList
        }
        sortedPairs.unzip._1.zipWithIndex.map { case (k, i) => (k, i + max + 1)}.toMap
      }.right
    }
  }
}

object ObsNumberCorrection {
  def apply(mc: MergeContext): ObsNumberCorrection = {
    val isKnown: (ProgramLocation, SPNodeKey) => Boolean = {
      case (Local, key)  => mc.local.isKnown(key)
      case (Remote, key) => mc.remote.isKnown(key)
    }

    new ObsNumberCorrection(mc.local.prog.getLifespanId, isKnown, mc.remote.diff.maxObsNumber)
  }

  def unmergeable[A](obsNum: List[Int]): TryVcs[A] = {
    val msg = if (obsNum.size > 1)
      s"Found executed observations (numbers ${obsNum.sorted.mkString(",")}) that were created outside of the observing database."
    else
      s"Found an executed observation (number ${obsNum.mkString}) that was created outside of the observing database."

    (Unmergeable(msg): VcsFailure).left[A]
  }


}
