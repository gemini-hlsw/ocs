package edu.gemini.spModel.obs

case class SchedulingBlock(start: Long, duration: Long) {
  def useRemainingTime: Boolean = duration == 0L
}

object SchedulingBlock {
  def valueOf(startString: String, durationString: String): SchedulingBlock = {
    SchedulingBlock(startString.toLong, durationString.toLong)
  }
}