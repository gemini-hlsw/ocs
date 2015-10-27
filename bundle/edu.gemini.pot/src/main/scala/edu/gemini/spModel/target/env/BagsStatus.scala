package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.{Option => GOption}
import edu.gemini.spModel.pio.{ParamSet, Pio, PioFactory}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.rich.shared.immutable._

// Possible BAGS statuses used by GuideProbeTargets to indicate the state of BAGS targets and lookups.
sealed trait BagsStatus extends Cloneable {
  val bagsStar: Option[SPTarget] = None
  def bagsStarAsJava: GOption[SPTarget] = bagsStar.asGeminiOpt
  override def clone: BagsStatus = this

  def getParamSet(factory: PioFactory): ParamSet = {
    val paramSet = factory.createParamSet(BagsStatus.BagsStatusParamSetName)
    Pio.addParam(factory, paramSet, BagsStatus.BagsStatusParamName, id)
    bagsStar.foreach { t =>
      val bagsParamSet = factory.createParamSet(BagsStatus.BagsTargetParamSetName)
      bagsParamSet.addParamSet(t.getParamSet(factory))
    }
    paramSet
  }

  def id: String
}

// TODO: For objects, we want to just return this.
// TODO: For case class, we want to return an instance.

// Note that we use BagsLookupPending as a default state when BAGS hasn't run or should be rerun, and the BagsManager
// determines based on the observation if BAGS actually should be run.
case object BagsLookupPending extends BagsStatus {
  override val id = "BagsLookupPending"
}
case object BagsLookupRunning extends BagsStatus {
  override val id = "BagsLookupRunning"
}

case class BagsSuccessWithTarget(target: SPTarget) extends BagsStatus with Cloneable {
  override val bagsStar = Option(target)
  override def clone = BagsSuccessWithTarget(target.clone())
  override val id = BagsSuccessWithTarget.id
}
object BagsSuccessWithTarget {
  val id = "BagsSuccessWithTarget"
}

case object BagsSuccessNoTargets extends BagsStatus {
  override val id = "BagsSuccessNoTargets"
}


object BagsStatus {
  val BagsStatusParamSetName: String = "bagsStatus"
  val BagsStatusParamName:    String = "bagsStatusType"
  val BagsTargetParamSetName: String = "bagsTarget"


//  def fromParamSet(parent: ParamSet): BagsStatus = {
//    Option(parent.getParamSet(BagsStatusParamSetName)).map { ps =>
//      Option(ps.getParam(BagsStatusParamName).getValue) match {
//        case Some(BagsSuccessNoTargets.id)  => BagsSuccessNoTargets
//        case Some(BagsSuccessWithTarget.id) =>
//          Option(ps.getParamSet(BagsTargetParamSetName)).map(bps => BagsSuccessWithTarget(SPTarget.fromParamSet(bps))).getOrElse(BagsLookupPending)
//        case _ => BagsLookupPending
//      }
//    }.getOrElse(BagsLookupPending)
//  }
  def fromParamSet(parent: ParamSet): BagsStatus = {
    Option(parent.getParamSet(BagsStatusParamSetName)).flatMap { ps =>
      Option(ps.getParam(BagsStatusParamName).getValue).collect {
        case BagsSuccessNoTargets.id  => BagsSuccessNoTargets
        case BagsSuccessWithTarget.id =>
          Option(ps.getParamSet(BagsTargetParamSetName)).map(bps => BagsSuccessWithTarget(SPTarget.fromParamSet(bps))).getOrElse(BagsLookupPending)
      }
    }.getOrElse(BagsLookupPending)
  }
}