package edu.gemini.qpt.core

import edu.gemini.qpt.core.util.Interval
import edu.gemini.qpt.shared.sp.MiniModel
import edu.gemini.skycalc.TwilightBoundedNight
import edu.gemini.skycalc.TwilightBoundType.CIVIL
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.pio.ParamSet
import edu.gemini.spModel.pio.xml.PioXmlUtil

import org.specs2.mutable._

import java.io.{ BufferedReader, InputStreamReader, Reader }

import scala.collection.JavaConverters._

/**
 * Schedule doc migration spec
 */
final class MigrationSpec extends Specification {

  private def closing[A](fileName: String)(test: ParamSet => A): A = {

    val is = new BufferedReader(new InputStreamReader(getClass.getResourceAsStream(fileName)))

    try {

      PioXmlUtil.read(is) match {
        case ps: ParamSet => test(ps.getParamSet("schedule"))
        case _            => sys.error(s"expected a paramset in file '$fileName'")
      }

    } finally {
      is.close
    }

  }

  val site: Site = Site.GS

  "Schedule import" should {

    "update normal block to civil twilight" in {

      closing("v1031.qpt") { ps =>

        val s = new Schedule(MiniModel.empty(site), ps, 1031)
        s.getBlocks.asScala.toList match {
          case b :: nil =>
            val n = TwilightBoundedNight.forTime(CIVIL, b.getStart, site)
            val e = new Interval(n.getStartTime, n.getEndTime)
            e shouldEqual b.getInterval
          case _        =>
            sys.error("expected a single block")
        }
      }

    }

    "leave unusual block times alone" in {

      closing("v1031-unexpected.qpt") { ps =>

        val s = new Schedule(MiniModel.empty(site), ps, 1031)
        s.getBlocks.asScala.toList match {
          case b :: nil =>
            b.getStart shouldEqual 1533164653760l
            b.getEnd   shouldEqual 1533206039227l
          case _        =>
            sys.error("expected a single block")
        }
      }

    }

  }

}
