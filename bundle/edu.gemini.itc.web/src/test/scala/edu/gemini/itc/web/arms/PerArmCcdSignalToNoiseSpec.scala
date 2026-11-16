package edu.gemini.itc.web.arms

import edu.gemini.itc.service.ItcServiceImpl
import edu.gemini.itc.shared._
import edu.gemini.itc.web.golden.{GoldenCase, GoldenCases}
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragment

/**
 * GHOST and IGRINS-2 compute each arm separately and return one chart group per arm. Each arm's CCD
 * must report the S/N of its own charts, not the S/N of the first arm.
 */
object PerArmCcdSignalToNoiseSpec extends Specification {

  private val itc = new ItcServiceImpl

  private val cases: List[GoldenCase] =
    GoldenCases.all.filter(c => c.key.startsWith("ghost") || c.key.startsWith("igrins2"))

  private def spectroscopy(c: GoldenCase): ItcSpectroscopyResult =
    itc.calculate(c.params, false).fold(e => sys.error(s"${c.key}: ITC error: ${e.msg}"), identity) match {
      case s: ItcSpectroscopyResult => s
      case r                        => sys.error(s"${c.key}: unexpected result $r")
    }

  private def peak(group: SpcChartGroup, t: SpcDataType): Double =
    group.charts.filter(_.chartType == S2NChart).flatMap(_.allSeries(t)).map(_.yValues.max).max

  "Per-arm CCD S/N" should {

    "cover GHOST and IGRINS-2" in {
      cases.map(_.key.takeWhile(_ != '-')).distinct must containTheSameElementsAs(List("ghost", "igrins2"))
    }

    Fragment.foreach(cases) { c =>
      s"come from each arm's own charts for ${c.key}" in {
        val r = spectroscopy(c)
        r.ccds must haveSize(2)

        val perArm = r.ccds.zip(r.chartGroups).map { case (ccd, group) =>
          (ccd.singleSNRatio must_== peak(group, SingleS2NData)) and
            (ccd.totalSNRatio must_== peak(group, FinalS2NData))
        }.reduce(_ and _)

        // The arms cover different wavelengths, so their S/N differs.
        perArm and (r.ccds.map(_.totalSNRatio).distinct must haveSize(2))
      }
    }
  }

}
