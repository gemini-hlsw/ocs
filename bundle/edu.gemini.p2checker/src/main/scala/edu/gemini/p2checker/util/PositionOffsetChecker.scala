package edu.gemini.p2checker.util

import edu.gemini.p2checker.api.ObservationElements
import edu.gemini.spModel.target.offset.OffsetPosBase
import edu.gemini.spModel.target.offset.OffsetPosList
import edu.gemini.spModel.target.offset.OffsetUtil

import scala.collection.JavaConverters._

object PositionOffsetChecker {
  val PROBLEM_CODE    = "LGS_MAX_DIST"
  val PROBLEM_MESSAGE = "The maximum offset when using LGS is 5 arcminutes. Please reduce the size of the offset or make a separate observation."

  def hasBadOffsets(elements: ObservationElements): Boolean = {
    // Note that the bizarre cast is required here in order to make the Array type match OffsetPostList[] in Java.
    val lists   = OffsetUtil.allOffsetPosLists(elements.getObservationNode).asScala
    val arrays  = lists.toArray.asInstanceOf[Array[OffsetPosList[_ <: OffsetPosBase]]]
    val offsets = OffsetUtil.getOffsets(arrays).asScala.toSet
    offsets.exists(_.distance.toArcmins.toPositive.getMagnitude > 5.0)
  }
}