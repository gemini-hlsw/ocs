package edu.gemini.sp.vcs.diff

import edu.gemini.pot.sp.{ISPNode, SPNodeKey}
import edu.gemini.pot.sp.version._
import edu.gemini.sp.vcs.diff.MergeCorrection.CorrectionFunction
import edu.gemini.spModel.template.{TemplateGroup, TemplateFolder}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.util.{VersionToken, VersionTokenUtil}


import scalaz._
import Scalaz._

/** A Merge correction function for duplicate template group numbers. Template
  * groups that are split locally can have `VersionToken`s that clash with
  * groups that were split remotely.  This correction function renumbers them
  * as appropriate so that they are unique. */
class TemplateNumberingCorrection(lifespanId: LifespanId, nodeMap: Map[SPNodeKey, ISPNode], isKnownRemote: SPNodeKey => Boolean) extends CorrectionFunction {

  def apply(mp: MergePlan): TryVcs[MergePlan] = {
    def correct(tokens: List[(VersionToken, SPNodeKey)]): TryVcs[MergePlan] = {
      def updateToken(tgLoc: TreeLoc[MergeNode], vt: VersionToken): TryVcs[TreeLoc[MergeNode]] =
        tgLoc.getLabel match {
          case m@Modified(_, _, tg: TemplateGroup, _) => TryVcs(tgLoc.modifyLabel(_ => m.copy(dob = tg.copy <| (_.setVersionToken(vt)))))
          case _                                      => TryVcs.fail(s"Expected a TemplateGroup for node ${tgLoc.key}")
        }

      (TryVcs(mp)/:tokens) { case (mp0, (vt, k)) =>
          for {
            mp  <- mp0
            tg0 <- mp.update.loc.find(_.key === k).toTryVcs(s"Missing template group $k")
            tg1 <- tg0.asModified(nodeMap)
            tg2 <- updateToken(tg1, vt)
            tg3 <- tg2.incr(lifespanId)
          } yield mp.copy(update = tg3.toTree)
      }
    }

    versionTokens(mp).flatMap { lst =>
      val (oldTokens, newTokens) = lst.partition { case (_, k) => isKnownRemote(k) }
      import VersionTokenUtil._
      correct(merge(normalize(oldTokens), normalize(newTokens)))
    }
  }

  /** Extracts all the `VersionToken`s from the `MergePlan`, assuming one or
    * more template groups have been modified.  Otherwise, produces an empty
    * list. */
  def versionTokens(mp: MergePlan): TryVcs[List[(VersionToken, SPNodeKey)]] = {

    // Find the template folder, if modified.  Otherwise, there's nothing to
    // renumber.
    val tf = mp.update.subForest.find(_.rootLabel match {
      case Modified(_, _, tf: TemplateFolder, _) => true
      case _                                     => false
    })

    // Get all the surviving template group keys and their version tokens,
    // looking up the unmodified values in the provided node map.
    tf.toList.flatMap { _.subForest.toList.map { _.rootLabel match {
      case Modified(k, _, tg: TemplateGroup, _) =>
        (tg.getVersionToken -> k).right

      case Unmodified(k)                        =>
        for {
          n <- nodeMap.get(k).toTryVcs(s"Missing unmodified node $k")
          p <- n.getDataObject match {
                 case tg: TemplateGroup => (tg.getVersionToken -> k).right
                 case _                 => TryVcs.fail(s"Expected a TemplateGroup for node $k")
               }
        } yield p

      case mn                                    =>
        TryVcs.fail(s"Expected a Template group for node ${mn.key}")

    }}}.sequenceU
  }

}


object TemplateNumberingCorrection {
  def apply(mc: MergeContext): TemplateNumberingCorrection = {
    val isKnown = (k: SPNodeKey) => nodeVersions(mc.remote.remoteVm, k) =/= EmptyNodeVersions

    new TemplateNumberingCorrection(mc.local.prog.getLifespanId, mc.local.nodeMap, isKnown)
  }
}
