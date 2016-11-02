package edu.gemini.ags.impl

import edu.gemini.ags.TargetsHelper
import edu.gemini.ags.api.{AgsMagnitude, AgsRegistrar, AgsStrategy}
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.pot.sp.SPComponentType
import edu.gemini.pot.ModelConverters._
import edu.gemini.shared.util.immutable.{ None => JNone }
import edu.gemini.skycalc.{Offset, DDMMSS, HHMMSS}
import edu.gemini.spModel.core._
import edu.gemini.spModel.gemini.altair.{InstAltair, AltairParams}
import edu.gemini.spModel.gemini.inst.InstRegistry
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.Conditions._
import edu.gemini.spModel.guide.{GuideSpeed, GuideProbe}
import edu.gemini.spModel.guide.GuideSpeed._
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.telescope.{PosAngleConstraintAware, PosAngleConstraint, IssPortProvider}
import edu.gemini.spModel.telescope.IssPort._

import org.junit.Assert._

import java.awt.geom.{Point2D, PathIterator, AffineTransform, Area}

import scala.collection.JavaConverters._
import AlmostEqual.AlmostEqualOps

/**
 * Support for running single-probe tests.
 */

object AgsTest extends TargetsHelper {

  private val magTable = ProbeLimitsTable.loadOrThrow()

  // Value to nudge things off of the border
  private val nudge = 1 //1e-3

  // Minimum distance that points must be from all points on the boundary defining points of the shape in order
  // to be considered for inclusion.
  private val minimumDistance = 1

  def apply(instType: SPComponentType, guideProbe: GuideProbe): AgsTest =
    apply(instType, guideProbe, None)

  def apply(instType: SPComponentType, guideProbe: GuideProbe, site: Site): AgsTest = {
    apply(instType, guideProbe, Some(site))
  }

  def apply(instType: SPComponentType, guideProbe: GuideProbe, site: Option[Site]): AgsTest = {
    val base       = new SPTarget(0.0, 0.0)
    val targetEnv  = TargetEnvironment.create(base)

    val inst = InstRegistry.instance.prototype(instType.narrowType).getValue
    inst match {
      case pp: IssPortProvider => pp.setIssPort(SIDE_LOOKING)
      case _ => // do nothing
    }
    inst.setPosAngleDegrees(0.0)

    val ctx = if (site.isDefined) ObsContext.create(targetEnv, inst, site.asGeminiOpt, BEST, java.util.Collections.emptySet(), null, JNone.instance())
              else ObsContext.create(targetEnv, inst, BEST, java.util.Collections.emptySet(), null, JNone.instance())
    AgsTest(
      ctx,
      guideProbe,
      Nil,
      Nil)
  }

  def siderealTarget(raDecStr: String, rMag: Double): SiderealTarget = {
    val (raStr, decStr) = raDecStr.span(_ != ' ')
    val ra  = Angle.fromDegrees(HHMMSS.parse(raStr).toDegrees.getMagnitude)
    val dec = Angle.fromDegrees(DDMMSS.parse(decStr.trim).toDegrees.getMagnitude)
    val sc  = Coordinates(RightAscension.fromAngle(ra), Declination.fromAngle(dec).getOrElse(Declination.zero))
    target(raDecStr, sc, List(new Magnitude(rMag, MagnitudeBand.R)))
  }

  def siderealTargets(so: (String, Double)*): List[SiderealTarget] = {
    so.toList.map((siderealTarget _).tupled)
  }

  def usableSiderealTarget(so: (String, Double, GuideSpeed)*): List[(SiderealTarget, GuideSpeed)] = {
    val sos = siderealTargets(so.map { case (c, d, _) => (c, d) }: _*)
    sos.zip(so.map { case (_, _, gs) => gs })
  }
}

case class AgsTest(ctx: ObsContext, guideProbe: GuideProbe, usable: List[(SiderealTarget, GuideSpeed)], unusable: List[SiderealTarget],
                   calculateValidArea: (ObsContext, GuideProbe) => Area
                    = (ctx: ObsContext, probe: GuideProbe) => probe.getCorrectedPatrolField(ctx).getValue.getArea) extends TargetsHelper {
  import AgsTest.{nudge, minimumDistance, magTable}
  type Point = Point2D.Double

  def unusable(so: (String, Double)*): AgsTest =
    copy(unusable = AgsTest.siderealTargets(so: _*))

  def usable(so: (String, Double, GuideSpeed)*): AgsTest =
    copy(usable = AgsTest.usableSiderealTarget(so: _*))

  def rotated(deg: Double): AgsTest =
    copy(ctx.withPositionAngle(Angle.fromDegrees(deg)))

  def withConditions(c: Conditions): AgsTest =
    copy(ctx.withConditions(c))

  def withAltair(mode: AltairParams.Mode) = {
    val aoComp = new InstAltair
    aoComp.setMode(mode)
    copy(ctx.withAOComponent(aoComp))
  }

  def withOffsets(offsets: (Double, Double)*) = {
    val o = offsets map {
      case (p, q) => new Offset(Angle.fromArcsecs(p).toOldModel, Angle.fromArcsecs(q).toOldModel)
    }
    copy(ctx.withSciencePositions(o.toSet.asJava))
  }

  def withStrategyOverride(s: AgsStrategy): AgsTest =
    copy(ctx.withAgsStrategyOverride(Option(s.key).asGeminiOpt))

  def withValidArea(f: (ObsContext, GuideProbe) => Area): AgsTest =
    copy(calculateValidArea = f)

  // Calculate a list of candidates from the area, making sure that these points respect the minimum distance.
  private def areaCandidates(a: Area, maxOuterPoints: Int = 10): (List[Point],List[Point]) = {
    // Unfortunately, we need mutability here of an array due to Java.
    def pathSegments(t: AffineTransform = new AffineTransform()): List[(Int, List[Point])] = {
      def pathSegmentsAux(pi: PathIterator): List[(Int, List[Point])] = {
        if (pi.isDone) Nil
        else {
          val arr = new Array[Double](6)
          val curveType = pi.currentSegment(arr)
          pi.next()

          val (xcoords, ycoords) = arr.zipWithIndex.partition { case (_, idx) => idx % 2 == 0}
          val points = xcoords.map(_._1).zip(ycoords.map(_._1)).map{ case (x,y) => new Point(x,y)}.toList

          (curveType, points.take(curveType match {
            case PathIterator.SEG_CLOSE => 0
            case PathIterator.SEG_MOVETO | PathIterator.SEG_LINETO => 1
            case PathIterator.SEG_QUADTO => 2
            case PathIterator.SEG_CUBICTO => 3
          })) :: pathSegmentsAux(pi)
        }
      }
      pathSegmentsAux(a.getPathIterator(t))
    }

    // Given a list of points on an area, create outer points from these points by nudging them in four
    // different directions and then determining if they are in or out of the area. We allow limiting (default 1, up to 4)
    // of the number of points allowed to be generated for each point. A list with the candidates
    // outside of the area is returned.
    def createOuterPoints(points: List[Point], maxOuterPointsPerPoint: Int = 1): List[Point] =
      (for (p <- points) yield (for {
        xmult <- List(-1,1)
        ymult <- List(-1,1)
        newPoint = new Point(p.getX + xmult * nudge, p.getY + ymult * nudge)
        if !points.exists(_.distance(newPoint) < minimumDistance) && !a.contains(newPoint)
      } yield newPoint).take(maxOuterPointsPerPoint)).flatten

    // Try to find a single working point in the area through a (depth-limited) quaternary BFS search.
    def findInnerPoint(depth: Int = 4): Option[Point] = {
      def areaBFS(remain: Stream[((Double,Double),(Double,Double))], remainDepth: Int): Stream[Point] = {
        if (remainDepth <= 0) Stream.empty
        else {
          remain match {
            case s if s.isEmpty => Stream.empty
            case ((xMin, xMax), (yMin, yMax)) #:: tail =>
              if (xMax >= xMin && yMax >= yMin) {
                val xMid = xMin + (xMax - xMin) / 2
                val yMid = yMin + (yMax - yMin) / 2

                // The processing for this area.
                val curr = if (a.contains(xMid, yMid)) Stream(new Point(xMid, yMid))
                           else Stream.empty

                // The new areas to process in a BFS fashion.
                val newAreas = for {
                  newx <- Stream((xMin, xMid - nudge), (xMid + nudge, xMax))
                  newy <- Stream((yMin, yMid - nudge), (yMid + nudge, yMax))
                } yield (newx, newy)

                curr append areaBFS(tail append newAreas, remainDepth-1)
              }
              else areaBFS(tail, remainDepth-1)
          }
        }
      }

      val boundingBox = a.getBounds2D
      areaBFS(Stream(((boundingBox.getMinX, boundingBox.getMaxX), (boundingBox.getMinY, boundingBox.getMaxY))), depth).headOption
    }

    // Get the points that fall outside of the area, limited to maxOuterPoints.
    val outpoints = pathSegments().map(_._2).flatMap(createOuterPoints(_)).take(maxOuterPoints)

    // Find an inner point to use, and return the candidates.
    (findInnerPoint().toList, outpoints)
  }

  private def boundingBoxCandidates(a: Area): (List[Point], List[Point]) = {
    val rect = a.getBounds2D

    val minX = rect.getMinX - nudge
    val minY = rect.getMinY - nudge
    val maxX = rect.getMaxX + nudge
    val maxY = rect.getMaxY + nudge
    val midX = minX + (maxX - minX)/2
    val midY = minY + (maxY - minY)/2

    val out = List(
      (minX, minY),
      (minX, midY),
      (minX, maxY),
      (midX, minY),
      (midX, maxY),
      (maxX, minY),
      (maxX, midY),
      (maxX, maxY)
    ).map{case (x,y) => new Point(x,y)}

    val in = List(new Point(midX, midY))

    (in, out)
  }

  private def genCandidates(a: Area, allUsable: Boolean = false, allUnusable: Boolean = false): AgsTest = {
    assertTrue(!(allUsable && allUnusable))

    val (inTmp, outList) = areaCandidates(a)
    val inList = if (a.isEmpty) Nil else inTmp

    val out = (if (!allUsable) outList else Nil) ++ (if (allUnusable)  inList else Nil)
    val in  = (if (allUsable)  outList else Nil) ++ (if (!allUnusable) inList else Nil)

    val mc = magTable.apply(ctx, guideProbe).get

    def mags = {
      val m    = GuideSpeed.values.toList.map { gs => gs -> mc.apply(ctx.getConditions, gs) }.toMap
      val fast = m(FAST)
      // Randomly pick one of the allowed bands
      val band = strategy.probeBands.bands(scala.util.Random.nextInt(strategy.probeBands.bands.size))

      def magList(base: Double)(adjs: (Double, Option[GuideSpeed])*): List[(Magnitude, Option[GuideSpeed])] =
        adjs.toList.map { case (adj, gs) => (new Magnitude(base + adj, band), gs) }

      val bright = fast.saturationConstraint.map(_.brightness).toList.flatMap { brightness =>
        magList(brightness)((-0.01, None), (0.0, Some(FAST)), (0.01, Some(FAST)))
      }

      val faintFast = magList(fast.faintnessConstraint.brightness)((-0.01, Some(FAST)), (0.0, Some(FAST)), (0.01, Some(MEDIUM)))
      val faintNorm = magList(m(MEDIUM).faintnessConstraint.brightness)((-0.01, Some(MEDIUM)), (0.0, Some(MEDIUM)), (0.01, Some(SLOW)))
      val faintSlow = magList(m(SLOW).faintnessConstraint.brightness)((-0.01, Some(SLOW)), (0.0, Some(SLOW)), (0.01, None))

      bright ++ faintFast ++ faintNorm ++ faintSlow
    }

    def toSkyCoordinates(lst: List[Point]): List[Coordinates] =
      lst.map { p => Coordinates(RightAscension.fromAngle(Angle.fromArcsecs(-p.getX)), Declination.fromAngle(Angle.fromArcsecs(-p.getY)).getOrElse(Declination.zero)) }.distinct

    def candidates(lst: List[Point]): List[(Coordinates, Magnitude, Option[GuideSpeed])] =
      for {
        sc <- toSkyCoordinates(lst)
        (mag, gs) <- mags
      } yield (sc, mag, gs)


    def name(base: String, i: Int): String =
      s"$base${ctx.getInstrument.getType.narrowType}($i)"

    val usableCandidates:List[(SiderealTarget, GuideSpeed)] = candidates(in).zipWithIndex.collect { case ((sc, mag, Some(gs)), i) =>
      (target(name("in", i), sc, List(mag)), gs)
    }

    val unusableCandidates = candidates(out).zipWithIndex.map { case ((sc, mag, _), i) =>
      target(name("out", i), sc, List(mag))
    }

    copy(usable = usableCandidates, unusable = unusableCandidates)
  }

  // A word about transforms.  The probe areas are specified in arcsecs but in
  // screen coordinates.  That means x increases to the right and y increases
  // toward the bottom.  When we provide offsets to the context, this is in
  // (p,q) so x increases to the left and y increases toward the top.  Rotation
  // is toward positive y in screen coordinates so again it is flipped with
  // respect to the position angle we set in the context.

  private def testXform(xform: AffineTransform = new AffineTransform(),
                        patrolField: Area = calculateValidArea(ctx, guideProbe),
                        allUsable: Boolean = false,
                        allUnusable: Boolean = false): Unit = {
    patrolField.transform(xform)
    genCandidates(patrolField, allUsable, allUnusable).test()
  }

  // Take a test case and produce three, one with each set of predefined conditions.
  private def allConditions(): List[AgsTest] =
    //List(BEST, WORST, NOMINAL).map(withConditions)
    List(BEST, WORST).map(withConditions)

  // Take a test case and produce 12, one with each 30 deg position angle
  private def allRotations(): List[AgsTest] =
    List(0.0, 270.0).map(rotated)

  // Take a test case and produce 36, one with each predefined condition and 30 deg position angle.
  private def allConditionsAndRotations(): List[AgsTest] =
    allConditions().flatMap(_.allRotations())

  // Convenience function to calculate the proper rotation.
  private def rotation: AffineTransform =
    AffineTransform.getRotateInstance(-ctx.getPositionAngle.toRadians)

  def testBase(): Unit =
    allConditions().foreach(_.testXform())

  def testBaseOneOffset(): Unit = {
    val xlat = AffineTransform.getTranslateInstance(-600.0, -600.0)
    allConditions().foreach(_.withOffsets((600.0, 600.0)).testXform(xlat))
  }

  def testBaseTwoDisjointOffsets(): Unit =
    allConditions().map(_.withOffsets((600.0, 600.0),(-600.0,-600.0))).foreach { tc =>
      tc.testXform(allUnusable = true)
      tc.testXform(patrolField = new Area(), allUnusable=true)
      tc.testXform(AffineTransform.getTranslateInstance(-600.0, -600.0), allUnusable = true)
      tc.testXform(AffineTransform.getTranslateInstance( 600.0,  600.0), allUnusable = true)

    }

  def testBaseTwoIntersectingOffsets(): Unit = {
    allConditions().map(_.withOffsets((400.0, 400.0), (300.0, 300.0))).foreach { tc =>
      // Construct the area consisting of the intersection of the patrol field with respect to both the offsets.
      val originalArea = calculateValidArea(ctx, guideProbe)
      val offsetArea1 = originalArea.createTransformedArea(AffineTransform.getTranslateInstance(-400.0, -400.0))
      val offsetArea2 = originalArea.createTransformedArea(AffineTransform.getTranslateInstance(-300.0, -300.0))

      // Intersection is a mutable operation, so finalArea will contain the intersection after this is done.
      val finalArea = new Area(offsetArea1)
      finalArea.intersect(offsetArea2)

      // At this point, the patrol field has already been translated, so no need to use a translation in testXform.
      tc.testXform(patrolField = finalArea)
    }
  }

  def testBaseRotated(): Unit =
    allConditionsAndRotations().foreach{ tc => tc.testXform(tc.rotation) }

  def testBaseRotatedOneOffset(): Unit = {
    val xlat = AffineTransform.getTranslateInstance(-600.0, -600.0)
    allConditionsAndRotations().foreach { tc =>
      val xlatRot = tc.rotation
      xlatRot.concatenate(xlat)
      tc.withOffsets((600.0, 600.0)).testXform(xlatRot)
    }
  }

  def testBaseRotatedTwoIntersectingOffsets(): Unit = {
    allConditionsAndRotations().map(_.withOffsets((400.0, 400.0), (300.0, 300.0))).foreach { tc =>
      // Construct the area consisting of the intersection of the patrol field with respect to both the offsets.
      val originalArea = calculateValidArea(ctx, guideProbe)

      val xlatRot1 = tc.rotation
      xlatRot1.concatenate(AffineTransform.getTranslateInstance(-400.0, -400.0))
      val offsetArea1  = originalArea.createTransformedArea(xlatRot1)

      val xlatRot2 = tc.rotation
      xlatRot2.concatenate(AffineTransform.getTranslateInstance(-300.0, -300.0))
      val offsetArea2  = originalArea.createTransformedArea(xlatRot2)

      // Intersection is a mutable operation, so finalArea will contain the intersection after this is done.
      val finalArea = new Area(offsetArea1)
      finalArea.intersect(offsetArea2)

      // At this point, the patrol field has already been transformed, so no need to use a transform in testXform.
      tc.testXform(patrolField = finalArea)
    }
  }

  def testBaseUnboundedPosAngleConstraint(): Unit = {
    if (ctx.getInstrument.isInstanceOf[PosAngleConstraintAware]) {
      allConditions().foreach { tc =>
        tc.ctx.getInstrument.asInstanceOf[PosAngleConstraintAware].setPosAngleConstraint(PosAngleConstraint.UNBOUNDED)

        // Now we want to create all the candidates for a position angle of 135 and test that these are accessible.
        val tcp = tc.rotated(135)
        val patrolField = tcp.calculateValidArea(ctx, guideProbe)
        patrolField.transform(tcp.rotation)
        val cands = tcp.genCandidates(calculateValidArea(ctx, guideProbe))

        val newTest = tc.copy(usable = cands.usable, unusable = Nil)
        newTest.test()
      }
    }
  }

  // gets the selected single probe strategy, or blows up
  def strategy: SingleProbeStrategy =
    AgsRegistrar.currentStrategy(ctx).get.asInstanceOf[SingleProbeStrategy]

  def assertEqualTarget(t1: SiderealTarget, t2: SiderealTarget): Unit = {
    assertEquals(t1.name, t2.name)
    assertEquals(t1.magnitudes, t2.magnitudes)
    assertTrue(t1.coordinates ~= t2.coordinates)
  }

  def test(): Unit = {
    val mc = magTable.apply(ctx, guideProbe).get

    def go(winners: List[(SiderealTarget, GuideSpeed)]): Unit = {
      val best:Option[(SiderealTarget, GuideSpeed)] = winners match {
        case Nil => None
        case lst => strategy.params.brightest(lst)(_._1)
      }

      val all = winners.map(_._1) ++ unusable
      val res = strategy.select(ctx, magTable, all)

      def equalPosAngles(e: Angle, a: Angle): Unit =
        assertEquals("Position angles do not match", e.toDegrees, a.toDegrees, 0.000001)

      def expectNothing(): Unit =
        res match {
          case None                                       => // ok
          case Some(AgsStrategy.Selection(posAngle, Nil)) =>
            equalPosAngles(ctx.getPositionAngle, posAngle)
          case Some(AgsStrategy.Selection(_,        asn)) =>
              fail("Expected nothing but got: " + asn.map { a =>
                s"(${a.guideStar.toString}, ${a.guideProbe})"
              }.mkString("[", ", ", "]"))
        }

      def expectSingleAssignment(expStar: SiderealTarget, expSpeed: GuideSpeed): Unit = {
        res match {
          case None      =>
            fail(s"Expected: ($expStar, $expSpeed), but nothing selected")
          case Some(AgsStrategy.Selection(posAngle, asn)) =>
            equalPosAngles(ctx.getPositionAngle, posAngle)
            asn match {
              case List(AgsStrategy.Assignment(actProbe, actStar)) =>
                assertEquals(guideProbe, actProbe)
                assertEqualTarget(expStar, actStar)
                strategy.params.referenceMagnitude(actStar).foreach { mag =>
                  val actSpeed = AgsMagnitude.fastestGuideSpeed(mc, mag, ctx.getConditions)
                  assertTrue(s"Expected: $expSpeed , actual: $actSpeed", actSpeed.contains(expSpeed))
                }
              case Nil => fail(s"Expected: ($expStar, $expSpeed), but nothing selected")
              case _   => fail(s"Multiple guide probe assignments: $asn")
            }
        }
      }

      best.fold(expectNothing()) { (expectSingleAssignment _).tupled }

      val remaining = best.map { case (so, gs) => winners.diff(List((so, gs)))}.toList.flatten
      if (best.isDefined) go(remaining)
    }

    go(usable)
  }
}
