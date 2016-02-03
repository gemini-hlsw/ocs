package edu.gemini.spModel.target.env

import scalaz._, Scalaz._

/** A collection of lenses for working with `TargetEnvironment` and its
  * guide groups.
  */
object TargetEnv {
  val Guide: TargetEnvironment @> GuideEnvironment =
    Lens.lensu((a,b) => a.setGuideEnvironment(b), _.getGuideEnvironment)

  val Auto: TargetEnvironment @> AutomaticGroup =
    Guide >=> GuideEnvironment.Auto

  val Manual: TargetEnvironment @> Option[OptsList[ManualGroup]] =
    Guide >=> GuideEnvironment.Manual
}
