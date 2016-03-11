package edu.gemini.spModel.io.impl.migration
package to2016B

import edu.gemini.pot.sp.SPComponentType
import edu.gemini.spModel.core._

import edu.gemini.spModel.pio.xml.PioXmlFactory
import edu.gemini.spModel.pio.{Pio, ParamSet, Document, Version}
import edu.gemini.spModel.target.{TargetParamSetCodecs, SPTargetPio, SourcePio}
import edu.gemini.spModel.target.env.GuideGroup

import scala.collection.JavaConverters._

import PioSyntax._
import scalaz._
import Scalaz._

object To2016B extends Migration {

  val version = Version.`match`("2016B-1")
  val R0 = RightAscension.zero
  val D0 = Declination.zero

  implicit class MoreLensOps[A, B](lens: A @> B) {
    def ?:=(op: Option[B]): State[A, B] =
      op.fold(lens.mods(identity))(lens.assign(_))
  }

  val conversions: List[Document => Unit] = List(
    updateGuideEnvironment, updateSchedulingBlocks, updateTargets // order matters!
  )

  val fact = new PioXmlFactory

  private def updateGuideEnvironment(d: Document): Unit = {

    def disabledGroup: ParamSet =
      fact.createParamSet(GuideGroup.ParamSetName) <|
        (ps => Pio.addParam(fact, ps, "tag", GuideGroup.AutoDisabledTag.toString))

    def addEmptyGuideEnv(targetEnv: ParamSet): Unit = {
      val guideEnv = fact.createParamSet("guideEnv")

      Pio.addIntParam(fact, guideEnv, "primary", 0)
      guideEnv.addParamSet(disabledGroup)

      targetEnv.addParamSet(guideEnv)
    }

    def addDisabledAutomaticGroup(guideEnv: ParamSet): Unit = {
      val manualGroups = guideEnv.getParamSets("guideGroup").asScala.toList

      // Add the manual tag to all the existing groups.
      manualGroups.foreach { grp =>
        Pio.addParam(fact, grp, "tag", GuideGroup.ManualTag.toString)
      }

      // Increment primary group to account for the new auto group
      val primary = Pio.getIntValue(guideEnv, "primary", 0) + (manualGroups.nonEmpty ? 1 | 0)
      val param   = Option(guideEnv.getParam("primary")) | {
        fact.createParam("primary") <| guideEnv.addParam
      }
      param.setValue(primary.toString)

      // Remove all guide gropus.
      while (guideEnv.removeChild("guideGroup") != null) {}

      // Add them back in the correct order.
      (disabledGroup :: manualGroups).foreach(guideEnv.addParamSet)
    }

    val tes = for {
      obs <- d.findContainers(SPComponentType.OBSERVATION_BASIC)
      env <- obs.findContainers(SPComponentType.TELESCOPE_TARGETENV)
      ps  <- env.allParamSets.filter(_.getName == "targetEnv")
    } yield ps

    tes.foreach { paramSet =>
      Option(paramSet.getParamSet("guideEnv")).fold(addEmptyGuideEnv(paramSet))(addDisabledAutomaticGroup)
    }
  }

  // Add a scheduling block to nonsidereal observations that don't have one. A duration of 0ms means
  // that the planned time will be used by internal calculations, which is a reasonable default.
  def updateSchedulingBlocks(d: Document): Unit =
    for {
      (o, b) <- obsAndBases(d) if o.value("schedulingBlockStart").isEmpty
      date   <- b.value("validAt").map(SPTargetPio.parseDate)
    } {
      Pio.addLongParam(fact, o, "schedulingBlockStart", date.getTime)
      Pio.addLongParam(fact, o, "schedulingBlockDuration", 0L)
    }

  // Update targets to the new model
  def updateTargets(d: Document): Unit =
    allTargets(d).foreach { t =>

      // In order to determine the target type we need the system as well as the coordinates
      // because a TOO target is encoded as a sidereal target at the origin.
      val data: Option[(String, Coordinates)] =
        for {
          system <- t.value("system")
          ra     <- t.value("c1").flatMap(Angle.parseHMS(_).toOption).map(RightAscension.fromAngle)
          dec    <- t.value("c2").flatMap(Angle.parseDMS(_).toOption).flatMap(Declination.fromAngle)
        } yield (system, Coordinates(ra, dec))

      // Construct a new target
      val newTarget: Target =
        data.map {
          case ("J2000", Coordinates.zero) => TooTarget.empty
          case ("J2000",               cs) => sidereal(t, cs).exec(SiderealTarget.empty)
          case ("JPL minor body",      cs) => NonSiderealTarget.empty
          case ("MPC minor planet",    cs) => NonSiderealTarget.empty
          case ("Solar system object", cs) => NonSiderealTarget.empty
        }.map(common(t).exec) getOrElse sys.error("cannot determine target type!") // TODO: more info

      // Drop it into the paramset.
      // TODO: remove everything else!
      t.addParamSet(TargetParamSetCodecs.TargetParamSetCodec.encode("target", newTarget))

    }

  def common(ps: ParamSet): State[Target, Unit] =
    for {
      _ <- Target.name                ?:= ps.value("name")
      _ <- Target.spatialProfile       := SourcePio.profileFromParamSet(ps)
      _ <- Target.spectralDistribution := SourcePio.distributionFromParamSet(ps)
    // todo: mags
    } yield ()

  // Read a sidereal target.
  def sidereal(ps: ParamSet, cs: Coordinates): State[SiderealTarget, Unit] =
    for {
      _ <- SiderealTarget.coordinates   := cs
      _ <- SiderealTarget.properMotion  := Some(pm(ps).exec(ProperMotion.zero)).filterNot(_ == ProperMotion.zero)
      _ <- SiderealTarget.redshift     ?:= ps.double("rv").map(Redshift.fromRadialVelocityJava).map(Some(_))
      _ <- SiderealTarget.redshift     ?:= ps.double("z").map(Redshift(_)).map(Some(_))
      _ <- SiderealTarget.parallax      := ps.double("parallax").map(Parallax(_))
    } yield ()

  // Read proper motion
  def pm(ps: ParamSet): State[ProperMotion, Unit] =
    for {
      _ <- ProperMotion.epoch    ?:= ps.double("epoch").map(Epoch(_))
      _ <- ProperMotion.deltaRA  ?:= ps.double("pm1").map(AngularVelocity(_)).map(RightAscensionAngularVelocity(_))
      _ <- ProperMotion.deltaDec ?:= ps.double("pm2").map(AngularVelocity(_)).map(DeclinationAngularVelocity(_))
    } yield ()

//  // Add magnitude information to the target.
//  final ParamSet magCollectionPset = paramSet.getParamSet(MagnitudePio.MAG_LIST);
//  if (magCollectionPset != null) {
//    try {
//      spt.setMagnitudes(MagnitudePio.instance.toList(magCollectionPset));
//    } catch (final ParseException ex) {
//      LOGGER.log(Level.WARNING, "Could not parse target magnitudes", ex);
//    }
//  }
//

}



