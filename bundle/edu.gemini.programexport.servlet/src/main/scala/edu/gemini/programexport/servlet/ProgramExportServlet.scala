package edu.gemini.programexport.servlet

import java.security.Principal
import java.util.logging.{Level, Logger}
import javax.servlet.ServletException
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.{BandsList, BlackBody, EmissionLine, GaussianSource, LibraryNonStar, LibraryStar, Magnitude, NonSiderealTarget, PointSource, PowerLaw, SPProgramID, SiderealTarget, Target, UniformSource, UserDefinedSpectrum}
import edu.gemini.pot.sp.{ISPGroup, ISPNode, ISPObsComponent, ISPObsQaLog, ISPObservation, ISPProgram, ISPSeqComponent, SPComponentType}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.data.YesNoType
import edu.gemini.spModel.gemini.obscomp.{SPProgram, SPSiteQuality}
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obscomp.{SPGroup, SPNote}
import edu.gemini.spModel.rich.pot.sp._

import scala.collection.JavaConverters._
import argonaut.Json
import argonaut.Json.JsonAssoc
import argonaut._
import Argonaut._
import scalaz._
import Scalaz._
import edu.gemini.ags.api.AgsMagnitude
import edu.gemini.ags.api.AgsMagnitude.MagnitudeTable
import edu.gemini.ags.conf.ProbeLimitsTable
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator
import edu.gemini.spModel.seqcomp.SeqBase
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.{Asterism, GuideEnv, GuideGroup, GuideGrp, OptsList, TargetEnvironment}
import edu.gemini.spModel.target.obsComp.TargetObsComp

import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._

final case class ProgramExportServlet(odb: IDBDatabaseService, user: Set[Principal]) extends HttpServlet {

  import ProgramExportServlet._

  override def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit =
    doRequest(request, response)

  override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit =
    doRequest(request, response)

  // Attempt to retrieve the program name.
  def doRequest(request: HttpServletRequest, response: HttpServletResponse): Unit =
    ParamParser(request).id.fold(t => throw new ServletException(t), build(_, response))

  // Build the JSON in a bottom-up approach starting at ISPProgram.
  // Traverse a node's children and on the way back up, add the information for the node.
  def build(id: SPProgramID, response: HttpServletResponse): Unit =
    Option(odb.lookupProgramByID(id)) match {
      case Some(ispProgram) =>
        // Start traversing the tree.
        response.setStatus(HttpServletResponse.SC_OK)
        response.setContentType("text/json; charset=UTF-8")
        val writer = response.getWriter
        // TODO: is this the right way to do this?
        process(ispProgram).foreach { case (k, j) => writer.write(s"{$k : ${j.spaces2}\n") }
        writer.close()

      case None =>
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, s"Program $id is not in the database!")
    }

  // Recursive method to build up the JSON representation of the program.
  // TODO: I am assuming on ISPProgram that we need to call getObservations and getObsComponents
  // TODO: at some point, as the observations and obs components are not
  def process(n: ISPNode): Option[JsonAssoc] = {
    n match {
      case _: ISPProgram | _: ISPGroup | _: ISPObservation | _: ISPObsComponent | _: ISPObsQaLog =>
        simpleNode(n)

      case _: ISPSeqComponent =>
        sequenceNode(n)

      case _ =>
        None
    }
  }

  // Shouldn't processing all the children like this take care of what you would get from
  // getObservations and getObsComponents on ISPProgram? That information is not appearing.
  def simpleNode(n: ISPNode): Option[JsonAssoc] = {
    for {
      t <- n.dataObject.map(_.getType)
      j <- componentFields(n)
    } yield (
      t.name,
      n.children.foldLeft(("key" := n.getNodeKey.toString) ->: j) { (jp, c) =>
        process(c) ->?: jp
      }
    )
  }

  def componentFields(n: ISPNode): Option[Json] =
    n.dataObject.flatMap {
      case p: SPProgram => Some(programFields(p))
      case n: SPNote => Some(noteFields(n))
      case g: SPGroup => Some(groupFields(n, g))
      case o: SPObservation => Some(observationFields(n.asInstanceOf[ISPObservation], o))
      case s: SPSiteQuality => Some(siteQualityFields(s))
      case t: TargetObsComp => Some(asterismFields(n.asInstanceOf[ISPObsComponent], t))
      // TODO: What else is needed here? Obslog data? Where is that?

      case _ => None

    }

  def magnitudeFields(magnitudes: List[Magnitude]): Json =
    magnitudes.foldLeft(jEmptyObject) { case (j, m) =>
      ("band" := m.band.name) ->:
        ("value" := m.value) ->:
        ("system" := m.system.name) ->:
        ("error" :=? m.error) ->?:
        j
    }

  def targetFields(spTarget: SPTarget, te: TargetEnvironment): JsonAssoc = {
    val target = spTarget.getTarget

    // Spectral distribution
    val sd = "spectralDistribution" :=? (Target.spectralDistribution.get(target).join match {
      case Some(BlackBody(t)) =>
        Some(("type" := "blackBody") ->: ("temperature" := t) ->: jEmptyObject)
      case Some(PowerLaw(idx)) =>
        Some(("type" := "powerLaw") ->: ("index" := idx) ->: jEmptyObject)
      case Some(EmissionLine(wl, w, f, c)) =>
        Some(("type" := "emissionLine") ->: ("wavelength" := wl.length.toMicrons) ->: ("width" := w.value) ->: ("flux" := f.value) ->: ("continuum" := c.value) ->: jEmptyObject)
      case Some(UserDefinedSpectrum(_, _)) =>
        Some(("type" := "userDefined") ->: jEmptyObject)
      case Some(s: LibraryStar) =>
        Some(("type" := "libraryStar") ->: ("sedSpectrum" := s.sedSpectrum) ->: jEmptyObject)
      case Some(n: LibraryNonStar) =>
        Some(("type" := "libraryNonStar") ->: ("label" := n.label) ->: ("sedSpectrum" := n.sedSpectrum) ->: jEmptyObject)
      case _ =>
        None
    })

    // Spatial profile
    val sp = "spatialProfile" :=? (Target.spatialProfile.get(target).join match {
      case Some(PointSource) =>
        Some(("type" := "pointSource") ->: jEmptyObject)
      case Some(UniformSource) =>
        Some(("type" := "uniformSource") ->: jEmptyObject)
      case Some(GaussianSource(fwhm)) =>
        Some(("type" := "gaussianSource") ->: ("fwhm" := fwhm) ->: jEmptyObject)
      case _ =>
        None
    })

    // Tag for sidereal vs nonsidereal.
    val tag = target.isSidereal ? "sidereal" | "nonsidereal"

    // Determine the target type.
    val targetType = {
      if (te.getUserTargets.asScala.map(_.target).contains(spTarget))
        te.getUserTargets.asScala.find(_.target == spTarget).map(_.`type`.displayName).get
      else if (te.getGuideEnvironment.containsTarget(spTarget)) {
        "GuideStar"
      } else
        "Base"
    }

    "target" := (target match {
      case SiderealTarget(name, coordinates, properMotion, _, _, magnitudes, spectralDistribution, spatialProfile) =>
        ("name" := name) ->:
          ("tag" := tag) ->:
          ("type" := targetType) ->:
          ("ra" := coordinates.ra.toAngle.formatHMS) ->:
          ("dec" := coordinates.dec.toAngle.formatDMS) ->:
          ("deltara" :=? properMotion.map(_.deltaRA.velocity.masPerYear)) ->?:
          ("deltadec" :=? properMotion.map(_.deltaDec.velocity.masPerYear)) ->?:
          ("epoch" :=? properMotion.map(_.epoch.year)) ->?:
          ("magnitudes" := magnitudeFields(magnitudes)) ->:
          sd ->?: sp ->?:
          jEmptyObject
      case NonSiderealTarget(name, _, horizonsDesignation, magnitudes, spectralDistribution, spatialProfile) =>
        ("name" := name) ->:
          ("tag" := tag) ->:
          ("type" := targetType) ->:
          ("des" :=? horizonsDesignation.map(_.des)) ->?:
          ("magnitudes" := magnitudeFields(magnitudes)) ->:
          sd ->?: sp ->?:
          jEmptyObject
    })
  }

  def asterismFields(oc: ISPObsComponent, t: TargetObsComp): Json = {
    // Needed for guide speed. May not include this as per discussion with Bryan.
    val ispObs = oc.getContextObservation
    val obsCtx = ObsContext.create(ispObs)
    val te = t.getTargetEnvironment
    val a = te.getAsterism

    // Base information: we are only dealing with asterisms with a single target.
    val base = a.asInstanceOf[Asterism.Single]
    val baseInfo = "base" := targetFields(base.t, te) ->: jEmptyObject

    // User target information.
    val userTargetInfo = te.getUserTargets.asScala.foldLeft(jEmptyObject) { (j, t) =>
      ("target" := targetFields(t.target, te)) ->: j
    }

    // Guide group information.



      // Guide groups.
//      val guideEnv = te.getGuideEnvironment.guideEnv
//      guideEnv.groups.foldLeft(jEmptyObject){(j, grp) =>
//        val primary = grp === guideEnv.primaryGroup
//        val tgtMap = GuideGrp.TargetCollectionGuideGroup.targets(grp)
//        tgtMap.values.map { xyz => xyz.map {  }}
//        grp.referencedGuiders.
//      }

      // Guide groups.
//      def guideGroups: Option[Json] = target match {
//        case t:SiderealTarget =>
//          var xyz =
//          Some()
//        case _: => None
//        val target: SiderealTarget = ???
//        val primaryGuiders = gg.getPrimaryReferencedGuiders.asScala.toSet
//        val magOpt = gg.getReferencedGuiders.asScala.foldLeft(jEmptyObject) { (j, gp) =>primaryGuiders.con
//          ("guiderKey" := gp.getKey) ->:
//          ("guiderPrimary" := primaryGuiders.contains(gp).toYesNo.displayValue) ->: j
//          val key = gp.getKey
//          val isPrimary = primaryGuiders.contains(gp)
//          gp.getBands.extract(target)
//        }
//        val guideSpeed = AgsMagnitude.fastestGuideSpeed(MagTable, )
//        jEmptyObject
//      }

//      val ge = te.getGroups.asScala.foldLeft(jEmptyObject){ case (j, g) =>
//        g
//        ("name" := g.getName.getOrElse("Unnamed")) ->: j
//
//      }

    baseInfo ->: ("targets" := userTargetInfo) ->: jEmptyObject
  }

  def programFields(p: SPProgram): Json = {
    val timeAcctAllocation = p.getTimeAcctAllocation
    ("piFirstName" := p.getPIFirstName) ->:
      ("piLastName" := p.getPILastName) ->:
      ("piEmail" := p.getPIInfo.getEmail) ->:
      ("investigators" := p.getGsaPhase1Data.getCois.asScala.map { i =>
        ("investigatorFirstName" := i.getFirst) ->:
          ("investigatorLastName" := i.getLast) ->:
          ("investigatorEmail" := i.getEmail) ->:
          jEmptyObject
      }.foldLeft(jEmptyObject) { (j, inv) =>
        (s"investigator" := inv) ->: j
      }) ->:
      ("affiliate" :=? Option(p.getPIAffiliate).map(_.displayValue)) ->?:
      ("queueBand" := p.getQueueBand) ->:
      ("rolloverFlag" := p.getRolloverStatus) ->:
      ("isThesis" := p.isThesis.toYesNo.displayValue) ->:
      ("programMode" := p.getProgramMode.displayValue) ->:
      ("tooType" := p.getTooType.getDisplayValue) ->:
      ("awardedTime" := p.getAwardedProgramTime.getMilliseconds) ->:
      ("timeAccountAllocationCategories" := timeAcctAllocation.getCategories.asScala.foldLeft(Json.jEmptyObject) { case (j, c) =>
        val award = timeAcctAllocation.getAward(c)
        ("category" := c.getDisplayName) ->: (
          ("programTime" := TimeUnit.SECONDS.toMillis(award.getProgramAward.getSeconds)) ->:
            ("partnerTime" := TimeUnit.SECONDS.toMillis(award.getPartnerAward.getSeconds)) ->:
            j)
      }) ->:
      Json.jEmptyObject
  }

  def noteFields(n: SPNote): Json =
    ("title" := n.getTitle) ->: ("text" := n.getNote) ->: jEmptyObject

  def siteQualityFields(s: SPSiteQuality): Json = {
    // TODO: there has to be a more elegant way to do this.
    // TODO: Basically, if there are no timing windows for an observation, omit the timingWindow section.
    val twOptList = Option(s.getTimingWindows.asScala).filter(_.nonEmpty).map(_.toList)
    val twInfo = "timingWindows" :=? twOptList.map { _.map { tw =>
      ("start" := tw.getStart) ->:
        ("duration" := tw.getDuration) ->:
        ("period" := tw.getPeriod) ->:
        ("repeat" := tw.getRepeat) ->:
        jEmptyObject
    }.foldLeft(jEmptyObject){ (j, tw) => ("timingWindow" := tw) ->: j} }

    ("cc" := s.getCloudCover.displayValue) ->:
      ("sb" := s.getSkyBackground.displayValue) ->:
      ("iq" := s.getImageQuality.displayValue) ->:
      ("wv" := s.getWaterVapor.displayValue) ->:
      ("elevationConstraintType" := s.getElevationConstraintType.displayValue) ->:
      ("elevationConstraintMin" := s.getElevationConstraintMin) ->:
      ("elevationConstraintMax" := s.getElevationConstraintMax) ->:
      twInfo ->?:
      jEmptyObject
  }

  def groupFields(n: ISPNode, g: SPGroup): Json =
    ("name" := g.getTitle) ->:
      ("key" := n.getNodeKey.toString) ->:
      jEmptyObject

  def observationFields(n: ISPObservation, o: SPObservation): Json = {
    n.children
    val times = PlannedTimeCalculator.instance.calc(n)

    ("title" := o.getTitle) ->:
      ("phase2Status" := o.getPhase2Status.displayValue) ->:
      ("execStatusOverride)" :=? o.getExecStatusOverride.asScalaOpt.map(_.displayValue)) ->?:
      ("priority" := o.getPriority.displayValue) ->:
      ("tooOverrideRapid" := o.isOverrideRapidToo.toYesNo.displayValue) ->:
      ("setupTimeType" := o.getSetupTimeType.toString) ->:
      // TODO: Need acquisition overhead here. Is this it?
      ("acquisitionOverhead" := times.toPlannedStepSummary.getSetupTime.reacquisitionOnlyTime.toMillis) ->:
      jEmptyObject
  }

  def sequenceNode(n: ISPNode): Option[JsonAssoc] =
    n.dataObject.flatMap {
      case _: SeqBase => Option(n.getContextObservation).map(sequence)
      case _          => None
    }

  def sequence(o: ISPObservation): JsonAssoc = {
    val steps = ConfigBridge
      .extractSequence(o, null, ConfigValMapInstances.TO_SEQUENCE_VALUE)
      .getAllSteps.toList

    "sequence" := steps.map {s =>
      s.itemEntries.toList.foldLeft(jEmptyObject) { (j, e) =>
        // TODO: e is an ItemEntry and the value should have been mapped to a String by TO_SEQUENCE_VALUE
        // TODO: I suspect this has something to do with Scala / Java Option since
        // TODO: TO_SEQUENCE_VALUE returns a Java Option<String>.
        (e.getKey.getPath := e.getItemValue.toString) ->: j
      }
    }
  }
}

object ProgramExportServlet {
  val Log: Logger = Logger.getLogger(getClass.getName)

  val IdParam: String = "id"
  case class ParamParser(req: HttpServletRequest) {
    def id: Throwable \/ SPProgramID =
      \/.fromTryCatchNonFatal(SPProgramID.toProgramID(req.getParameter(IdParam))).leftMap { t =>
        Log.log(Level.SEVERE, "Problem running ProgramExportServlet", t)
        t
      }
  }

  implicit class ToYesNo(val b: Boolean) extends AnyVal {
    def toYesNo: YesNoType =
      b ? YesNoType.YES | YesNoType.NO
  }

  val MagTable: MagnitudeTable = ProbeLimitsTable.loadOrThrow()
}
