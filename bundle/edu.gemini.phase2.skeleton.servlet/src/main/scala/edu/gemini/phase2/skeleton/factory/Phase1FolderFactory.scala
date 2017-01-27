package edu.gemini.phase2.skeleton.factory

import edu.gemini.spModel.gemini.graces.blueprint.SpGracesBlueprint
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality

import scala.collection.JavaConverters._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.model.p1.immutable._
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.template._
import edu.gemini.spModel.gemini.gnirs.blueprint.SpGnirsBlueprintSpectroscopy
import edu.gemini.spModel.gemini.nifs.blueprint.SpNifsBlueprintBase
import edu.gemini.shared.util.TimeValue
import edu.gemini.spModel.gemini.flamingos2.blueprint.SpFlamingos2BlueprintLongslit
import edu.gemini.spModel.core

object Phase1FolderFactory {

  case class ObsQuad(bName: String, target: SPTarget, siteQuality: SPSiteQuality, time: TimeValue)

  object Folder {
    def empty(site: core.Site) = new Folder(site, new Namer, Map.empty, Nil)
  }

  implicit def RightProjection[A,B](v: Either[A,B]): Either.RightProjection[A,B] = v.right

  case class Folder(site: core.Site,
                    namer: Namer,
                    bMap: Map[String, SpBlueprint],
                    oList: List[ObsQuad]) {

    def add(o: Observation, time: Long): Either[String, Folder] =
      for {
        blueprintE  <- extractBlueprintEntry(o).right
        target      <- extractTarget(site, o, time).right
        siteQuality <- extractSiteQuality(o).right
        timeValue   <- extractIntTime(o).right
      } yield Folder(site, namer,
                bMap + blueprintE,
                ObsQuad(blueprintE._1, target, siteQuality, timeValue) :: oList)

    private def extractBlueprintEntry(o: Observation): Either[String, (String, SpBlueprint)] =
      for {
        b1 <- o.blueprint.toRight("Observation missing instrument resources").right
        b2 <- SpBlueprintFactory.create(b1).right
        bn = namer.nameOf(b1)
      } yield bn -> bMap.getOrElse(bn, b2)

    private def extractTarget(s: core.Site, o: Observation, time: Long): Either[String, SPTarget] =
      for {
        t1 <- o.target.toRight("Observation missing target").right
        t2 <- SpTargetFactory.create(s, t1, time).right
      } yield t2

    private def extractSiteQuality(o: Observation): Either[String, SPSiteQuality] =
      for {
        s1 <- o.condition.toRight("Observation missing conditions").right
        s2 <- SpSiteQualityFactory.create(s1).right
      } yield s2

    private def extractIntTime(o: Observation): Either[String, TimeValue] =
      for {
        time <- o.intTime.toRight("Observation missing integration time").right
      } yield new TimeValue(time.toHours.hours, TimeValue.Units.hours)


    def toPhase1Folder: Phase1Folder = {
      val groups = oList.groupBy(_.bName).toList map {
        case (blueprintId, args) =>
          val templateArgs = args map { arg => TemplateParameters.newInstance(arg.target, arg.siteQuality, arg.time)}
          new Phase1Group(blueprintId, templateArgs.asJava)
      }

      // Some kinds of blueprints need to be parameterized by the H-magnitude of
      // their targets, which kind of sucks. Sorry.

      val groups0 = groups.flatMap { pig =>
        bMap(pig.blueprintId) match {
          case _: SpGnirsBlueprintSpectroscopy  => GnirsSpectroscopyPartitioner.partition(pig)
          case _: SpNifsBlueprintBase           => NifsPartitioner.partition(pig)
          case _: SpFlamingos2BlueprintLongslit => F2LongslitPartitioner.partition(pig)
          case _: SpGracesBlueprint             => GracesPartitioner.partition(pig)
          case _                                => List(pig)
        }
      }

      new Phase1Folder(bMap.asJava, groups0.asJava)
    }
  }


  // If there is an itac acceptance, then use its band assignment.  Otherwise
  // just figure we will use the "normal" band 1/2 observations.
  private def band(proposal: Proposal): Band =
    (for {
      itac   <- proposal.proposalClass.itac
      accept <- itac.decision.right.toOption
    } yield accept.band).getOrElse(1) match {
      case 3 => Band.BAND_3
      case _ => Band.BAND_1_2
    }

  def create(site: core.Site, proposal: Proposal): Either[String, Phase1Folder] = {
    val empty: Either[String, Folder] = Right(Folder.empty(site))

    val b       = band(proposal)
    val time    = proposal.semester.midPoint
    val efolder = (empty/:proposal.observations.filter(obs => obs.band == b && obs.enabled)) { (e, obs) =>
      e.right flatMap { _.add(obs, time) }
    }

    efolder.right map { _.toPhase1Folder }
  }
}


trait Partitioner {
  def partition(pig: Phase1Group): List[Phase1Group] = {
    val argsLists = pig.argsList.asScala.toList.groupBy(args => bucket(args.getTarget)).map(_._2.asJava).toList
    argsLists.map(args => new Phase1Group(pig.blueprintId, args))
  }
  def bucket(t:SPTarget):Int
}

object GnirsSpectroscopyPartitioner extends Partitioner {
  import edu.gemini.spModel.core.MagnitudeBand.H
  def bucket(t:SPTarget):Int = t.getMagnitude(H).map(_.value).map { H =>
    if (H < 11.5) 1
    else if (H < 16) 2
    else if (H < 20) 3
    else 4
  }.getOrElse(5)
}

// TARGET BRIGHTNESS = TB
// Use K magnitude from target information if available:
// IF      K <= 9  then BT = True   # Bright Target
// IF  9 < K <= 13 then MT = True   # Moderate Target
// IF 13 < K <= 20 then FT = True   # Faint Target
// IF 20 < K       then BAT = True  # Blind acquisition target

object NifsPartitioner extends Partitioner {
  import edu.gemini.spModel.core.MagnitudeBand.K
  def bucket(t:SPTarget):Int = t.getMagnitude(K).map(_.value).map { K =>
         if (K <= 9) 1
    else if (K <= 13) 2
    else if (K <= 20) 3
    else 4
  }.getOrElse(5) // targets with no K-band are treated differently
}

//IF TARGET H-MAGNITUDE < 7 INCLUDE {13} # Bright, no sky subtraction
//IF TARGET H-MAGNITUDE > 7 INCLUDE {14} # Faint, with sky subtraction
//ELSE INCLUDE {13,14}                   # Unknown mag so include both acq templates

object F2LongslitPartitioner extends Partitioner {
  import edu.gemini.spModel.core.MagnitudeBand.H
  def bucket(t: SPTarget): Int = t.getMagnitude(H).map(_.value).map { h =>
    if (h <= 12.0) 1 else 2
  }.getOrElse(3)
}

// R = Phase-I target R-band or V-band magnitude
//... ELIF FIBER-MODE == 1 AND READ-MODE == Slow:
//IF   R> 10 INCLUDE {3}
//ELIF R<=10 INCLUDE {4}
//ELSE       INCLUDE {3,4}
object GracesPartitioner extends Partitioner {
  import edu.gemini.spModel.core.MagnitudeBand.{ R, V }
  def bucket(t:SPTarget):Int =
    (t.getMagnitude(R) orElse
     t.getMagnitude(V)).map(_.value).map { mag =>
         if (mag > 10) 1 else 2
     }.getOrElse(3) // no R/V-mag is treated differently
}
