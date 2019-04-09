package edu.gemini.ags.servlet.arb

import edu.gemini.ags.api.AgsStrategy.{ Assignment, Selection }
import edu.gemini.spModel.core.{ Angle, SiderealTarget }
import edu.gemini.spModel.guide.{ GuideProbe, GuideProbeMap }

import org.scalacheck._
import org.scalacheck.Arbitrary._

import scala.collection.JavaConverters._


trait ArbSelection {

  import siderealtarget._

  implicit val arbGuideProbe: Arbitrary[GuideProbe] =
    Arbitrary {
      Gen.oneOf(GuideProbeMap.instance.values.asScala.toList)
    }

  implicit val arbAssignment: Arbitrary[Assignment] =
    Arbitrary {
      for {
        p <- arbitrary[GuideProbe]
        t <- arbitrary[SiderealTarget]
      } yield Assignment(p, t)
    }

  implicit val arbSelection: Arbitrary[Selection] =
    Arbitrary {
      for {
        d  <- Gen.choose(0, 359)
        as <- arbitrary[List[Assignment]].map(_.groupBy(_.guideProbe).mapValues(_.head).values.toList)
      } yield Selection(Angle.fromDegrees(d.toDouble), as)
    }

}

object selection extends ArbSelection