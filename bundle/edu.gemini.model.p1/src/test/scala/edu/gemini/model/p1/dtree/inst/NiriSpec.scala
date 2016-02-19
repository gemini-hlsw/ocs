package edu.gemini.model.p1.dtree.inst

import org.specs2.mutable.Specification
import edu.gemini.model.p1.immutable.{NiriCamera, AltairLGS}
import edu.gemini.model.p1.immutable.NiriFilter

class NiriSpec extends Specification {
  "The Niri decision tree" should {
    "allow Br(alpha) cont (3.990 um) filter with F/6 camera, REL-1057" in {
      val niri = new Niri.FilterNode((AltairLGS(pwfs1 = true), NiriCamera.F6))
      niri.choices must contain(NiriFilter.BBF_BRACONT)
    }
  }

}
