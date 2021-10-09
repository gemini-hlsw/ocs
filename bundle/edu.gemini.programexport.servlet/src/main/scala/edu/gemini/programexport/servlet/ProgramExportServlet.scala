package edu.gemini.programexport.servlet

import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.core.{AuxFileSpectrum, BlackBody, EmissionLine, GaussianSource, LibraryNonStar, LibraryStar,
  Magnitude, NonSiderealTarget, PointSource, PowerLaw, SPProgramID, SiderealTarget, SpatialProfile,
  SpectralDistribution, TooTarget, UniformSource, UserDefinedSpectrum}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow
import edu.gemini.spModel.gemini.obscomp.{SPProgram, SPSiteQuality}
import edu.gemini.spModel.gemini.phase1.GsaPhase1Data
import edu.gemini.spModel.guide._
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator
import edu.gemini.spModel.obscomp.{SPGroup, SPNote}
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
        case gp: SPGroup  => s"${t.name}_${(gp.getGroupType == SPGroup.GroupType.TYPE_FOLDER) ? "FOLDER" | "SCHEDULING"}-$i"
        case _            => s"${t.name}-$i"
      },
      n.children.zipWithIndex.foldLeft(("key" := n.getNodeKey.toString) ->: j) { case (jp, (c, x)) => process_(c, x) ->?: jp }
    )
  }

  def componentFields(n: ISPNode): Option[Json] =
    n.dataObject.flatMap {
      case p: SPProgram => Some(p.asJson)
      case n: SPNote => Some(n.asJson)
      case g: SPGroup => Some((n, g).asJson)
      case o: SPObservation => Some((n.asInstanceOf[ISPObservation], o).asJson)
      case s: SPSiteQuality => Some(s.asJson)
      case t: TargetObsComp => Some((n.asInstanceOf[ISPObsComponent], t).asJson)
      case _ => None
    }

  implicit def MagnitudeEncodeJson: EncodeJson[Magnitude] =
    EncodeJson(m =>
      ("system" := m.system.name) ->: ("value" := m.value) ->: ("name" := m.band.name) ->: jEmptyObject
    )

  implicit def InvestigatorsEncodeJson: EncodeJson[GsaPhase1Data.Investigator] =
    EncodeJson(i =>
      ("investigatorEmail" := i.getEmail) ->:
        ("investigatorLastName" := i.getLast) ->:
        ("investigatorFirstName" := i.getFirst) ->:
        jEmptyObject
    )

  implicit def TimeAccountAward: EncodeJson[(TimeAcctCategory, TimeAcctAward)] =
    EncodeJson(p => {
      val (c, a) = p
      ("partnerTime" := TimeUnit.SECONDS.toMillis(a.getPartnerAward.getSeconds)) ->:
        ("programTime" := TimeUnit.SECONDS.toMillis(a.getProgramAward.getSeconds)) ->:
        ("category" := c.getDisplayName) ->:
        jEmptyObject}
    )
  
  implicit def TimeAccountAllocationEncodeJson: EncodeJson[TimeAcctAllocation] =
    EncodeJson(t =>
      t.getCategories.asScala.toList.map(c => (c, t.getAward(c))).filter(_._2 != TimeAcctAward.ZERO).asJson
    )

  implicit def TimingWindowEncodeJson: EncodeJson[TimingWindow] =
    EncodeJson(tw =>
      ("repeat" := tw.getRepeat) ->:
        ("period" := tw.getPeriod) ->:
        ("duration" := tw.getDuration) ->:
        ("start" := tw.getStart) ->:
        jEmptyObject
    )

  implicit def SpatialProfileEncodeJson: EncodeJson[SpatialProfile] =
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

  implicit def SpectralDistributionEncodeJson: EncodeJson[SpectralDistribution] =
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
  implicit def SiderealTargetEncodeJson: EncodeJson[(TargetEnvironment, SPTarget, SiderealTarget, Option[Int])] =
    EncodeJson(p => {
      val (te, sp, t, idx) = p
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
    })

  implicit def NonSiderealTargetEncodeJson: EncodeJson[(TargetEnvironment, SPTarget, NonSiderealTarget, Option[Int])] =
    EncodeJson(p => {
      val (te, sp, t, idx) = p
      ("spatialProfile" :=? t.spatialProfile) ->?:
        ("spectralDistribution" :=? t.spectralDistribution) ->?:
        ("magnitudes" := t.magnitudes) ->:
        ("des" :=? t.horizonsDesignation.map(_.des)) ->?:
        ("type" := targetType(te, sp)) ->:
        ("tag" := "nonsidereal") ->:
        ("index" :=? idx) ->?:
        ("name" := t.name) ->:
        jEmptyObject
    })

  implicit def TargetEncodeJson: EncodeJson[(TargetEnvironment, SPTarget, Option[Int])] =
    EncodeJson(p => {
      val (te, sp, idx) = p
      sp.getTarget match {
        case st: SiderealTarget     => (te, sp, st, idx).asJson
        case nst: NonSiderealTarget => (te, sp, nst, idx).asJson
        case _: TooTarget           => jEmptyObject
      }
    })

  // Unfortunately, we have to pass a lot of crap to this to get everything we need.
  implicit def UserTargetEncodeJson: EncodeJson[(TargetEnvironment, UserTarget)] =
    EncodeJson(p => {
      val (te, ut) = p
      (te, ut.target, None).asJson
    })

  // For automatic groups, where there is only maximum one guide probe per target.
  implicit def AutomaticGroupGuideProbeMapEncodeJson: EncodeJson[(TargetEnvironment, GuideProbe, SPTarget)] =
    EncodeJson(p => {
      val (te, gp, sp) = p
      ("target" := (te, sp, None).asJson) ->: ("guideProbeKey" := gp.getKey) ->: jEmptyObject
    })

  implicit def AutomaticGroupEncodeJson: EncodeJson[(TargetEnvironment, AutomaticGroup.Active)] =
    EncodeJson(p => {
      val (te, grp) = p
      val gpm = grp.targetMap
      ("guideProbe" := gpm.keys.map(gp => (gp, gpm.lookup(gp))).collect {
        case (gp, Some(sp)) => (te, gp, sp)
      }) ->:
        ("primaryGroup" := (te.getGuideEnvironment.guideEnv.primaryGroup === grp)) ->:
        ("tag" := "auto") ->:
        ("name" := "auto") ->:
        jEmptyObject
    })

  // For manual groups, where there is a GuideProbe ==>> Options[OptsList[SPTarget]] with order.
  // This represents one guide probe and one OptsList[SPTarget]
  // The second last parameter is the index of the guide probe.
  // The last parameter is the index of the primary guide star.
  implicit def ManualGroupGuideProbeMapEntryEncodeJson: EncodeJson[(TargetEnvironment, GuideProbe, OptsList[SPTarget], Int, Option[Int])] =
    EncodeJson( p => {
      val (te, gp, oolst, gpidx, pgsidx) = p
      ("targets" := oolst.toList.zipWithIndex.map { case (sp, idx) => (te, sp, Some(idx)) }) ->:
        ("primaryGuideStarIndex" :=? pgsidx) ->?:
        ("guideProbeIndex" := gpidx) ->:
        ("guideProbeKey" := gp.getKey) ->:
        jEmptyObject
    })

  // For manual groups, where there is a GuideProbe ==>> Options[OptsList[SPTarget]] with order.
  // This represents the whole structure GuideProbe ==>> Options[OptsList[SPTarget]].
  implicit def ManualGroupGuideProbeMapEncodeJson: EncodeJson[(TargetEnvironment, List[(GuideProbe, OptsList[SPTarget])])] =
    EncodeJson(p => {
      val (te, gpmlst) = p
      ("guideProbe" := gpmlst.zipWithIndex.map { case ((gp, olst), gpidx) => (te, gp, olst, gpidx, olst.focusIndex) }) ->:
        jEmptyObject
    })

  implicit def ManualGuideGroupEncodeJson: EncodeJson[(TargetEnvironment, ManualGroup)] =
    EncodeJson(p => {
      val (te, mg) = p
      // Convert the GuideGroup ==>> Option[OptsList[SPTarget]] to List[(GuideGroup, OptsList[SPTarget])].
      val gplst = mg.targetMap.keys.map { gp => (gp, mg.targetMap.lookup(gp)) }.collect {
        case (gp, Some(sp)) => (gp, sp)
      }

      // Only print this out if there are guide groups.
      // Complex translation into Some / None to invoke the implicit.
      val gplistopt = Option(gplst).filter(_.nonEmpty).map(lst => (te, lst))

      ("guideProbes" :=? gplistopt) ->?:
        ("primaryGroup" := (te.getGuideEnvironment.guideEnv.primaryGroup === mg)) ->:
        ("tag" := "manual") ->:
        ("name" := mg.name) ->:
        jEmptyObject
    })

  // A general encoding of a GuideGrp that has an implicit JSON encoder.
  // This works for T = UserTarget and T = ManualGroup.
  implicit def GeneralGuideGrpEncodeJson[T](implicit enc: EncodeJson[(TargetEnvironment, T)]): EncodeJson[(TargetEnvironment, List[T])] =
    EncodeJson( { case (te, lst) =>
      lst.map(t => (te, t)).asJson
    })

  // We include ISPObsComponent because if we expand with GuideSpeed in the future, we need an:
  // ObsContext.create(oc.getContextObservation).
  implicit def AsterismEncodeJson: EncodeJson[(ISPObsComponent, TargetObsComp)] =
    EncodeJson(p => {
      val (_, targetObs) = p

      val te = targetObs.getTargetEnvironment
      val ge = te.getGuideEnvironment.guideEnv

      // Base information: we are only dealing with asterisms with a single target for now.
      val asterism = te.getAsterism.asInstanceOf[Asterism.Single]

      ("guideGroups" := ge.groups.map {
        case mg: ManualGroup => "guideGroup" := (te, mg)
        case ag: AutomaticGroup.Active => "guideGroup" := (te, ag)
        case _ => "guideGroup" := "initial"
      }) ->:
        ("primaryIndex" := ge.primaryIndex) ->:
        ("userTargets" := (te, te.getUserTargets.asScala.toList)) ->:
        ("base" := (te, asterism.t, None)) ->:
        jEmptyObject
    })

  implicit def ProgramFieldsEncodeJson: EncodeJson[SPProgram] =
    EncodeJson(p =>
      ("timeAccountAllocationCategories" := p.getTimeAcctAllocation) ->:
        ("awardedTime" := p.getAwardedProgramTime.getMilliseconds) ->:
        ("tooType" := p.getTooType.getDisplayValue) ->:
        ("programMode" := p.getProgramMode.displayValue) ->:
        ("isThesis" := p.isThesis) ->:
        ("rolloverFlag" := p.getRolloverStatus) ->:
        ("queueBand" := p.getQueueBand) ->:
        ("affiliate" :=? Option(p.getPIAffiliate).map(_.displayValue)) ->?:
        ("investigators" := p.getGsaPhase1Data.getCois.asScala.toList) ->:
        ("piEmail" := p.getPIInfo.getEmail) ->:
        ("piLastName" := p.getPILastName) ->:
        ("piFirstName" := p.getPIFirstName) ->:
        jEmptyObject
    )

  implicit def NoteEncodeJson: EncodeJson[SPNote] =
    EncodeJson(n =>
      ("text" := n.getNote) ->:
        ("title" := n.getTitle) ->:
        jEmptyObject
    )

  implicit def SiteQualityEncodeJson: EncodeJson[SPSiteQuality] =
    EncodeJson(s =>
      ("elevationConstraintMax" := s.getElevationConstraintMax) ->:
        ("elevationConstraintMin" := s.getElevationConstraintMin) ->:
        ("elevationConstraintType" := s.getElevationConstraintType.displayValue) ->:
        ("timingWindows" := s.getTimingWindows.asScala.toList) ->:
        ("wv" := s.getWaterVapor.displayValue) ->:
        ("iq" := s.getImageQuality.displayValue) ->:
        ("sb" := s.getSkyBackground.displayValue) ->:
        ("cc" := s.getCloudCover.displayValue) ->:
        jEmptyObject
    )

  implicit def GroupEncodeJson: EncodeJson[(ISPNode, SPGroup)] =
    EncodeJson( { case (n, g) =>
      ("key" := n.getNodeKey.toString) ->:
        ("name" := g.getTitle) ->:
        jEmptyObject
    })

  implicit def ObservationFieldsEncodeJson: EncodeJson[(ISPObservation, SPObservation)] =
    EncodeJson( { case (ispObs, spObs) =>
      ("obsLog" :=? Option(ObsLog.getIfExists(ispObs))) ->?:
        ("totalTime" := PlannedTimeCalculator.instance.calc(ispObs).totalTime()) ->: // output in ms
        ("setupTimeType" := spObs.getSetupTimeType.toString) ->:
        ("tooOverrideRapid" := spObs.isOverrideRapidToo) ->:
        ("priority" := spObs.getPriority.displayValue) ->:
        ("execStatusOverride" :=? spObs.getExecStatusOverride.asScalaOpt.map(_.displayValue)) ->?:
        ("phase2Status" := spObs.getPhase2Status.displayValue) ->:
        ("title" := spObs.getTitle) ->:
        jEmptyObject
    })

  implicit def ObsLogEncodeJson: EncodeJson[ObsLog] = {
    EncodeJson(obslog => {
      jArray(obslog.getDatasetLabels.toList.map { l =>
        val rec = obslog.getDatasetRecord(l)
        ("label" := rec.label.toString) ->:
          ("filename" := rec.exec.dataset.getDhsFilename) ->:
          ("qaState" := rec.qa.qaState.displayValue) ->:
          jEmptyObject
      })
    })
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
