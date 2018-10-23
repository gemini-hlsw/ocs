package edu.gemini.spModel.gemini.obscomp

import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow
import TimingWindow.{ REPEAT_FOREVER, REPEAT_NEVER, WINDOW_REMAINS_OPEN_FOREVER }

import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import java.time.Instant

object TimingWindowEndSpec extends Specification {
  val Empty = ImOption.empty[TimingWindow]

  "TimingWindow.getEnd" should {

    "return None if repeating" in {
      val tw = new TimingWindow(0L, 10, REPEAT_FOREVER, 10)
      tw.getEnd shouldEqual Empty
    }

    "return None if always open" in {
      val tw = new TimingWindow(0L, WINDOW_REMAINS_OPEN_FOREVER, REPEAT_NEVER, 0)
      tw.getEnd shouldEqual Empty
    }

    "return the end of the last repeat period if any" in {
      val tw = new TimingWindow(0L, 2, 10, 10)
      tw.getEnd.getValue shouldEqual Instant.ofEpochMilli(102L) // 0 + 10*10 + 2
    }
  }

}
