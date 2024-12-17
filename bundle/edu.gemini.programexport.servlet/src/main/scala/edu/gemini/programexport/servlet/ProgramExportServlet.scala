package edu.gemini.programexport.servlet

import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.core.{AuxFileSpectrum, BlackBody, EmissionLine, GaussianSource, HorizonsDesignation, LibraryNonStar, LibraryStar, Magnitude, NonSiderealTarget, PointSource, PowerLaw, SPProgramID, SiderealTarget, SpatialProfile, SpectralDistribution, TooTarget, UniformSource, UserDefinedSpectrum}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow
import edu.gemini.spModel.gemini.obscomp.{SPProgram, SPSiteQuality}
import edu.gemini.spModel.gemini.phase1.GsaPhase1Data
import edu.gemini.spModel.guide._
import edu.gemini.spModel.obs.{ObsClassService, ObsQaStateService, ObsTimesService, ObservationStatus, SPObservation}
import edu.gemini.spModel.obs.plannedtime.{PlannedTimeCalculator, SetupTime}
import edu.gemini.spModel.obscomp.{SPGroup, SPInstObsComp, SPNote}
import edu.gemini.spModel.obslog._
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.seqcomp.SeqBase
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env._
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.timeacct.{TimeAcctAllocation, TimeAcctAward, TimeAcctCategory}
import argonaut.Argonaut._
import argonaut.Json.JsonAssoc
import argonaut._

import java.security.Principal
import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}
import javax.servlet.ServletException
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scalaz._
import Scalaz._
import edu.gemini.spModel.gemini.ghost.GhostAsterism
import edu.gemini.spModel.time.ChargeClass
import edu.gemini.spModel.util.SPTreeUtil

import java.time.Duration
import java.util.function

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
        // This feels hackish but if we don't wrap k in quotes, the PROGRAM_BASIC key does not get quotes and
        // the JSON cannot be parsed. It is a known bug that escape characters do not work in string interpolations
        // in this version of Scala.
        process(ispProgram).foreach { case (k, j) => writer.write(s"{${'"'}$k${'"'} : ${j.spaces2}}") }

        // This approach doesn't work at all.
        // process(ispProgram).foreach { out => writer.write(out.asJson.spaces2) }
        writer.close()
      case None =>
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, s"Program $id is not in the database!")
    }

  // Recursive method to build up the JSON representation of the program.
  def process(n: ISPNode): Option[JsonAssoc] =
    process_(n, 0)

  def process_(n: ISPNode, i: Int): Option[JsonAssoc] = {
    n match {
      case _: ISPProgram | _: ISPGroup | _: ISPObservation | _: ISPObsComponent | _: ISPObsQaLog =>
        simpleNode(n, i)

      case _: ISPSeqComponent =>
        sequenceNode(n)

      case _ =>
        None
    }
  }

  def simpleNode(n: ISPNode, i: Int): Option[JsonAssoc] = {
    for {
      o <- n.dataObject
      t = o.getType
      j <- componentFields(n)
    } yield (
      // We want to not append a suffix to program as it is unnecessary.
      // We also want to identify groups by their types in the JSON output code.
      o match {
        case _: SPProgram => t.name
        case gp: SPGroup => s"${t.name}_${(gp.getGroupType == SPGroup.GroupType.TYPE_FOLDER) ? "FOLDER" | "SCHEDULING"}-$i"
        case _ => s"${t.name}-$i"
      },
      n.children.zipWithIndex.foldLeft(("key" := n.getNodeKey.toString) ->: j) { case (jp, (c, x)) => process_(c, x) ->?: jp }
    )
  }

  def componentFields(n: ISPNode): Option[Json] =
    n.dataObject.flatMap {
      case p: SPProgram => Some(programFieldsEncodeJson(n.asInstanceOf[ISPProgram], p).asJson)
      case n: SPNote => Some(n.asJson)
      case g: SPGroup => Some(groupEncodeJson(n, g).asJson)
      case o: SPObservation => Some(observationFieldsEncodeJson(n.asInstanceOf[ISPObservation], o).asJson)
      case s: SPSiteQuality => Some(s.asJson)
      case t: TargetObsComp => Some(asterismEncodeJson(n.asInstanceOf[ISPObsComponent], t).asJson)
      case _ => None
    }

  implicit val magnitudeEncodeJson: EncodeJson[Magnitude] =
    EncodeJson { m =>
      ("system" := m.system.name) ->:
        ("value" := m.value) ->:
        ("name" := m.band.name) ->:
        jEmptyObject
    }

  implicit val investigatorsEncodeJson: EncodeJson[GsaPhase1Data.Investigator] =
    EncodeJson { i =>
      ("investigatorEmail" := i.getEmail) ->:
        ("investigatorLastName" := i.getLast) ->:
        ("investigatorFirstName" := i.getFirst) ->:
        jEmptyObject
    }

  val timeAccountAllocationEncodeJson: EncodeJson[(ISPProgram, TimeAcctAllocation)] =
    EncodeJson { case (p, t) =>
      // Get the total used program and partner time.
      val times = ObsTimesService.getCorrectedObsTimes(p)
      val totalProgramUsed = times.getTimeCharges.getTime(ChargeClass.PROGRAM)
      val totalPartnerUsed = times.getTimeCharges.getTime(ChargeClass.PARTNER)

      // Get the ratios used by each category for program and partner time.
      def getRatios(f: TimeAcctAward => Duration): Map[TimeAcctCategory, Double] = {
        t.getRatios(new function.Function[TimeAcctAward, Duration] {
          override def apply(t: TimeAcctAward): Duration = f(t)
        }).map{ case (c,d) => (c, d.toDouble)}.toMap
      }

      val programRatios = getRatios { _.getPartnerAward }
      val partnerRatios = getRatios { _.getProgramAward }

      def calculateRatio(category: TimeAcctCategory, ratios: Map[TimeAcctCategory, Double], totalTime: Long): Long =
        ratios.get(category).map { t => (t * totalTime).toLong }.getOrElse(0L)

      t.getCategories.asScala.toList.map(c => (c, t.getAward(c))).filter(_._2 != TimeAcctAward.ZERO).map {
        case (c, a) =>
          val progUsed = calculateRatio(c, programRatios, totalProgramUsed)
          val partUsed = calculateRatio(c, partnerRatios, totalPartnerUsed)
          ("usedProgramTime" := progUsed) ->:
            ("usedPartnerTime" := partUsed) ->:
            ("awardedPartnerTime" := TimeUnit.SECONDS.toMillis(a.getPartnerAward.getSeconds)) ->:
            ("awardedProgramTime" := TimeUnit.SECONDS.toMillis(a.getProgramAward.getSeconds)) ->:
            ("category" := c.getDisplayName) ->:
            jEmptyObject
      }.asJson
    }

  implicit val timingWindowEncodeJson: EncodeJson[TimingWindow] =
    EncodeJson { tw =>
      ("repeat" := tw.getRepeat) ->:
        ("period" := tw.getPeriod) ->:
        ("duration" := tw.getDuration) ->:
        ("start" := tw.getStart) ->:
        jEmptyObject
    }

  implicit val spatialProfileEncodeJson: EncodeJson[SpatialProfile] =
    EncodeJson {
      case PointSource =>
        ("type" := "pointSource") ->:
          jEmptyObject
      case UniformSource =>
        ("type" := "uniformSource") ->:
          jEmptyObject
      case GaussianSource(fwhm) =>
        ("fwhm" := fwhm) ->:
          ("type" := "gaussianSource") ->:
          jEmptyObject
    }

  implicit val spectralDistributionEncodeJson: EncodeJson[SpectralDistribution] =
    EncodeJson {
      case BlackBody(t) =>
        ("temperature" := t) ->:
          ("type" := "blackBody") ->:
          jEmptyObject
      case PowerLaw(idx) =>
        ("index" := idx) ->:
          ("type" := "powerLaw") ->:
          jEmptyObject
      case EmissionLine(wl, w, f, c) =>
        ("continuum" := c.value) ->:
          ("flux" := f.value) ->:
          ("width" := w.value) ->:
          ("wavelength" := wl.length.toMicrons) ->:
          ("type" := "emissionLine") ->:
          jEmptyObject
      case UserDefinedSpectrum(_, _) =>
        ("type" := "userDefined") ->:
          jEmptyObject
      case s: LibraryStar =>
        ("sedSpectrum" := s.sedSpectrum) ->:
          ("type" := "libraryStar") ->:
          jEmptyObject
      case n: LibraryNonStar =>
        ("sedSpectrum" := n.sedSpectrum) ->:
          ("label" := n.label) ->:
          ("type" := "libraryNonStar") ->:
          jEmptyObject
      case _: AuxFileSpectrum =>
        jEmptyObject
    }

  // Determine the target type.
  def targetType(te: TargetEnvironment, spTarget: SPTarget): String =
    if (te.getUserTargets.asScala.map(_.target).contains(spTarget))
      te.getUserTargets.asScala.find(_.target == spTarget).map(_.`type`.displayName).get
    else if (te.getGuideEnvironment.containsTarget(spTarget))
      "GuideStar"
    else
      "Base"

  // Unfortunately, we have to pass a lot of crap to this to get everything we need.
  val siderealTargetEncodeJson: EncodeJson[(TargetEnvironment, SPTarget, SiderealTarget, Option[Int])] =
    EncodeJson { case (te, sp, t, idx) =>
      ("spatialProfile" :=? t.spatialProfile) ->?:
        ("spectralDistribution" :=? t.spectralDistribution) ->?:
        ("magnitudes" := t.magnitudes) ->:
        ("epoch" :=? t.properMotion.map(_.epoch.year)) ->?:
        ("deltadec" :=? t.properMotion.map(_.deltaDec.velocity.masPerYear)) ->?:
        ("deltara" :=? t.properMotion.map(_.deltaRA.velocity.masPerYear)) ->?:
        ("dec" := t.coordinates.dec.toAngle.formatDMS) ->:
        ("ra" := t.coordinates.ra.toAngle.formatHMS) ->:
        ("type" := targetType(te, sp)) ->:
        ("tag" := "sidereal") ->:
        ("index" :=? idx) ->?:
        ("name" := t.name) ->:
        jEmptyObject
    }

  private def nonsiderealObjectType(t: NonSiderealTarget): Option[String] =
    t.horizonsDesignation.map {
      case HorizonsDesignation.Comet(_)            => "COMET"
      case HorizonsDesignation.AsteroidNewStyle(_) => "ASTEROID"
      case HorizonsDesignation.AsteroidOldStyle(_) => "ASTEROID"
      case HorizonsDesignation.MajorBody(_)        => "MAJORBODY"
    }

  val nonSiderealTargetEncodeJson: EncodeJson[(TargetEnvironment, SPTarget, NonSiderealTarget, Option[Int])] =
    EncodeJson { case (te, sp, t, idx) =>
      ("spatialProfile" :=? t.spatialProfile) ->?:
        ("spectralDistribution" :=? t.spectralDistribution) ->?:
        ("magnitudes" := t.magnitudes) ->:
        ("des" :=? t.horizonsDesignation.map(_.des)) ->?:
        ("nonsiderealObjectType" :=? nonsiderealObjectType(t)) ->?:
        ("horizonsQuery" :=? t.horizonsDesignation.map(_.queryString)) ->?:
        ("type" := targetType(te, sp)) ->:
        ("tag" := "nonsidereal") ->:
        ("index" :=? idx) ->?:
        ("name" := t.name) ->:
        jEmptyObject
    }

  val targetEncodeJson: EncodeJson[(TargetEnvironment, SPTarget, Option[Int])] =
    EncodeJson { case (te, sp, idx) =>
      sp.getTarget match {
        case st: SiderealTarget => siderealTargetEncodeJson(te, sp, st, idx).asJson
        case nst: NonSiderealTarget => nonSiderealTargetEncodeJson(te, sp, nst, idx).asJson
        case _: TooTarget => jEmptyObject
      }
    }

  // Unfortunately, we have to pass a lot of crap to this to get everything we need.
  val userTargetEncodeJson: EncodeJson[(TargetEnvironment, UserTarget)] =
    EncodeJson { case (te, ut) =>
      targetEncodeJson(te, ut.target, None).asJson
    }

  // For automatic groups, where there is only maximum one guide probe per target.
  val automaticGroupGuideProbeMapEncodeJson: EncodeJson[(TargetEnvironment, GuideProbe, SPTarget)] =
    EncodeJson { case (te, gp, sp) =>
      ("target" := targetEncodeJson(te, sp, None).asJson) ->:
        ("guideProbeKey" := gp.getKey) ->:
        jEmptyObject
    }

  // For manual groups, where there is a GuideProbe ==>> Options[OptsList[SPTarget]] with order.
  // This represents one guide probe and one OptsList[SPTarget]
  // The second last parameter is the index of the guide probe.
  // The last parameter is the index of the primary guide star.
  val manualGroupGuideProbeMapEntryEncodeJson: EncodeJson[(TargetEnvironment, GuideProbe, OptsList[SPTarget], Int, Option[Int])] =
    EncodeJson { case (te, gp, oolst, gpidx, pgsidx) =>
      ("targets" := oolst.toList.zipWithIndex.map { case (sp, idx) => targetEncodeJson(te, sp, Some(idx)) }) ->:
        ("primaryGuideStarIndex" :=? pgsidx) ->?:
        ("guideProbeIndex" := gpidx) ->:
        ("guideProbeKey" := gp.getKey) ->:
        jEmptyObject
    }

  // For manual groups, where there is a GuideProbe ==>> Options[OptsList[SPTarget]] with order.
  // This represents the whole structure GuideProbe ==>> Options[OptsList[SPTarget]].
  val manualGroupGuideProbeMapEncodeJson: EncodeJson[(TargetEnvironment, List[(GuideProbe, OptsList[SPTarget])])] =
    EncodeJson { case (te, gpmlst) =>
      ("guideProbe" := gpmlst.zipWithIndex.map {
        case ((gp, olst), gpidx) =>
          manualGroupGuideProbeMapEntryEncodeJson(te, gp, olst, gpidx, olst.focusIndex) }) ->:
        jEmptyObject
    }

  val manualGuideGroupEncodeJson: EncodeJson[(TargetEnvironment, ManualGroup)] =
    EncodeJson { case (te, mg) =>
      // Convert the GuideGroup ==>> Option[OptsList[SPTarget]] to List[(GuideGroup, OptsList[SPTarget])].
      val gplst = mg.targetMap.keys.map { gp => (gp, mg.targetMap.lookup(gp)) }.collect {
        case (gp, Some(sp)) => (gp, sp)
      }

      // Only print this out if there are guide groups.
      // Complex translation into Some / None to invoke the implicit.
      val gplistopt = Option(gplst).filter(_.nonEmpty).map(lst => (te, lst))

      ("guideProbes" :=? gplistopt.map(manualGroupGuideProbeMapEncodeJson(_))) ->?:
        ("primaryGroup" := (te.getGuideEnvironment.guideEnv.primaryGroup === mg)) ->:
        ("tag" := "manual") ->:
        ("name" := mg.name) ->:
        jEmptyObject
    }

  val automaticGroupEncodeJson: EncodeJson[(TargetEnvironment, AutomaticGroup)] =
    EncodeJson { case (te, grp) =>
      val gpm = grp.targetMap
      ("guideProbe" := gpm.keys.map(gp => (gp, gpm.lookup(gp))).collect {
        case (gp, Some(sp)) => automaticGroupGuideProbeMapEncodeJson(te, gp, sp)
      }) ->:
        ("primaryGroup" := (te.getGuideEnvironment.guideEnv.primaryGroup === grp)) ->:
        ("tag" := "auto") ->:
        ("name" := "auto") ->:
        jEmptyObject
    }

  val userTargetsEncodeJson: EncodeJson[(TargetEnvironment, List[UserTarget])] =
    EncodeJson { case (te, userTargetList) =>
      userTargetList.map(ut => userTargetEncodeJson(te, ut)).asJson
    }

  val guideGrpListEncodeJson: EncodeJson[(TargetEnvironment, List[GuideGrp])] =
    EncodeJson { case (te, lst) =>
      lst.map {
        case mg: ManualGroup => manualGuideGroupEncodeJson(te, mg).asJson
        case ag: AutomaticGroup => automaticGroupEncodeJson(te, ag).asJson
      }.asJson
    }

  // We include ISPObsComponent because if we expand with GuideSpeed in the future, we need an:
  // ObsContext.create(oc.getContextObservation).
  val asterismEncodeJson: EncodeJson[(ISPObsComponent, TargetObsComp)] =
    EncodeJson { case (_, targetObs) =>
      val te = targetObs.getTargetEnvironment
      val ge = te.getGuideEnvironment.guideEnv

      // Base information: we are only dealing with asterisms with a single target for now.
      val base: SPTarget = te.getAsterism match {
        case GhostAsterism.SingleTarget(t, _)                      => t.spTarget
        case GhostAsterism.DualTarget(t, _, _)                     => t.spTarget // use the first target
        case GhostAsterism.TargetPlusSky(t, _, _)                  => t.spTarget
        case GhostAsterism.SkyPlusTarget(_, t, _)                  => t.spTarget
        case GhostAsterism.HighResolutionTargetPlusSky(t, _, _, _) => t.spTarget
        case Asterism.Single(t)                                    => t
        case _                                                     => sys.error("Cannot happen") // Famous last words
      }

      ("guideGroups" := guideGrpListEncodeJson(te, ge.groups)) ->:
        ("primaryIndex" := ge.primaryIndex) ->:
        ("userTargets" := userTargetsEncodeJson(te, te.getUserTargets.asScala.toList)) ->:
        ("base" := targetEncodeJson(te, base, None)) ->:
        jEmptyObject
    }

  val programFieldsEncodeJson: EncodeJson[(ISPProgram, SPProgram)] =
    EncodeJson { case (ispProg, spProg) =>
      ("timeAccountAllocationCategories" := timeAccountAllocationEncodeJson(ispProg, spProg.getTimeAcctAllocation)) ->:
        ("awardedTime" := spProg.getAwardedProgramTime.getMilliseconds) ->:
        ("tooType" := spProg.getTooType.getDisplayValue) ->:
        ("programMode" := spProg.getProgramMode.displayValue) ->:
        ("isThesis" := spProg.isThesis) ->:
        ("rolloverFlag" := spProg.getRolloverStatus) ->:
        ("queueBand" := spProg.getQueueBand) ->:
        ("affiliate" :=? Option(spProg.getPIAffiliate).map(_.displayValue)) ->?:
        ("investigators" := spProg.getGsaPhase1Data.getCois.asScala.toList) ->:
        ("piEmail" := spProg.getPIInfo.getEmail) ->:
        ("piLastName" := spProg.getPILastName) ->:
        ("piFirstName" := spProg.getPIFirstName) ->:
        ("programId" := ispProg.getProgramID.toString) ->:
        jEmptyObject
    }

  implicit val noteEncodeJson: EncodeJson[SPNote] =
    EncodeJson { n =>
      ("text" := n.getNote) ->:
        ("title" := n.getTitle) ->:
        jEmptyObject
    }

  implicit val siteQualityEncodeJson: EncodeJson[SPSiteQuality] =
    EncodeJson { s =>
      ("elevationConstraintMax" := s.getElevationConstraintMax) ->:
        ("elevationConstraintMin" := s.getElevationConstraintMin) ->:
        ("elevationConstraintType" := s.getElevationConstraintType.displayValue) ->:
        ("timingWindows" := s.getTimingWindows.asScala.toList) ->:
        ("wv" := s.getWaterVapor.displayValue) ->:
        ("iq" := s.getImageQuality.displayValue) ->:
        ("sb" := s.getSkyBackground.displayValue) ->:
        ("cc" := s.getCloudCover.displayValue) ->:
        jEmptyObject
    }

  val groupEncodeJson: EncodeJson[(ISPNode, SPGroup)] =
    EncodeJson { case (n, g) =>
      ("key" := n.getNodeKey.toString) ->:
        ("name" := g.getTitle) ->:
        jEmptyObject
    }

  def observationSetupTime(ispObs: ISPObservation): Option[Duration] = {
    val spObs = ispObs.getDataObject.asInstanceOf[SPObservation]
    Option(SPTreeUtil.findInstrument(ispObs))
      .flatMap(_.dataObject)
      .map(_.asInstanceOf[SPInstObsComp]).map { i =>
      spObs.getSetupTimeType match {
        case SetupTime.Type.FULL => i.getSetupTime(ispObs)
        case SetupTime.Type.REACQUISITION => i.getReacquisitionTime(ispObs)
        case SetupTime.Type.NONE => Duration.ZERO
      }
    }
  }

  val observationFieldsEncodeJson: EncodeJson[(ISPObservation, SPObservation)] =
    // Time / duration in ms.
    EncodeJson { case (ispObs, spObs) =>
      ("obsLog" :=? Option(ObsLog.getIfExists(ispObs))) ->?:
        ("setupTimeType" := spObs.getSetupTimeType.toString) ->:
        ("setupTime" :=? observationSetupTime(ispObs).map(_.toMillis)) ->?:
        ("tooOverrideRapid" := spObs.isOverrideRapidToo) ->:
        ("priority" := spObs.getPriority.displayValue) ->:
        ("qaState" := ObsQaStateService.getObsQaState(ispObs).name) ->:
        ("obsStatus" := ObservationStatus.computeFor(ispObs).name) ->:
        ("phase2Status" := spObs.getPhase2Status.displayValue) ->:
        ("title" := spObs.getTitle) ->:
        // TODO: Do we want the header value here or the log value? I don't think we want the display value.
        ("obsClass" := ObsClassService.lookupObsClass(ispObs).headerValue) ->:
        ("observationId" := ispObs.getObservationID.toString) ->:
        jEmptyObject
    }

  implicit val obsLogEncodeJson: EncodeJson[ObsLog] =
    EncodeJson { obslog =>
      jArray(obslog.getDatasetLabels.toList.map { l =>
        val rec = obslog.getDatasetRecord(l)
        ("label" := rec.label.toString) ->:
          ("filename" := rec.exec.dataset.getDhsFilename) ->:
          ("qaState" := rec.qa.qaState.displayValue) ->:
          jEmptyObject
      })
    }

  def sequenceNode(n: ISPNode): Option[JsonAssoc] =
    n.dataObject.flatMap {
      case _: SeqBase => Option(n.getContextObservation).map(sequence)
      case _ => None
    }

  def sequence(o: ISPObservation): JsonAssoc = {
    val steps = ConfigBridge
      .extractSequence(o, null, ConfigValMapInstances.TO_SEQUENCE_VALUE)
      .getAllSteps.toList

    val pts = PlannedTimeCalculator.instance.calc(o).toPlannedStepSummary

    "sequence" := steps.zipWithIndex.map { case (s, idx) =>
      ("totalTime" := pts.getStepTime(idx)) ->:
        s.itemEntries.toList.foldLeft(jEmptyObject) { (j, e) =>
          // e is an ItemEntry and the value should have been mapped to a String by TO_SEQUENCE_VALUE.
          // I suspect this has something to do with Scala / Java Option since TO_SEQUENCE_VALUE returns a
          // Java Option<String>.
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
}
