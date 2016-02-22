package edu.gemini.spModel.target.env

import scalaz._

/** A collection of lenses for working with `TargetEnvironment` and its
  * guide groups.
  */
object TargetEnv {
  val guide: TargetEnvironment @> GuideEnvironment =
    Lens.lensu((a,b) => a.setGuideEnvironment(b), _.getGuideEnvironment)

  val auto: TargetEnvironment @> AutomaticGroup =
    guide >=> GuideEnvironment.auto

  val manual: TargetEnvironment @> Option[OptsList[ManualGroup]] =
    guide >=> GuideEnvironment.manual
}
