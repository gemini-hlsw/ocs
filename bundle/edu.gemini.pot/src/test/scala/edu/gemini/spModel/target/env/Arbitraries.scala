package edu.gemini.spModel.target.env

import edu.gemini.spModel.core.{Declination, RightAscension}
import edu.gemini.spModel.guide.{GuideProbeMap, GuideProbe}
import edu.gemini.spModel.target.SPTarget
import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

trait Arbitraries extends edu.gemini.spModel.core.Arbitraries {

  implicit val arbSpTarget: Arbitrary[SPTarget] = // arbitrary[Target].map(t => new SPTarget(???))
    Arbitrary {
      for {
        n <- alphaStr
        r <- arbitrary[RightAscension]
        d <- arbitrary[Declination]
      } yield new SPTarget(r.toAngle.toDegrees, d.toDegrees) <| (_.setName(n.take(4)))
    }

  implicit val arbZipperTarget: Arbitrary[Zipper[SPTarget]] =
    Arbitrary {
      for {
        l <- arbitrary[List[SPTarget]]
        f <- arbitrary[SPTarget]
        r <- arbitrary[List[SPTarget]]
      } yield Zipper(l.toStream, f, r.toStream)
    }

  implicit def arbDisjunction[A: Arbitrary, B: Arbitrary]: Arbitrary[A \/ B] =
    Arbitrary {
      for {
        b <- arbitrary[Boolean]
        d <- if (b) arbitrary[A].map(_.left[B]) else arbitrary[B].map(_.right[A])
      } yield d
    }

  implicit def arbNonEmptyList[A: Arbitrary]: Arbitrary[NonEmptyList[A]] =
    Arbitrary {
      for {
        a  <- arbitrary[A]
        as <- arbitrary[List[A]]
      } yield NonEmptyList.nel(a, as)
    }

  implicit val arbOptsListTarget: Arbitrary[OptsList[SPTarget]] =
    Arbitrary {
      arbitrary[NonEmptyList[SPTarget] \/ Zipper[SPTarget]].map { d => OptsList(d) }
    }

  implicit val arbGuideProbe: Arbitrary[GuideProbe] =
    Arbitrary { oneOf(GuideProbeMap.instance.values().asScala.toList) }

  implicit val arbManualGroup: Arbitrary[ManualGroup] =
    Arbitrary {
      for {
        n <- alphaStr
        m <- listOf(arbitrary[(GuideProbe, OptsList[SPTarget])]).map { _.toMap }
      } yield ManualGroup(n.take(4), m)
    }

  implicit val arbAutomaticActiveGroup: Arbitrary[AutomaticGroup.Active] =
    Arbitrary {
      listOf(arbitrary[(GuideProbe, SPTarget)]).map( _.toMap).map(AutomaticGroup.Active)
    }

  implicit val arbAutomaticGroup: Arbitrary[AutomaticGroup] =
    Arbitrary {
      oneOf(AutomaticGroup.Initial, arbitrary[AutomaticGroup.Active])
    }

  implicit val arbGuideGrp: Arbitrary[GuideGrp] =
    Arbitrary {
      oneOf(arbitrary[AutomaticGroup], arbitrary[ManualGroup])
    }

  implicit val arbGuideGroup: Arbitrary[GuideGroup] =
    Arbitrary {
      arbitrary[GuideGrp].map(GuideGroup)
    }
}
