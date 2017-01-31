package edu.gemini.model.p1.immutable

// All time amounts are in hours, so convert as necessary to nights.
case class ObservationTimes(progTime: TimeAmount, partTime: TimeAmount) {
  val totalTime = progTime |+| partTime
}
