package edu.gemini.model.p1.immutable

// All time amounts are in hours, so convert as necessary to nights.
// Note that partTime here refers to the Night Basecal Time and is not "partner time" in the traditional sense.
case class ObservationTimes(progTime: TimeAmount, partTime: TimeAmount) {
  val totalTime = progTime |+| partTime
}
