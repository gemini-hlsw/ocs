package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.sp.vcs.diff.NodeDetail.Obs
import edu.gemini.sp.vcs.diff.ProgramLocation.{Remote, Local}
import edu.gemini.spModel.obslog.ObsExecLog
import edu.gemini.spModel.rich.pot.sp._

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
class ObsNumberCorrection(isKnown: (ProgramLocation, SPNodeKey) => Boolean) extends MergeCorrection {
  def apply(mp: MergePlan): Unmergeable \/ MergePlan =
    renumberedObs(mp).map { obsMap =>
      if (obsMap.isEmpty)  // usually empty so we might as well check and save a traversal in that case
        mp
      else
        mp.copy(update = mp.update.map {
          case Modified(k, nv, dob, Obs(n)) => Modified(k, nv, dob, Obs(obsMap.getOrElse(k, n)))
          case lab                          => lab
        })
    }

  // Obtains a Set of pairs of observation node keys that need to be
  // renumbered along with the new observation number they should have.
  private def renumberedObs(mp: MergePlan): Unmergeable \/ Map[SPNodeKey, Int] = {

    /** A pair of the max remote-only observation number and a set of all
      * local-only observation keys with their current observation number.
      */
    case class ObsRenum(maxRemote: Option[Int], localOnly: List[(SPNodeKey, Int, Boolean)]) {
      def addRemote(num: Int): ObsRenum =
        if (maxRemote.forall(_ < num)) ObsRenum(Some(num), localOnly) else this

      def addLocal(k: SPNodeKey, num: Int, executed: Boolean): ObsRenum =
        ObsRenum(maxRemote, (k, num, executed) :: localOnly)
    }

    def isExecuted(children: Stream[Tree[MergeNode]]): Boolean =
      children.toList.exists { _.rootLabel match {
        case Modified(_, _, log: ObsExecLog, _) => !log.isEmpty
        case _                                  => false
      }}

    val or = mp.update.foldObservations(ObsRenum(None, List.empty)) { (mod, i, children, or) =>
      val k = mod.key
      (isKnown(Local, k), isKnown(Remote, k)) match {
        case (false, true) => or.addRemote(i)
        case (true, false) => or.addLocal(k, i, isExecuted(children))
        case _             => or
      }
    }

    val executedLocalOnly = or.localOnly.collect { case (_, num, true) => num }

    if (executedLocalOnly.nonEmpty)
      ObsNumberCorrection.unmergeable(executedLocalOnly).left
    else {
      val localOnly = or.localOnly.map { case (key, num, _) => (key, num) }
      or.maxRemote.foldMap { max =>
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

    new ObsNumberCorrection(isKnown)
  }

  def unmergeable(obsNum: List[Int]): Unmergeable = {
    val msg = if (obsNum.size > 1)
      s"Found executed observations (numbers ${obsNum.sorted.mkString(",")}) that were created outside of the observing database."
    else
      s"Found an executed observation (number ${obsNum.mkString}) that was created outside of the observing database."

    Unmergeable(msg)
  }


}
