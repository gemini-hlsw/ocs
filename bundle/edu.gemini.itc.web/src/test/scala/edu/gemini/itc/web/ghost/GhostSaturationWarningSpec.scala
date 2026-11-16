package edu.gemini.itc.web.ghost

import edu.gemini.itc.service.ItcServiceImpl
import edu.gemini.itc.shared._
import edu.gemini.itc.web.golden.GoldenCases
import org.specs2.mutable.Specification

/**
 * GHOST reports a saturation warning on each arm's CCD once its peak pixel reaches the A-D limit
 * of 65535 ADU. These warnings are what the JSON API (and so GPP) receives; the web page used to
 * show the same check only in its HTML.
 */
object GhostSaturationWarningSpec extends Specification {

  private val itc = new ItcServiceImpl

  private val AdcLimit = 65535

  // A V = 15 A0V star in 4 x 600 s, far below saturation.
  private val faint: ItcParameters = GoldenCases.ghost.head.params

  // The same request for a V = 0 star, which saturates both arms.
  private val bright: ItcParameters = faint.copy(source = faint.source.copy(norm = 0.0))

  private def ccds(p: ItcParameters): List[ItcCcd] =
    itc.calculate(p, false).fold(e => sys.error(s"ITC error: ${e.msg}"), identity) match {
      case s: ItcSpectroscopyResult => s.ccds
      case r                        => sys.error(s"Unexpected result: $r")
    }

  "GHOST saturation warnings" should {

    "not be reported for a faint target" in {
      val cs = ccds(faint)
      cs must haveSize(2)
      cs.forall(_.adu < AdcLimit) must beTrue
      cs.flatMap(_.warnings) must beEmpty
    }

    "be reported on each saturated arm, against that arm's own gain" in {
      val cs = ccds(bright)
      cs must haveSize(2)
      cs.forall(_.adu >= AdcLimit) must beTrue

      // The two arms have different gains, so each warning must state its own limit in e-.
      cs.map(_.ampGain).distinct must haveSize(2)
      cs.map { c =>
        c.warnings.map(_.msg) must_== List(
          f"Peak pixel count is ${c.peakPixelFlux / (AdcLimit * c.ampGain) * 100.0}%.0f%% of the A-D saturation limit of ${AdcLimit * c.ampGain}%.0f e- ($AdcLimit%d ADU)."
        )
      }.reduce(_ and _)
    }
  }

}
