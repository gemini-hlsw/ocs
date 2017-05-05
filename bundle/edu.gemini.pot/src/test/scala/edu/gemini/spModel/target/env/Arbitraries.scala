package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.ImList
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.{Angle, Declination, RightAscension}
import edu.gemini.spModel.guide.{GuideProbeMap, GuideProbe}
import edu.gemini.spModel.target.SPTarget
import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

import scala.collection.JavaConverters._

import scalaz._
import Scalaz._

trait Arbitraries extends edu.gemini.spModel.core.Arbitraries {

  def boundedList[A](max: Int, g: => Gen[A]): Gen[List[A]] =
    for {
      sz <- choose(0, max)
      as <- listOfN(sz, g)
    } yield as

  def boundedListOf[A: Arbitrary](max: Int): Gen[List[A]] =
    boundedList(max, arbitrary[A])

  implicit val arbSpTarget: Arbitrary[SPTarget] =
    Arbitrary {
      for {
        n <- alphaStr
        r <- arbitrary[RightAscension]
        d <- arbitrary[Declination]
      } yield new SPTarget(r.toAngle.toDegrees, d.toDegrees) <| (_.setName(n.take(4)))
    }

  implicit def arbZipper[A: Arbitrary]: Arbitrary[Zipper[A]] =
    Arbitrary {
      for {
        l <- boundedListOf[A](3)
        f <- arbitrary[A]
        r <- boundedListOf[A](3)
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
        as <- boundedListOf[A](3)
      } yield NonEmptyList(a, as: _*)
    }

  implicit def arbOneAndList[A: Arbitrary]: Arbitrary[OneAnd[List, A]] =
    Arbitrary {
      for {
        a  <- arbitrary[A]
        as <- boundedListOf[A](3)
      } yield OneAnd(a, as)
    }

  implicit def arbImList[A: Arbitrary]: Arbitrary[ImList[A]] =
    Arbitrary {
      boundedListOf[A](3).map(_.asImList)
    }

  implicit def arbOptsList[A: Arbitrary]: Arbitrary[OptsList[A]] =
    Arbitrary {
      arbitrary[OneAnd[List, A] \/ Zipper[A]].map { d => OptsList(d) }
    }

  implicit def arbScalazMap[A: Arbitrary : Order, B: Arbitrary]: Arbitrary[A ==>> B] =
    Arbitrary {
      boundedListOf[(A, B)](3).map(lst => ==>>.fromList(lst))
    }

  implicit val arbGuideProbe: Arbitrary[GuideProbe] =
    Arbitrary {
      oneOf(GuideProbeMap.instance.values().asScala.toList)
    }

  implicit val arbManualGroup: Arbitrary[ManualGroup] =
    Arbitrary {
      for {
        n <- alphaStr
        m <- boundedListOf[(GuideProbe, OptsList[SPTarget])](3).map { lst => ==>>.fromList(lst) }
      } yield ManualGroup(n.take(4), m)
    }

  implicit val arbAutomaticActiveGroup: Arbitrary[AutomaticGroup.Active] =
    Arbitrary {
      for {
        m <- boundedListOf[(GuideProbe, SPTarget)](3).map(lst => ==>>.fromList(lst))
        a <- arbitrary[Angle]
      } yield AutomaticGroup.Active(m, a)
    }

  implicit val arbAutomaticGroup: Arbitrary[AutomaticGroup] =
    Arbitrary {
      oneOf[AutomaticGroup](AutomaticGroup.Initial, AutomaticGroup.Disabled, arbitrary[AutomaticGroup.Active])
    }

  implicit val arbGuideGrp: Arbitrary[GuideGrp] =
    Arbitrary {
      oneOf(arbitrary[AutomaticGroup], arbitrary[ManualGroup])
    }

  implicit val arbGuideGroup: Arbitrary[GuideGroup] =
    Arbitrary {
      arbitrary[GuideGrp].map(GuideGroup)
    }

  implicit val arbGuideProbeTargets: Arbitrary[GuideProbeTargets] =
    Arbitrary {
      for {
        g  <- arbitrary[GuideProbe]
        ts <- arbitrary[NonEmptyList[SPTarget]]
      } yield GuideProbeTargets.create(g, ts.toList.asImList)
    }

  implicit val arbGuideEnv: Arbitrary[GuideEnv] =
    Arbitrary {
      for {
        a <- arbitrary[AutomaticGroup]
        m <- arbitrary[Option[OptsList[ManualGroup]]]
      } yield GuideEnv(a, m)
    }

  implicit val arbGuideEnvironment: Arbitrary[GuideEnvironment] =
    Arbitrary {
      arbitrary[GuideEnv].map(ge => GuideEnvironment(ge))
    }

  implicit val arbTargetEnvironment: Arbitrary[TargetEnvironment] =
    Arbitrary {
      for {
        b <- arbitrary[SPTarget]
        g <- arbitrary[GuideEnvironment]
        u <- boundedListOf[SPTarget](3)
      } yield TargetEnvironment.create(b, g, u.asImList)
    }

  implicit val arbSingleAsterism: Arbitrary[Asterism.Single] =
    Arbitrary(arbitrary[SPTarget].map(Asterism.Single(_)))

  implicit val arbAsterism: Arbitrary[Asterism] =
    Arbitrary(arbitrary[Asterism.Single]) // TODO:ASTERISM: add GHOST

}
