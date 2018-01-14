package edu.gemini.qv.plugin.chart

import edu.gemini.qpt.shared.sp.Obs
import edu.gemini.qv.plugin.QvStore.NamedElement
import edu.gemini.shared.util.DateTimeUtils

object Chart {

  trait ChartFunction extends NamedElement {
    override val isEditable = false
    def label: String
  }

  case class Calculation(label: String, value: Set[Obs] => Double, isHours: Boolean = true) extends ChartFunction

  object ObservationCount extends Calculation("Count", {s: Set[Obs] => s.size}, isHours=false)

  // NOTE: Don't forget to transform observation set to seq first, otherwise identical values are lost and not summed up!
  object PiTimeSum extends Calculation("PI Planned Time (Hrs)", {s: Set[Obs] => s.toSeq.map(_.getPiPlannedTime).sum.toDouble / DateTimeUtils.MillisecondsPerHour})
  object ExecutionTimeSum extends Calculation("Exec Planned Time (Hrs)", {s: Set[Obs] => s.toSeq.map(_.getExecPlannedTime).sum.toDouble / DateTimeUtils.MillisecondsPerHour})
  object ElapsedTimeSum extends Calculation("Elapsed Time (Hrs)", {s: Set[Obs] => s.toSeq.map(_.getElapsedTime).sum.toDouble / DateTimeUtils.MillisecondsPerHour})
  object RemainingTimeSum extends Calculation("Remaining Time (Hrs)", {s: Set[Obs] => s.toSeq.map(_.getRemainingTime).sum.toDouble / DateTimeUtils.MillisecondsPerHour})

  val Calculations = Seq(ObservationCount, PiTimeSum, ExecutionTimeSum, ElapsedTimeSum, RemainingTimeSum)

}

