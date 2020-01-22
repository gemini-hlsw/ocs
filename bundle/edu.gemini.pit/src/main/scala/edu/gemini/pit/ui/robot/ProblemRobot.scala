package edu.gemini.pit.ui.robot

import edu.gemini.pit.ui._
import action.AppPreferencesAction
import edu.gemini.model.p1.immutable._
import edu.gemini.pit.ui.editor.Institutions
import edu.gemini.pit.util.PDF
import edu.gemini.pit.catalog._
import edu.gemini.spModel.core.MagnitudeBand
import view.obs.{ObsListGrouping, ObsListView}
import edu.gemini.model.p1.visibility.TargetVisibilityCalc
import edu.gemini.pit.ui.view.tac.TacView
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.io.File

import edu.gemini.pit.model.{AppPreferences, Model}
import edu.gemini.pit.catalog.NotFound
import edu.gemini.pit.catalog.Error

import scalaz.{Band => _, _}
import Scalaz._

object ProblemRobot {

  class Problem(val severity: Severity,
                val description: String,
                val section: String,
                fix: => Unit) extends Ordered[Problem] {

    def compare(other: Problem) = Some(severity.compare(other.severity)).filter(_ != 0).getOrElse(description.compareTo(other.description))

    def apply() {
      fix
    }

  }

  object Severity extends Enumeration {
    // Order is important; severity increases
    val Error, Todo, Warning, Info = Value
  }

  type Severity = Severity.Value

  private implicit class pimpLong(val n: Long) extends AnyVal {
    def ms = n
    def secs = ms * 1000
    def mins = secs * 60
    def hours = mins * 60
    def days = hours * 24
  }
}

class ProblemRobot(s: ShellAdvisor) extends Robot {

  // Bring our related types into scope

  import ProblemRobot._

  val MaxAttachmentSize = 30 // in megabytes
  val MaxAttachmentSizeBytes = MaxAttachmentSize * 1000 * 1000 // kbytes as used on the phase1 backends

  // Our state
  type State = List[Problem]
  protected[this] val initialState = Nil

  override protected def refresh(m: Option[Model]) {
    state = m.map(m => new Checker(m.proposal, s.shell.file).all).getOrElse(Nil)
  }

  // TODO: factor this out better; it's a leftover from the original implementation
  private class Checker(p: Proposal, xml: Option[File]) {

    lazy val all = {
      val ps =
        List(noObs, nonUpdatedInvestigatorName, noPIPhoneNumber, invalidPIPhoneNumber, titleCheck, band3option, abstractCheck, categoryCheck,
          keywordCheck, attachmentCheck, attachmentValidityCheck, attachmentSizeCheck, missingObsDetailsCheck,
          duplicateInvestigatorCheck, ftReviewerOrMentor, ftAffiliationMismatch, band3Obs).flatten ++
          TimeProblems(p, s).all ++
          TimeProblems.noCFHClassical(p, s) ++
          TimeProblems.partnerZeroTimeRequest(p, s) ++
          TacProblems(p, s).all ++
          Semester2020AProblems(p, s).all ++ // Still apply to 2020B.
          Semester2020BProblems(p, s).all ++
          List(incompleteInvestigator, missingObsElementCheck, emptyTargetCheck,
            emptyEphemerisCheck, singlePointEphemerisCheck, initialEphemerisCheck, finalEphemerisCheck,
            badGuiding, cwfsCorrectionsIssue, badVisibility, iffyVisibility, minTimeCheck, wrongSite, band3Orphan2, gpiCheck, lgsIQ70Check, lgsGemsIQ85Check,
            lgsCC50Check, texesCCCheck, texesWVCheck, gmosWVCheck, gmosR600Check, f2MOSCheck, dssiObsolete, band3IQ, band3LGS, band3RapidToO, sbIrObservation).flatten
      ps.sorted
    }

    private def when[A](b: Boolean)(a: => A) = b option a

    private lazy val titleCheck = when(p.title.isEmpty) {
      new Problem(Severity.Todo, "Please provide a title.", "Overview", s.inOverview(_.title.requestFocus()))
    }

    private lazy val abstractCheck = when(p.abstrakt.isEmpty) {
      new Problem(Severity.Todo, "Please provide an abstract.", "Overview", s.inOverview(_.abstrakt.requestFocus()))
    }

    private lazy val categoryCheck = when(p.category.isEmpty) {
      new Problem(Severity.Todo, "Please select a science category.", "Overview", s.inOverview(_.category.peer.setPopupVisible(true)))
    }

    private lazy val keywordCheck = when(p.keywords.isEmpty) {
      new Problem(Severity.Todo, "Please provide keywords.", "Overview", s.inOverview(_.keywords.select.doClick()))
    }

    private lazy val attachmentCheck = when(p.meta.attachment.isEmpty) {
      new Problem(Severity.Todo, "Please provide a PDF attachment.", "Overview", s.inOverview(_.attachment.select.doClick()))
    }

    def extractInvestigator(i:PrincipalInvestigator) = (i.firstName, i.lastName, i.email, i.phone, i.status, i.address.institution)

    def duplicateInvestigators(investigators:List[Investigator]) = {
      val uniqueInvestigators = for {
        i <- investigators
        if i.isComplete
      } yield extractInvestigator(i.toPi)
      uniqueInvestigators.distinct.size != uniqueInvestigators.size
    }

    def similarInvestigators(investigators:List[Investigator]) = {
      val uniqueInvestigators = for {
        i <- investigators
        if i.isComplete
      } yield i.fullName.toLowerCase
      uniqueInvestigators.distinct.size != uniqueInvestigators.size
    }

    private val duplicateInvestigatorCheck = if (duplicateInvestigators(p.investigators.all)) {
      Some(new Problem(Severity.Error, "Please remove duplicates from the investigator list.", "Overview", s.inOverview(_.investigators.editPi())))
    } else if (similarInvestigators(p.investigators.all)) {
      Some(new Problem(Severity.Warning, "Please check for duplications in the investigator list.", "Overview", s.inOverview(_.investigators.editPi())))
    } else {
      None
    }

    private lazy val attachmentValidityCheck = for {
      a <- p.meta.attachment
      if !PDF.isPDF(xml, a)
    } yield new Problem(Severity.Error, s"File ${a.getName} does not exist or is not a PDF file.", "Overview", s.inOverview(_.attachment.select.doClick()))

    private lazy val attachmentSizeCheck = for {
      a <- p.meta.attachment
      if a.length() > MaxAttachmentSizeBytes
    } yield new Problem(Severity.Error, s"Attachment '${a.getName}' is larger than ${MaxAttachmentSize}MB.", "Overview", s.inOverview(_.attachment.select.doClick()))

    private lazy val emptyTargetCheck = for {
      t <- p.targets
      if t.isEmpty
      msg = s"""Target "${t.name}" appears to be empty."""
    } yield new Problem(Severity.Error, msg, "Targets", s.inTargetsView(_.edit(t)))


    private lazy val emptyEphemerisCheck = for {
      t @ NonSiderealTarget(_, n, e, _) <- p.targets
      if e.isEmpty
      msg = s"""Ephemeris for target "$n" is undefined."""
    } yield new Problem(Severity.Warning, msg, "Targets", s.inTargetsView(_.edit(t)))

    private lazy val singlePointEphemerisCheck = for {
      t @ NonSiderealTarget(_, n, e, _) <- p.targets
      if e.size == 1
      msg = s"""Ephemeris for target "$n" contains only one point; please specify at least two."""
    } yield new Problem(Severity.Warning, msg, "Targets", s.inTargetsView(_.edit(t)))

    lazy val dateFormat = DateTimeFormatter.ofPattern("yyyy-MMM-dd").withZone(ZoneId.of("UTC"))

    private lazy val initialEphemerisCheck = for {
      t @ NonSiderealTarget(_, n, e, _) <- p.targets
      if !e.isEmpty
      ds = e.map(_.validAt) if ds.size > 1
      dsMin = ds.min
      diff = dsMin - p.semester.firstDay
      if diff >= 1.days
      msg = if (diff < 2.days)
        s"""Ephemeris for target "$n" is undefined for ${dateFormat.format(Instant.ofEpochMilli(p.semester.firstDay))} UTC."""
      else {
        val lastDay = (dsMin < p.semester.lastDay + 1.days) ? (dsMin - 1.days) | p.semester.lastDay
        s"""Ephemeris for target "$n" is undefined between ${dateFormat.format(Instant.ofEpochMilli(p.semester.firstDay))} and ${dateFormat.format(Instant.ofEpochMilli(lastDay))} UTC."""
      }
    } yield new Problem(Severity.Warning, msg, "Targets", s.inTargetsView(_.edit(t)))

    private lazy val finalEphemerisCheck = for {
      t @ NonSiderealTarget(_, n, e, _) <- p.targets
      if e.nonEmpty
      ds = e.map(_.validAt) if ds.size > 1
      dsMax = ds.max
      diff = p.semester.lastDay - dsMax
      if diff >= 1.days
      msg = if (diff == 1.days)
        s"""Ephemeris for target "$n" is undefined for ${dateFormat.format(Instant.ofEpochMilli(p.semester.lastDay))} UTC."""
      else {
        val firstDay = (dsMax > p.semester.firstDay) ? (dsMax + 1.days) | p.semester.firstDay
        s"""Ephemeris for target "$n" is undefined between ${dateFormat.format(Instant.ofEpochMilli(firstDay))} and ${dateFormat.format(Instant.ofEpochMilli(p.semester.lastDay))} UTC."""
      }
    } yield new Problem(Severity.Warning, msg, "Targets", s.inTargetsView(_.edit(t)))

    def aoPerspectiveIsLgs(ao: AoPerspective): Boolean = ao match {
      case AoLgs => true
      case _     => false
    }

    def bpIsLgs(b: BlueprintBase): Boolean =
      aoPerspectiveIsLgs(b match {
        case a: GmosNBlueprintBase         => a.altair.ao
        case a: GnirsBlueprintImaging      => a.altair.ao
        case a: GnirsBlueprintSpectroscopy => a.altair.ao
        case a: NifsBlueprintAo            => a.altair.ao
        case a: NiriBlueprint              => a.altair.ao
        case a: GsaoiBlueprint             => a.ao
        case _                             => AoNone
      })

    // NOTE: This needs to be maintained for any future instruments that use GeMS.
    def bpIsGemsLgs(b: BlueprintBase): Boolean =
      b match {
        case a: GsaoiBlueprint => aoPerspectiveIsLgs(a.ao)
        case _                 => false
      }

    private val lgsIQ70Check = for {
      o  <- p.nonEmptyObservations
      c  <- o.condition
      b  <- o.blueprint
      if bpIsLgs(b) && (!bpIsGemsLgs(b)) && (!List(ImageQuality.IQ70, ImageQuality.BEST).contains(c.iq))
    } yield new Problem(Severity.Error, s"LGS requires IQ70 or better.", "Observations", s.inObsListView(o.band, _.Fixes.fixConditions(c)))

    private val lgsGemsIQ85Check = for {
      o <- p.nonEmptyObservations
      c <- o.condition
      b <- o.blueprint
      if bpIsGemsLgs(b) && (!List(ImageQuality.IQ85, ImageQuality.IQ70, ImageQuality.BEST).contains(c.iq))
    } yield new Problem(Severity.Error, s"GeMS LGS requires IQ85 or better.", "Observations", s.inObsListView(o.band, _.Fixes.fixConditions(c)))

    private val lgsCC50Check = for {
      o  <- p.nonEmptyObservations
      c  <- o.condition
      b  <- o.blueprint
      if bpIsLgs(b) && (c.cc != CloudCover.BEST)
    } yield new Problem(Severity.Error, s"LGS requires CC50 conditions.", "Observations", s.inObsListView(o.band, _.Fixes.fixConditions(c)))

    private val texesCCCheck = for {
      o  <- p.nonEmptyObservations
      c  <- o.condition
      b  <- o.blueprint
      if b.isInstanceOf[TexesBlueprint]
      if c.cc == CloudCover.ANY || c.cc == CloudCover.CC80
    } yield new Problem(Severity.Warning, s"TEXES is not recommended for worse than CC70.", "Observations", s.inObsListView(o.band, _.Fixes.fixConditions(c)))

    private val texesWVCheck = for {
      o  <- p.nonEmptyObservations
      c  <- o.condition
      b  <- o.blueprint
      if b.isInstanceOf[TexesBlueprint]
      if c.wv == WaterVapor.ANY
    } yield new Problem(Severity.Warning, s"TEXES is not recommended for worse than WV80.", "Observations", s.inObsListView(o.band, _.Fixes.fixConditions(c)))

    private val gmosWVCheck = for {
      o  <- p.nonEmptyObservations
      c  <- o.condition
      b  <- o.blueprint
      if b.isInstanceOf[GmosNBlueprintBase] || b.isInstanceOf[GmosSBlueprintBase]
      if c.wv != WaterVapor.ANY
    } yield new Problem(Severity.Warning, s"GMOS is usually unaffected by atmospheric water vapor.", "Observations", s.inObsListView(o.band, _.Fixes.fixConditions(c)))


    private def gmosNDisperser(b: BlueprintBase, d: GmosNDisperser) = b match {
      case gn: GmosNBlueprintSpectrosopyBase => gn.disperser == d
      case _                                 => false
    }
    private def gmosSDisperser(b: BlueprintBase, d: GmosSDisperser) = b match {
      case gs: GmosSBlueprintSpectrosopyBase => gs.disperser == d
      case _                                 => false
    }

    private val gmosR600Check = p.proposalClass match {
      case _: ClassicalProposalClass => Nil
      case _                         =>
        for {
          o <- p.nonEmptyObservations
          b <- o.blueprint
          if gmosNDisperser(b, GmosNDisperser.R600) || gmosSDisperser(b, GmosSDisperser.R600)
        } yield new Problem(Severity.Warning, s"The R600 is little used and may be difficult to schedule.", "Observations", s.inObsListView(o.band, _.Fixes.fixBlueprint(b)))
    }

    private val f2MOSCheck = for {
      o <- p.nonEmptyObservations
      b <- o.blueprint.collect { case mos: Flamingos2BlueprintMos => mos }
    } yield new Problem(Severity.Error, "Flamingos2 Multi-Object Spectroscopy is not offered.", "Observations", s.inObsListView(o.band, _.Fixes.fixBlueprint(b)))

    private val dssiObsolete = for {
      o <- p.nonEmptyObservations
      b <- o.blueprint
      if b.isInstanceOf[DssiBlueprint]
    } yield {
      val sol = if (b.site == Site.GN) "Use 'Alopeke instead for GN." else "Use Zorro instead for GS."
      new Problem(Severity.Error, s"DSSI is no longer offered at Gemini for 2019B. $sol", "Observations", s.inObsListView(o.band, _.Fixes.fixBlueprint(b)))
    }

    def isBand3(o: Observation) = o.band == Band.BAND_3 && (p.proposalClass match {
                  case q: QueueProposalClass if q.band3request.isDefined => true
                  case _                                                 => false
                })

    def isBand3(p: Proposal) = p.meta.band3OptionChosen && (p.proposalClass match {
                  case q: QueueProposalClass if q.band3request.isDefined => true
                  case _                                                 => false
                })

    private val band3IQ = for {
      o  <- p.nonEmptyObservations
      if isBand3(o)
      c  <- o.condition
      if c.iq == ImageQuality.BEST
    } yield new Problem(Severity.Warning, s"IQ20 observations are unlikely to be executed in Band-3.", "Band 3", s.inObsListView(o.band, _.Fixes.fixConditions(c)))

    private val band3LGS = for {
      o  <- p.nonEmptyObservations
      b  <- o.blueprint
      if bpIsLgs(b) && isBand3(o)
    } yield new Problem(Severity.Error, s"LGS cannot be scheduled in Band 3.", "Band 3", s.showObsListView(Band.BAND_3))

    def proposalToO(p: ProposalClass): Option[ToOChoice] = p match {
      case q: QueueProposalClass         => q.tooOption.some
      case l: LargeProgramClass          => l.tooOption.some
      case f: FastTurnaroundProgramClass => f.tooOption.some
      case _                             => None
    }

    private val band3RapidToO = for {
      o  <- p.nonEmptyObservations
      to <- proposalToO(p.proposalClass)
      if isBand3(o) && to == ToOChoice.Rapid
    } yield new Problem(Severity.Error, s"Rapid ToO observations cannot be scheduled in Band 3.", "Time Requests", s.showPartnersView())

    private val band3Obs = (!p.nonEmptyObservations.exists(_.band == Band.BAND_3) && isBand3(p)) option
      new Problem(Severity.Todo, s"Please create Band 3 observations with conditions, targets, and resources.", "Band 3", s.showObsListView(Band.BAND_3))

    def isIR(b: BlueprintBase): Boolean = b match {
      case _: GsaoiBlueprint                           => true
      case _: Flamingos2BlueprintBase                  => true
      case _: PhoenixBlueprint                         => true
      case _: NiciBlueprintBase                        => true
      case _: TrecsBlueprintBase                       => true
      case _: NiriBlueprint                            => true
      case _: GnirsBlueprintBase                       => true
      case _: NifsBlueprintBase                        => true
      case _: TexesBlueprint                           => true
      case SubaruBlueprint(SubaruInstrument.COMICS, _) => true
      case SubaruBlueprint(SubaruInstrument.FMOS, _)   => true
      case SubaruBlueprint(SubaruInstrument.IRCS, _)   => true
      case SubaruBlueprint(SubaruInstrument.MOIRCS, _) => true
      case _                                           => false
    }

    private val sbIrObservation = for {
      o  <- p.nonEmptyObservations
      b  <- o.blueprint
      c  <- o.condition
      if isIR(b) && !bpIsLgs(b) && c.sb != SkyBackground.ANY
    } yield new Problem(Severity.Warning, s"Infrared observations usually do not require background constraints.", "Observations", s.inObsListView(o.band, _.Fixes.fixConditions(c)))

    private val gpiCheck = {
      def gpiMagnitudesPresent(target: SiderealTarget):List[(Severity, String)] = {
        val requiredBands = Set(MagnitudeBand.I, MagnitudeBand.Y, MagnitudeBand.J, MagnitudeBand.H, MagnitudeBand.K)
        val observationBands = target.magnitudes.map(_.band).toSet
        ~(((requiredBands & observationBands) =/= requiredBands) option {List((Severity.Error, "The magnitude information in the GPI target component should include the bandpasses I, Y, J, H, and K."))})
      }

      def magAdjust(cond: Condition): Double = {
        val ccAdjust = cond.cc match {
          case CloudCover.BEST                  =>  0.0
          case CloudCover.CC70                  =>  1.5
          case CloudCover.CC80 | CloudCover.ANY => 10.0
        }
        val iqAdjust = cond.iq match {
          case ImageQuality.BEST | ImageQuality.IQ70 =>  0.0
          case ImageQuality.IQ85                     =>  1.5
          case ImageQuality.IQANY                    => 10.0
        }
        ccAdjust + iqAdjust
      }

      def gpiIChecks(target: SiderealTarget, cond: Condition):List[(Severity.Value, String)] = {
        // REL-3413: Apply adjustments to magnitude faintness limits.
        val magAdj = magAdjust(cond)
        for {
          m <- target.magnitudes
          if m.band == MagnitudeBand.I
          iMag = m.value
          iMagCondFaint = iMag + magAdj
          if iMag < 3.0 || iMagCondFaint > 9.0
          severity = if (iMag <= 1.0 || iMagCondFaint > 10.0) Severity.Error else Severity.Warning

          // We give priority to:
          // 1. Problems due to conditions affecting faintness levels.
          // 2. Problems due to brightness.
          // 3. Problems due to faintness.
          message = if (iMagCondFaint > 9.0) {
            s"""GPI Target ${target.name}" is too faint due to non-optimal conditions for proper AO (OIWFS) operation: the AO performance will be poor."""
          } else if (iMag > 1.0 && iMag < 3.0) {
            s"""GPI Target "${target.name}" may be too bright for the OIWFS."""
          } else if (iMag <= 1.0) {
            s"""GPI Target "${target.name}" too bright to work with the OIWFS."""
          } else {
            s"""GPI Target "${target.name}" is too faint for proper AO (OIWFS) operation: the AO performance will be poor."""
          }
        } yield (severity, message)
      }

      def gpiIfsChecks(obsMode: GpiObservingMode, disperser: GpiDisperser, target: SiderealTarget):List[(Severity.Value, String)] = for {
          m <- target.magnitudes
          scienceBand <- GpiObservingMode.scienceBand(obsMode)
          if scienceBand.startsWith(m.band.name)
          scienceMag = m.value
          disperserLimit = if (disperser.value.equalsIgnoreCase("Prism")) 0.0 else 2.0
          coronographLimit = 0.0 + disperserLimit
          directLimit = 8.5 + disperserLimit
          if (scienceMag < coronographLimit && GpiObservingMode.isCoronographMode(obsMode)) || (scienceMag < directLimit && GpiObservingMode.isDirectMode(obsMode))
        } yield (Severity.Warning, s"""GPI Target "${target.name}" risks saturating the science detector even for short exposure times.""")

      def gpiLowfsChecks(obsMode: GpiObservingMode, target: SiderealTarget, cond: Condition): List[(Severity.Value, String)] = {
        // REL-3413: Apply adjustments to magnitude faintness limits.
        val magAdj = magAdjust(cond)
        for {
          m <- target.magnitudes
          if m.band == MagnitudeBand.H && GpiObservingMode.isCoronographMode(obsMode)
          hMag = m.value
          hMagCondFaint = hMag + magAdj

          // We give priority to:
          // 1. Problems due to conditions affecting faintness levels.
          // 2. Problems due to brightness.
          // 3. Problems due to faintness.
          if hMag < 2.0 || hMagCondFaint > 9.0
          message = if (hMagCondFaint > 9.0) {
            s"""GPI Target "${target.name}" is too faint due to non-optimal conditions for proper CAL (LOWFS) operation, and thus mask centering on the coronogrph will be severely affected."""
          } else if (hMag < 2.0) {
            s"""GPI Target "${target.name}" is too bright, it will saturate the LOWFS."""
          } else  {
            s"""GPI Target "${target.name}" is too faint for proper CAL (LOWFS) operation and thus mask centering on the coronograph will be severely affected."""
          }
        } yield (Severity.Warning, message)
      }

      val gpiTargetsWithProblems: List[(SiderealTarget, List[(Severity, String)])] = for {
          o <- p.nonEmptyObservations
          b <- o.blueprint
          if b.isInstanceOf[GpiBlueprint]
          obsMode = b.asInstanceOf[GpiBlueprint].observingMode
          disperser = b.asInstanceOf[GpiBlueprint].disperser
          t @ SiderealTarget(_, _, _, _, _, mag) <- o.target
          c <- o.condition
        } yield (t, (gpiMagnitudesPresent(t) :: gpiIChecks(t, c) :: gpiLowfsChecks(obsMode, t, c) :: gpiIfsChecks(obsMode, disperser, t) :: Nil).flatten)

      for {
        gpiProblems <- gpiTargetsWithProblems
        target      =  gpiProblems._1
        problem     <- gpiProblems._2
        severity    =  problem._1
        message     =  problem._2
      } yield new Problem(severity, message, "Targets", s.inTargetsView(_.edit(target)))
    }

    private lazy val missingObsElementCheck = {
      def check[A](msg: String, g: ObsListGrouping[A], adder: ObsListView => Unit) =
        p.nonEmptyObservations.filter(o => g.lens.get(o).isEmpty) match {
          case Nil => None
          case o :: Nil => Some(new Problem(Severity.Error, s"One observation has no $msg.", "Observations", s.inObsListView(o.band, adder)))
          case o :: tail => Some(new Problem(Severity.Error, s"${1 + tail.length} observations have no $msg.", "Observations", s.inObsListView(o.band, adder)))
        }

      List(
        check("instrument configuration", ObsListGrouping.Blueprint, _.Fixes.addBlueprint()),
        check("target", ObsListGrouping.Target, _.Fixes.addTarget()),
        check("observing conditions", ObsListGrouping.Condition, _.Fixes.addConditions())).flatten
    }

    private def indicateObservation(o: Observation) {
      s.inObsListView(o.band, _.Fixes.indicateObservation(o))
    }

    private lazy val missingObsDetailsCheck =
      p.nonEmptyObservations.filter(o => o.nonEmpty && o.calculatedTimes.isEmpty) match {
        case Nil => None
        case h :: Nil => Some(new Problem(Severity.Error, "One observation has no observation time.", "Observations", indicateObservation(h)))
        case h :: tail => Some(new Problem(Severity.Error, s"${1 + tail.length} observations have no observation times.", "Observations", indicateObservation(h)))
      }

    private def hasBestGuidingConditions(o: Observation): Boolean =
      o.condition.exists {
        c =>
          c.cc == CloudCover.BEST && c.iq == ImageQuality.BEST && c.sb == SkyBackground.BEST
      }

    private def guidingMessage(o: Observation): String = {
      val base = "Observation unlikely to have usable guide stars."
      if (hasBestGuidingConditions(o)) base else s"$base Try better conditions?"
    }

    private lazy val badGuiding = for {
      o <- p.nonEmptyObservations
      m <- o.meta
      g <- m.guiding if g.evaluation == GuidingEvaluation.FAILURE
    } yield new Problem(Severity.Warning, guidingMessage(o), "Observations", indicateObservation(o))

    private lazy val cwfsCorrectionsIssue = for {
      o <- p.nonEmptyObservations
      b <- o.blueprint if b.isInstanceOf[GsaoiBlueprint]
      m <- o.meta
      g <- m.guiding if g.evaluation != GuidingEvaluation.SUCCESS
    } yield new Problem(Severity.Warning,
      "Less than three CWFS stars. Corrections will not be optimal.",
      "Observations",
      indicateObservation(o)
    )

    private def visibilityMessage(tmpl: String, sem: Semester, o: Observation): String =
      tmpl.format(
        o.blueprint.map(_.site.name).getOrElse("this telescope"),
        sem.display
      )

    private lazy val badVisibility = for {
      o @ Observation(Some(_), Some(_), Some(t), _, _) <- p.nonEmptyObservations
      v                                                <- if (p.proposalClass.isSpecial) TargetVisibilityCalc.getOnDec(p.semester, o) else TargetVisibilityCalc.get(p.semester, o)
      if v == TargetVisibility.Bad
    } yield new Problem(Severity.Error,
        visibilityMessage("Target is inaccessible at %s during %s. Consider an alternative.", p.semester, o),
        "Observations",
        indicateObservation(o))

    private lazy val iffyVisibility = for {
      o @ Observation(Some(_), Some(_), Some(_), _, _) <- p.nonEmptyObservations
      v                                                <- if (p.proposalClass.isSpecial) TargetVisibilityCalc.getOnDec(p.semester, o) else TargetVisibilityCalc.get(p.semester, o)
      if v == TargetVisibility.Limited
    } yield new Problem(Severity.Warning,
        visibilityMessage("Target has limited visibility at %s during %s.", p.semester, o),
        "Observations",
        indicateObservation(o))

    private lazy val minTimeCheck = {

      val subs:List[Submission] = p.proposalClass match {
        case n: GeminiNormalProposalClass   => n.subs match {
          case Left(ss)  => ss
          case Right(ss) => List(ss)
        }
        case e: ExchangeProposalClass       => e.subs
        case s: SpecialProposalClass        => List(s.sub)
        case l: LargeProgramClass           => List(l.sub)
        case i: SubaruIntensiveProgramClass => List(i.sub)
        case f: FastTurnaroundProgramClass  => List(f.sub)
      }

      subs.filter(sub => sub.request.time.hours < sub.request.minTime.hours).map {
        sub =>
          val kind = sub match {
            case n: NgoSubmission                    => Partners.name(n.partner)
            case e: ExchangeSubmission               => Partners.name(e.partner)
            case s: SpecialSubmission                => s.specialType.value
            case l: LargeProgramSubmission           => "large program"
            case i: SubaruIntensiveProgramSubmission => "intensive program observing at Subaru"
            case f: FastTurnaroundSubmission         => "fast-turnaround"
          }
          new Problem(Severity.Error, s"Requested time for $kind is less than minimum requested time.", TimeProblems.SCHEDULING_SECTION,
            s.inPartnersView(_.editSubmissionTime(sub)))
      }

    }

    private lazy val band3option = (p.meta.band3OptionChosen, p.proposalClass) match {
      case (false, q: QueueProposalClass) => Some(new Problem(Severity.Todo, "Please select a Band 3 option.", TimeProblems.SCHEDULING_SECTION, s.showPartnersView()))
      case _                              => None
    }

    private lazy val band3Orphan2 = for {
      o <- p.nonEmptyObservations
      if o.band == Band.BAND_3 && (p.proposalClass match {
        case q: QueueProposalClass if q.band3request.isDefined => false
        case _                                                 => true
      })
    } yield {
      new Problem(Severity.Error,
        "Allow consideration for Band 3 or delete the Band 3 observation.", "Observations", {
        s.showPartnersView()
        s.inObsListView(o.band, v => v.Fixes.indicateObservation(o))
      })
    }

    private lazy val ftReviewerOrMentor = for {
        f @ FastTurnaroundProgramClass(_, _, _, _, _, _, r, m, _) <- Some(p.proposalClass)
        if r.isEmpty || (~r.map(_.status != InvestigatorStatus.PH_D) && m.isEmpty)
      } yield new Problem(Severity.Error,
            "A Fast Turnaround program must select a reviewer or a mentor with PhD degree.", TimeProblems.SCHEDULING_SECTION, {
              s.showPartnersView()
            })

    private lazy val ftAffiliationMismatch = for {
        pi                                                                   <- Option(p.investigators.pi)
        piNgo                                                                <- Option(Institutions.institution2Ngo(pi.address))
        f @ FastTurnaroundProgramClass(_, _, _, _, _, _, _, _, affiliateNgo) <- Option(p.proposalClass)
        samePartner                                                          <- (affiliateNgo |@| piNgo){_ === _}
        if !samePartner // Show a warning if the PI's institution partner isn't the same as the partner affiliation
      } yield new Problem(Severity.Info,
            s"The Fast Turnaround affiliation, '${~Partners.nameOfFTPartner(affiliateNgo)}', is different from the PI's default affiliation, '${~Partners.nameOfFTPartner(piNgo)}'.", TimeProblems.SCHEDULING_SECTION, {
              s.showPartnersView()
            })

    private lazy val wrongSite = for {
      o <- p.nonEmptyObservations
      b <- o.blueprint if (p.proposalClass match {
      case e: ExchangeProposalClass if e.partner == ExchangePartner.KECK   => b.site != Site.Keck
      case e: ExchangeProposalClass if e.partner == ExchangePartner.SUBARU => b.site != Site.Subaru
      case s: SubaruIntensiveProgramClass                                  => b.site != Site.Subaru
      case _                                                               => b.site != Site.GN && b.site != Site.GS
    })
    } yield {
      val host = p.proposalClass match {
        case e: ExchangeProposalClass if e.partner == ExchangePartner.KECK    => Site.Keck.name
        case e: ExchangeProposalClass if e.partner == ExchangePartner.SUBARU  => Site.Subaru.name
        case e: ExchangeProposalClass if e.partner == ExchangePartner.SUBARU  => Site.Subaru.name
        case e: SubaruIntensiveProgramClass                                   => Site.Subaru.name
        case _                                                                => "Gemini"
      }
      new Problem(Severity.Error, s"Scheduling request is for $host but resource resides at ${b.site.name}.", "Observations", {
        s.showPartnersView()
        s.inObsListView(o.band, v => v.Fixes.indicateObservation(o))
      })
    }

    private lazy val noObs = when (!p.hasNonEmptyObservations) {
      new Problem(Severity.Todo, "Please create observations with conditions, targets, and resources.", "Observations", ())
    }

    private def investigatorFullName(i: Investigator, default: String = "Investigator"): String = {
      val PiEmptyName = PrincipalInvestigator.empty.fullName
      i.fullName.trim match {
        case ""          => default
        case PiEmptyName => "PI"
        case n           => n
      }
    }

    private lazy val incompleteInvestigator = for {
      i <- p.investigators.all if !i.isComplete
    } yield new Problem(Severity.Todo, s"Please provide full contact information for ${investigatorFullName(i)}.", "Overview", s.inOverview{_.edit(i)})

    private val nonUpdatedInvestigatorName =
      when(p.investigators.pi.fullName.trim === PrincipalInvestigator.empty.fullName || p.investigators.pi.fullName.trim.isEmpty) {
        new Problem(Severity.Todo, s"Please provide PI's full name.", "Overview", s.inOverview{_.edit(p.investigators.pi)})
      }

    private val noPIPhoneNumber = when (p.investigators.pi.phone.isEmpty) {
      new Problem(Severity.Warning,
        s"No phone number given for ${investigatorFullName(p.investigators.pi, "PI")}. This is for improved user support.",
        "Overview", s.inOverview(_.editPi(_.Phone.requestFocus)))
    }

    private val invalidPIPhoneNumber = {
      val MinDigits = 8
      p.investigators.pi.phone.find(_.count(_.isDigit) < MinDigits).map { _ =>
        new Problem(Severity.Warning,
          s"The phone number given for ${investigatorFullName(p.investigators.pi, "PI")} contains less than $MinDigits digits and thus is not valid.",
          "Overview", s.inOverview(_.editPi(_.Phone.requestFocus())))
      }
    }
  }
}

import ProblemRobot._
import TimeProblems._

object TimeProblems {
  // Are two time amounts close enough to be considered the same?
  // Times are all rounded to two decimal places.
  def sameTime(t1: TimeAmount, t2: TimeAmount): Boolean =
    (t1.hours - t2.hours).abs < 0.01

  // The goal here is to not show too much precision and yet not say two
  // times are different and print out two amounts that look the same.
  def formatDifferingTimes(t1: TimeAmount, t2: TimeAmount, prec: Int): Option[(String, String)] =
    if (t1.units != t2.units)
      formatDifferingTimes(t1.toHours, t2.toHours, prec)
    else
      prec match {
        case n if n < 0 => formatDifferingTimes(t1, t2, 0)
        case n if n < 4 =>
          val s1 = t1.format(n)
          val s2 = t2.format(n)
          if (s1.equals(s2)) formatDifferingTimes(t1, t2, n + 1) else Some((s1, s2))
        case _ => None
      }

  val SCHEDULING_SECTION = "Time Requests"

  // REL-3493: All CFH exchange time will now be done in queue.
  def noCFHClassical(p: Proposal, s: ShellAdvisor): Option[ProblemRobot.Problem] = p.proposalClass match {
    case ClassicalProposalClass(_, _, _, Right(ExchangeSubmission(_, _, ExchangePartner.CFH, _)), _) =>
      Some(new Problem(Severity.Error, "All CFH exchange time will now be done in queue.", SCHEDULING_SECTION, s.inPartnersView(_.editProposalClass())))
    case _ => None
  }

  // REL-2032 Check that none of the requested times per partner are zero
  def partnerZeroTimeRequest(p: Proposal, s:ShellAdvisor): List[ProblemRobot.Problem] = p.proposalClass match {
    case g:GeminiNormalProposalClass =>
      val probs = for {
          sub <- g.subs.swap.right
        } yield for {
            ps <- sub
            if ps.request.time.value <= 0.0
          } yield new Problem(Severity.Error, s"Please specify a time request for ${Partners.name.getOrElse(ps.partner, "")} or remove partner.", SCHEDULING_SECTION, s.inPartnersView(_.editSubmissionTime(ps)))
      probs.right.getOrElse(Nil)
    case _                            => Nil
  }
}

object ResourceIssues {
  def resourceNotOfferedCheck[B <: BlueprintBase](p: Proposal, s: ShellAdvisor, sem: Semester, name: Option[String] = None): List[Problem] = for {
    o <- p.observations
    b <- o.blueprint
    if b.isInstanceOf[B]
    if p.semester == sem
  } yield new Problem(Severity.Error, s"${name.getOrElse(b.name)} not offered for ${sem.display}.",
                      "Observations", s.inObsListView(o.band, _.Fixes.fixBlueprint(b)))
}

case class Semester2020BProblems(p: Proposal, s: ShellAdvisor) {
  private val sem = Semester(2020, SemesterOption.B)

  private val subaruResourcesNotOfferedCheck = for {
    o <- p.observations
    b <- o.blueprint
    if b.isInstanceOf[SubaruBlueprint]
    if p.semester == sem
    i = b.asInstanceOf[SubaruBlueprint].instrument
    if i == SubaruInstrument.FMOS && i != SubaruInstrument.SUPRIME_CAM && i != SubaruInstrument.COMICS
  } yield new Problem(Severity.Error, s"Subaru ${i.value} not offered for 2020B.", "Observations", s.inObsListView(o.band, _.Fixes.fixBlueprint(b)))

  def all: List[Problem] = List(
    subaruResourcesNotOfferedCheck,
    ResourceIssues.resourceNotOfferedCheck[GpiBlueprint](p, s, sem, "GPI".some)).flatten
}

case class Semester2020AProblems(p: Proposal, s: ShellAdvisor) {
  private val sem = Semester(2020, SemesterOption.A)
  def all: List[Problem] = List(
    ResourceIssues.resourceNotOfferedCheck[TexesBlueprint](p, s, sem),
    ResourceIssues.resourceNotOfferedCheck[PhoenixBlueprint](p, s, sem)).flatten
}

case class TimeProblems(p: Proposal, s: ShellAdvisor) {
  lazy val requested = p.proposalClass.requestedTime
  def obsTimeSum(b: Band) = TimeAmount.sum(for {
    o <- p.nonEmptyObservations if o.band == b
    t <- o.totalTime
  } yield t)
  lazy val obs = obsTimeSum(Band.BAND_1_2)
  lazy val obsB3 = obsTimeSum(Band.BAND_3)

  lazy val b3Req = p.proposalClass match {
    case q: QueueProposalClass => q.band3request
    case _ => None
  }
  lazy val b3ReqOrZero = b3Req.map(_.time).getOrElse(TimeAmount.empty)
  lazy val jointNotAllowed = {
    def checkForNotAllowedJointProposals(subs: Option[List[NgoSubmission]]):List[Problem] = {
      val r = for {
          subs <- subs
          if subs.size > 1
        } yield for {
            p <- subs.filter(s => Partners.jointProposalNotAllowed.contains(s.partner))
          } yield new Problem(Severity.Error, s"${~Partners.name.get(p.partner)} cannot be part of a joint proposal, please update the time request.", SCHEDULING_SECTION,
              s.showPartnersView())
      r.sequence.flatten
    }

    p.proposalClass match {
      case p: GeminiNormalProposalClass => checkForNotAllowedJointProposals(p.subs.left.toOption)
      case e: ExchangeProposalClass     => checkForNotAllowedJointProposals(e.subs.some)
      case x                            => Nil
    }
  }

  private def when[A](b: Boolean)(a: => A) = b option a

  private def requestedTimeCheck(r: TimeAmount, o: TimeAmount, b: Band) =
    when(r.hours > 0 && !sameTime(r, o)) {
      val b3 = if (b == Band.BAND_3) "Band 3 " else ""
      val msg = (formatDifferingTimes(r, o, 2) map {
        case (s1, s2) => s"Requested ${b3}time, $s1, differs from the sum of times for all ${b3}observations, $s2."
      }).getOrElse(s"Requested ${b3}time differs from the sum of times for all ${b3}observations.")
      new Problem(Severity.Warning, msg, SCHEDULING_SECTION, {
        s.showPartnersView()
        s.showObsListView(b)
      })
    }

  def requestedTimeDiffers = requestedTimeCheck(requested, obs, Band.BAND_1_2)
  def requestedB3TimeDiffers = requestedTimeCheck(b3ReqOrZero, obsB3, Band.BAND_3)

  def noTimeRequest = when(requested.hours <= 0.0) {
    new Problem(Severity.Todo, "Please specify a time request.", SCHEDULING_SECTION, s.showPartnersView())
  }

  private def b3Problem(f: SubmissionRequest => Boolean, sev: Severity, msg: String) = when(b3Req.exists(r => f(r))) {
    new Problem(sev, msg, SCHEDULING_SECTION, s.inPartnersView(_.editBand3Time()))
  }

  def noBand3Time = b3Problem(_.time.hours <= 0.0, Severity.Todo, "Please enter the total requested time for a Band 3 allocation.")
  def noMinBand3Time = b3Problem(r => r.time.hours > 0.0 && r.minTime.hours <= 0.0, Severity.Todo, "Please enter the minimum required time for a usable Band 3 allocation.")
  def band3MinTime = b3Problem(r => r.time.hours < r.minTime.hours, Severity.Error, "The minimum Band 3 required time must not be longer than the total Band 3 requested time.")

  def all = List(requestedTimeDiffers, requestedB3TimeDiffers, noTimeRequest, noBand3Time, noMinBand3Time, band3MinTime).flatten ++ jointNotAllowed
}


object TacProblems {

  val TAC_SECTION = "TAC"
  type Partner = Any
  // ugh
  def tac = AppPreferences.current.mode == AppPreferences.PITMode.TAC
  def name(p: Partner) = Partners.name.getOrElse(p, "<unknown>")

}

case class TacProblems(proposal: Proposal, s: ShellAdvisor) {

  import TacProblems._

  lazy val responses = TacView.responses(proposal.proposalClass)
  lazy val accepts = responses.flatMap {
    case (p, r) =>
        r.decision.flatMap {
          case SubmissionDecision(Right(a)) => Some((p, a))
          case _ => None
        }
    }

  def tacProblem(f: => Boolean, sev: Severity, msg: String, partner: Partner): Option[Problem] =
    Option(f).filter(_ == true).map(_ => new Problem(sev, msg, TAC_SECTION, s.showTacView(partner)))

  def noResponses = Option(responses).filter(_.isEmpty).map(_ => new Problem(
    Severity.Error,
    "This proposal has no responses; TAC mode is not applicable.",
    TAC_SECTION,
    new AppPreferencesAction(s.shell)()))

  def noDecision = responses.map {
    case (p, r) =>
      tacProblem(
        r.decision.isEmpty,
        Severity.Todo,
        s"Please provide a TAC decision for ${name(p)}.",
        p)
  }

  def noEmail = accepts.map {
    case (p, a) =>
      tacProblem(a.email.trim.isEmpty, Severity.Todo, s"Please provide a contact email address for ${name(p)}.", p)
  }

  def noRanking = accepts.map {
    case (p, a) =>
      tacProblem(a.ranking == 0, Severity.Todo, s"Please provide a non-zero ranking for ${name(p)}.", p)
  }

  def noTimes = accepts.map {
    case (p, a) =>
      tacProblem(a.recommended.isEmpty, Severity.Todo, s"Please provide a recommended time for ${name(p)}.", p)
  }

  def badTimes = accepts.map {
    case (p, a) =>
      tacProblem(a.recommended.hours < a.minRecommended.hours, Severity.Error, s"Minimum time is greater than recommended time for ${name(p)}.", p)
  }

  def all: List[Problem] = if (tac) (
      noDecision ++
      noEmail ++
      noRanking ++
      noTimes ++
      badTimes ++
      List(noResponses)
    ).flatten.toList
  else Nil

}
