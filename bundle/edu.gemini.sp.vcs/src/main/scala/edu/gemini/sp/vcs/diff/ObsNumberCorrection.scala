package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.SPNodeKey
import edu.gemini.sp.vcs.diff.NodeDetail.Obs
import edu.gemini.sp.vcs.diff.ProgramLocation.{Remote, Local}
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
  def apply(mp: MergePlan): MergePlan = {
    val t = (mp.update.loc/:renumberedObs(mp)) { case (loc, (key, num)) =>
      loc.find(_.tree.rootLabel.key === key).fold(loc) { obsLoc =>
        val mergeNode = obsLoc.tree.rootLabel match {
          case Modified(k,nv,dob,Obs(_)) => Modified(k, nv, dob, Obs(num))
          case lab                       => lab
        }
        obsLoc.setLabel(mergeNode).root
      }
    }.toTree

    mp.copy(update = t)
  }

  // Obtains a Set of pairs of observation node keys that need to be
  // renumbered along with the new observation number they should have.
  private def renumberedObs(mp: MergePlan): Set[(SPNodeKey, Int)] = {

    /** A pair of the max remote-only observation number and a set of all
      * local-only observation keys with their current observation number.
      */
    case class ObsRenum(maxRemote: Option[Int], localOnly: List[(SPNodeKey, Int)]) {
      def addRemote(num: Int): ObsRenum =
        if (maxRemote.forall(_ < num)) ObsRenum(Some(num), localOnly) else this

      def addLocal(k: SPNodeKey, num: Int): ObsRenum =
        ObsRenum(maxRemote, (k, num) :: localOnly)
    }

    val or = mp.update.foldObservations(ObsRenum(None, List.empty)) { (mod, i, or) =>
      val k = mod.key
      (isKnown(Local, k), isKnown(Remote, k)) match {
        case (false, true) => or.addRemote(i)
        case (true, false) => or.addLocal(k, i)
        case _             => or
      }
    }

    or.maxRemote.fold(Set.empty[(SPNodeKey, Int)]) { max =>
      val sortedPairs = or.localOnly.sortBy(_._2) match {
        case (_, i) :: _ if i > max => Nil
        case os                     => os
      }
      sortedPairs.unzip._1.zipWithIndex.map { case (k, i) => (k, i + max + 1) }.toSet
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
}
