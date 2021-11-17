package edu.gemini.spModel.target.env

import edu.gemini.shared.util.immutable.ImList
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension, SiderealTarget}
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.guide.{GuideProbe, GuideProbeMap}
import edu.gemini.spModel.target.{SPCoordinates, SPTarget}
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
        a <- arbitrary[A]
        as <- boundedListOf[A](3)
      } yield NonEmptyList(a, as: _*)
    }

  implicit def arbOneAndList[A: Arbitrary]: Arbitrary[OneAnd[List, A]] =
    Arbitrary {
      for {
        a <- arbitrary[A]
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
        g <- arbitrary[GuideProbe]
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

  implicit val arbUserTarget: Arbitrary[UserTarget] =
    Arbitrary {
      for {
        u <- Gen.oneOf(UserTarget.Type.values)
        t <- arbitrary[SPTarget]
      } yield new UserTarget(u, t)
    }

  // Create generators for TargetEnvironments with specific types of arbitraries.
  private def createTargetEnvironmentGen[A <: Asterism](arb: Gen[A]): Gen[TargetEnvironment] =
    for {
      a <- arb
      g <- arbitrary[GuideEnvironment]
      u <- boundedListOf[UserTarget](3)
    } yield new TargetEnvironment(a, g, u.asImList)

  implicit val arbTargetEnvironment: Arbitrary[TargetEnvironment] =
    Arbitrary(createTargetEnvironmentGen(arbitrary[Asterism]))

  implicit val arbSingleAsterism: Arbitrary[Asterism.Single] =
    Arbitrary(arbitrary[SPTarget].map(Asterism.Single(_)))

  // Seal off all the generators for the pieces of GhostAsterisms.
  object GhostGens {
    import GhostAsterism._

    // This generates a list of n coordinates, wherein the last n-1 are within distance [lower, upper) from the first.
    def genNCoordsWithinDistance(n: Int, lower: Angle, upper: Angle): Gen[List[Coordinates]] =
      for {
        c  <- arbitrary[Coordinates]
        cs <- listOfN(n-1, genCoordsWithinDistance(c, lower, upper))
      } yield c :: cs

    // TODO:GHOST Change these to GHOST constants as we learn more, and move to Ghost, probably.
    val inRangeDistance: Angle = Angle.fromArcsecs(5.0)
    val border:          Angle = Angle.fromArcsecs(100.0)
    val borderDelta:     Angle = Angle.fromArcsecs(5.0)

    // The patterns we allow.
    def totallyRandomCoords(n: Int): Gen[List[Coordinates]] =
      Gen.listOfN(n, arbitrary[Coordinates])
    def alwaysInRangeCoords(n: Int): Gen[List[Coordinates]] =
      genNCoordsWithinDistance(n, Angle.zero, inRangeDistance)
    def nearBorderCoords(n: Int): Gen[List[Coordinates]] =
      genNCoordsWithinDistance(n, border - borderDelta, border + borderDelta)

    def coordinateGen(n: Int): Gen[List[Coordinates]] =
      Gen.frequency((1, totallyRandomCoords(n)), (1, alwaysInRangeCoords(n)), (8, nearBorderCoords(n)))

    // Create a GHOST target with a given set of coordinates.
    def ghostTargetWithCoords(c: Coordinates): Gen[GhostAsterism.GhostTarget] =
      for {
        t <- arbitrary[SiderealTarget].map(_.copy(coordinates = c)).map(new SPTarget(_))
        f <- arbitrary[GhostAsterism.GuideFiberState]
      } yield GhostTarget(t, f)

    // SINGLE TARGET GENERATORS
    val genSingleTargetNoBase: Gen[SingleTarget] =
      arbitrary[GhostTarget].map(t => SingleTarget(t, None))

    val genSingleTargetWithBase: Gen[SingleTarget] =
      for {
        bc :: tc :: _ <- coordinateGen(2)
        t <- ghostTargetWithCoords(tc)
      } yield SingleTarget(t, Some(new SPCoordinates(bc)))

    // DUAL TARGET GENERATORS
    val genDualTargetsNoBase: Gen[DualTarget] =
      for {
        tc1 :: tc2 :: _ <- coordinateGen(2)
        t1 <- ghostTargetWithCoords(tc1)
        t2 <- ghostTargetWithCoords(tc2)
      } yield DualTarget(t1, t2, None)

    val genDualTargetsWithBase: Gen[DualTarget] =
      for {
        bc :: tc1 :: tc2 :: _ <- coordinateGen(3)
        t1 <- ghostTargetWithCoords(tc1)
        t2 <- ghostTargetWithCoords(tc2)
      } yield DualTarget(t1, t2, Some(new SPCoordinates(bc)))

    // TARGET + SKY
    val genTargetPlusSkyNoBase: Gen[TargetPlusSky] =
      for {
        tc :: s :: _ <- coordinateGen(2)
        t <- ghostTargetWithCoords(tc)
      } yield TargetPlusSky(t, new SPCoordinates(s), None)

    val genTargetPlusSkyWithBase: Gen[TargetPlusSky] =
      for {
        bc :: tc :: s :: _ <- coordinateGen(3)
        t <- ghostTargetWithCoords(tc)
      } yield TargetPlusSky(t, new SPCoordinates(s), Some(new SPCoordinates(bc)))

    // SKY + TARGET
    val genSkyPlusTargetNoBase: Gen[SkyPlusTarget] =
      genTargetPlusSkyNoBase.map {
        case TargetPlusSky(t, s, None) => SkyPlusTarget(s, t, None)
      }

    val genSkyPlusTargetWithBase: Gen[SkyPlusTarget] =
      genTargetPlusSkyWithBase.map {
        case TargetPlusSky(t, s, Some(bc)) => SkyPlusTarget(s, t, Some(bc))
      }

    // HIGH RESOLUTION TARGET + SKY
    val genHighResTargetPlusSkyNoBase: Gen[HighResolutionTargetPlusSky] =
      for {
        tc :: s :: _ <- coordinateGen(2)
        t <- ghostTargetWithCoords(tc)
      } yield HighResolutionTargetPlusSky(t, new SPCoordinates(s), None)

    val genHighResTargetPlusSkyWithBase: Gen[HighResolutionTargetPlusSky] =
      for {
        bc :: tc :: s :: _ <- coordinateGen(3)
        t <- ghostTargetWithCoords(tc)
      } yield HighResolutionTargetPlusSky(t, new SPCoordinates(s), Some(new SPCoordinates(bc)))
  }

  implicit val arbGuideFiberState: Arbitrary[GhostAsterism.GuideFiberState] =
    Arbitrary(oneOf(GhostAsterism.GuideFiberState.All.toList))

  implicit val arbGhostTarget: Arbitrary[GhostAsterism.GhostTarget] =
    Arbitrary {
      for {
        t <- arbitrary[SPTarget]
        f <- arbitrary[GhostAsterism.GuideFiberState]
      } yield GhostAsterism.GhostTarget(t, f)
    }

  implicit val arbGhostSingleTarget: Arbitrary[GhostAsterism.SingleTarget] =
    Arbitrary(Gen.oneOf(
      GhostGens.genSingleTargetNoBase,
      GhostGens.genSingleTargetWithBase
    ))

  implicit val arbGhostDualTarget: Arbitrary[GhostAsterism.DualTarget] =
    Arbitrary(Gen.oneOf(
      GhostGens.genDualTargetsNoBase,
      GhostGens.genDualTargetsWithBase
    ))

  implicit val arbGhostTargetPlusSky: Arbitrary[GhostAsterism.TargetPlusSky] =
    Arbitrary(Gen.oneOf(
      GhostGens.genTargetPlusSkyNoBase,
      GhostGens.genTargetPlusSkyWithBase
    ))

  implicit val arbGhostSkyPlusTarget: Arbitrary[GhostAsterism.SkyPlusTarget] =
    Arbitrary(Gen.oneOf(
      GhostGens.genSkyPlusTargetNoBase,
      GhostGens.genSkyPlusTargetWithBase
    ))

  implicit val arbGhostStandardResolutionAsterism: Arbitrary[GhostAsterism.StandardResolution] =
    Arbitrary(Gen.oneOf(
      arbitrary[GhostAsterism.SingleTarget],
      arbitrary[GhostAsterism.DualTarget],
      arbitrary[GhostAsterism.TargetPlusSky],
      arbitrary[GhostAsterism.SkyPlusTarget]
    ))

  implicit val arbGhostHighResolutionTargetPlusSkyAsterism: Arbitrary[GhostAsterism.HighResolutionTargetPlusSky] =
    Arbitrary(Gen.oneOf(
      GhostGens.genHighResTargetPlusSkyNoBase,
      GhostGens.genHighResTargetPlusSkyWithBase
    ))


  implicit val arbGhostHighResolutionAsterism: Arbitrary[GhostAsterism.HighResolution] =
    Arbitrary(
      arbitrary[GhostAsterism.HighResolutionTargetPlusSky]
    )

  implicit val arbGhostAsterism: Arbitrary[GhostAsterism] =
    Arbitrary(Gen.oneOf(
      arbitrary[GhostAsterism.StandardResolution],
      arbitrary[GhostAsterism.HighResolution]
    ))

  implicit val arbAsterism: Arbitrary[Asterism] =
    Arbitrary(Gen.oneOf(
      arbitrary[Asterism.Single],
      arbitrary[GhostAsterism]
    ))

  // Specific target environment types to avoid Scalacheck giving up.
  // These can't go in GhostGens or we get a stack overflow error.
  val genGhostSingleTargetTargetEnvironment: Gen[TargetEnvironment] =
    createTargetEnvironmentGen(arbitrary[GhostAsterism.SingleTarget])
  val genGhostDualTargetTargetEnvironment: Gen[TargetEnvironment] =
    createTargetEnvironmentGen(arbitrary[GhostAsterism.DualTarget])
  val genGhostTargetPlusSkyTargetEnvironment: Gen[TargetEnvironment] =
    createTargetEnvironmentGen(arbitrary[GhostAsterism.TargetPlusSky])
  val genGhostSkyPlusTargetTargetEnvironment: Gen[TargetEnvironment] =
    createTargetEnvironmentGen(arbitrary[GhostAsterism.SkyPlusTarget])
  val genGhostHighResTargetPlusSkyAsterismTargetEnvironment: Gen[TargetEnvironment] =
    createTargetEnvironmentGen(arbitrary[GhostAsterism.HighResolutionTargetPlusSky])
  val genGhostAsterismTargetEnvironment: Gen[TargetEnvironment] =
    createTargetEnvironmentGen(arbitrary[GhostAsterism])
  val genSingleAsterismTargetEnvironment: Gen[TargetEnvironment] =
    createTargetEnvironmentGen(arbitrary[Asterism.Single])
}
