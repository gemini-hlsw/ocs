package edu.gemini.spModel.target.env

import edu.gemini.spModel.guide.GuideProbe

import java.io.IOException

import scalaz._
import Scalaz._

final case class GuideEnv(auto: AutomaticGroup, manual: List[ManualGroup] \/ Zipper[ManualGroup]) {

  def groups: List[GuideGrp] =
    auto :: manual.fold(identity, _.toList)

  def primaryGroup: GuideGrp =
    manual.fold(_ => auto, _.focus)

  def referencedGuiders: Set[GuideProbe] =
    groups.foldMap(_.referencedGuiders)

  def primaryReferencedGuiders: Set[GuideProbe] =
    primaryGroup.referencedGuiders
}

/** A guide environment is a bags group (possibly empty or "initial") followed
  * by zero or more manual groups. One is always selected. If the second
  * element in the pair is a list, it means the bags group is selected.
  * Otherwise the selection is indicated by the zipper.
  */
object GuideEnv {
  val initial: GuideEnv = GuideEnv(AutomaticGroup.Initial, Nil.left)
}