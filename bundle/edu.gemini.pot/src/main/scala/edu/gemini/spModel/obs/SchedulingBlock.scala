package edu.gemini.spModel.obs

/**
 * Created with IntelliJ IDEA.
 * User: sraaphor
 * Date: 3/15/14
 * Time: 4:07 PM
 * To change this template use File | Settings | File Templates.
 */
case class SchedulingBlock(start: Long, duration: Long) {
}

object SchedulingBlock {
  def valueOf(startString: String, durationString: String): SchedulingBlock = {
    SchedulingBlock(startString.toLong, durationString.toLong)
  }
}