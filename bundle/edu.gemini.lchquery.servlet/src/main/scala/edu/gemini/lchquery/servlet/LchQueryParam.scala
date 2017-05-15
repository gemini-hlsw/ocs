package edu.gemini.lchquery.servlet

import edu.gemini.odb.browser.{Investigator, Partner}
import edu.gemini.pot.sp.{ISPObservation, ISPProgram}
import edu.gemini.spModel.`type`.DisplayableSpType
import edu.gemini.spModel.ao.{AOConstants, AOTreeUtil}
import edu.gemini.spModel.core.ProgramId
import edu.gemini.spModel.core.ProgramId.Science
import edu.gemini.spModel.data.YesNoType
import edu.gemini.spModel.dataset.{DatasetLabel, DatasetQaRecord}
import edu.gemini.spModel.gemini.altair.{AltairParams, InstAltair}
import edu.gemini.spModel.gemini.obscomp.{SPProgram, SPSiteQuality}
import edu.gemini.spModel.gemini.obscomp.SPProgram.Active
import edu.gemini.spModel.gemini.phase1.GsaPhase1Data
import edu.gemini.spModel.obs.{ObsClassService, ObsTimesService, ObservationStatus, SPObservation}
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.obscomp.SPInstObsComp
import edu.gemini.spModel.obslog.ObsLog
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.too.{Too, TooType}
import edu.gemini.spModel.util.SPTreeUtil

import scala.collection.JavaConverters._
import scala.util.matching.Regex
import scalaz._
import Scalaz._


sealed trait ValueMatcher[A] {
  def matches(expression: String, x: A): Boolean
}

object ValueMatcher {
  private[servlet] implicit class ToRegex(val expression: String) extends AnyVal {
    def toRegex: Regex = ("^(?i)" +
        (expression.contains("|") ? s"($expression)" | expression).
          replaceAllLiterally("-", "\\-").
          replaceAllLiterally("+", "\\+").
          replaceAllLiterally("*", ".*").
          replaceAllLiterally("%", ".*").
          replaceAllLiterally("?", ".")
        + "$").r
  }

  abstract class StringValueMatcher[A] extends ValueMatcher[A] {
    protected def extractor(a: A): Option[String]

    override def matches(expression: String, x: A): Boolean = (for {
      r <- Option(expression).map(_.toRegex)
      m <- Option(x).flatMap(extractor)
    } yield r.findFirstMatchIn(m).isDefined).getOrElse(false)
  }


  abstract class BooleanValueMatcher[A, B <: DisplayableSpType] extends ValueMatcher[A] {
    protected def extractor(a: A): Option[B]

    private def transform(expression: String): String = expression.toLowerCase match {
      case "true" => YesNoType.YES.displayValue()
      case "false" => YesNoType.NO.displayValue()
      case _ => expression
    }

    override def matches(expression: String, x: A): Boolean = (for {
      r <- Option(expression).map(transform).map(_.toRegex)
      m <- Option(x).flatMap(extractor).map(_.displayValue)
    } yield r.findFirstMatchIn(m).isDefined).getOrElse(false)
  }
}

case class InvestigatorInfo(name: String, email: Option[String], isPrincipal: Boolean)
object InvestigatorInfo {
  private[servlet] implicit class ToInvestigator(val i: InvestigatorInfo) extends AnyVal {
    def toInvestigator: Investigator = new Investigator() {
      setName(i.name)
      i.email.foreach(setEmail)
      setPi(i.isPrincipal)
    }
  }
}

case class PartnerInfo(name: String, hoursAllocated: Double)
object PartnerInfo {
  private[servlet] implicit class ToPartner(val p: PartnerInfo) extends AnyVal {
    def toPartner: Partner = new Partner() {
      setName(p.name)
      setHoursAllocated(p.hoursAllocated)
    }
  }
}

case class LchQueryParam[A](name: String, v: ValueMatcher[A])

object LchQueryParam {
  import ValueMatcher._

  def stringToOpt(s: String): Option[String] =
    Option(s.trim).filterNot(_.isEmpty)

  private[servlet] implicit class ToYesNo(val b: Boolean) extends AnyVal {
    def toYesNo: YesNoType =
      b ? YesNoType.YES | YesNoType.NO
  }


  private[servlet] implicit class ISPProgramExtractors(val prog: ISPProgram) extends AnyVal {
    def toSPProg: SPProgram =
      prog.getDataObject.asInstanceOf[SPProgram]

    def scienceSemester: Option[String] =
      ProgramId.parse(prog.getProgramID.toString) match {
        case Science(_, sem, _, _) => Some(sem.toString)
        case _                     => None
      }

    // Returns a tuple of investigator info in the form name /

    def investigatorInfo: List[InvestigatorInfo] = {
      val spProg = toSPProg

      def extractPi: Option[InvestigatorInfo] = {
        val piInfo       = spProg.getPIInfo
        val piNameStrOpt = stringToOpt(s"${piInfo.getFirstName} ${piInfo.getLastName}")
        piNameStrOpt.map(InvestigatorInfo(_, stringToOpt(piInfo.getEmail), isPrincipal = true))
      }

      def extractCoi(coi: GsaPhase1Data.Investigator): Option[InvestigatorInfo] = for {
        c <- Option(coi)
        n <- stringToOpt(s"${c.getFirst} ${c.getLast}")
      } yield InvestigatorInfo(n, stringToOpt(c.getEmail), isPrincipal = false)

      (extractPi :: spProg.getGsaPhase1Data.getCois.asScala.map(extractCoi).toList).flatten
    }

    def abstrakt: String =
      toSPProg.getGsaPhase1Data.getAbstract.toString

    def scienceBand: String =
      toSPProg.getQueueBand

    def supportPartner: Option[String] =
      Option(toSPProg.getPIAffiliate).map(_.displayValue)

    def partnerList: List[PartnerInfo] = {
      val taa = toSPProg.getTimeAcctAllocation
      taa.getCategories.asScala.toList.map { c =>
        PartnerInfo(c.getDisplayName, taa.getAward(c).getProgramHours)
      }
    }

    def tooStatus: TooType =
      toSPProg.getTooType

    def rolloverStatus: YesNoType =
      toSPProg.getRolloverStatus.toYesNo

    def completed: YesNoType =
      toSPProg.isCompleted.toYesNo

    def thesis: YesNoType =
      toSPProg.isThesis.toYesNo

    def proprietaryMonths: Option[Int] =
      Option(toSPProg.getGsaAspect).map(_.getProprietaryMonths)

    // Allocated time in ms.
    def allocatedTime: Long =
      toSPProg.getTimeAcctAllocation.getSum().getProgramAward().toMillis();

    // Remaining time in ms.
    def remainingTime: Long =
      ObsTimesService.getRemainingProgramTime(prog)
  }


  private[servlet] implicit class ISPObservationExtractors(val obs: ISPObservation) extends AnyVal {
    def toSPObs: SPObservation =
      obs.getDataObject.asInstanceOf[SPObservation]

    def instrument: Option[SPInstObsComp] =
      SPTreeUtil.findInstruments(obs).asScala.map(_.getDataObject).collect {
        case inst: SPInstObsComp => inst
      }.headOption

    def instrumentType: Option[String] =
      instrument.map(_.getType.readableStr)

    def ao: AOConstants.AO = {
      Option(AOTreeUtil.findAOSystem(obs)).map(_.getDataObject).
        collect {
          case inst: InstAltair if inst.getGuideStarType == AltairParams.GuideStarType.LGS => AOConstants.AO.Altair_LGS
          case inst: InstAltair if inst.getGuideStarType == AltairParams.GuideStarType.NGS => AOConstants.AO.Altair_NGS
        }.getOrElse(AOConstants.AO.NONE)
    }

    def tooStatus: TooType =
      Too.get(obs)

    def status: ObservationStatus =
      ObservationStatus.computeFor(obs)

    def obsClass: ObsClass =
      ObsClassService.lookupObsClass(obs)

    def targetEnvironment: Option[TargetEnvironment] = for {
      n <- Option(SPTreeUtil.findTargetEnvNode(obs))
      c <- Option(n.getDataObject).map(_.asInstanceOf[TargetObsComp])
      t <- Option(c.getTargetEnvironment)
    } yield t

    def timingWindows: Option[List[SPSiteQuality.TimingWindow]] = for {
      n <- Option(SPTreeUtil.findObsCondNode(obs))
      c <- Option(n.getDataObject).map(_.asInstanceOf[SPSiteQuality])
    } yield c.getTimingWindows.asScala.toList

    def obsLogComments: Option[Map[DatasetLabel,DatasetQaRecord]] =
      Option(ObsLog.getIfExists(obs)).map(_.getQaRecord.qaMap.filterNot {
        case (_,rec) => rec.comment.isEmpty
      })
  }



  private[servlet] val ProgramSemesterParam = LchQueryParam("programSemester",
    new StringValueMatcher[ISPProgram] {
      override protected def extractor(prog: ISPProgram): Option[String] =
        prog.scienceSemester
    }
  )

  private[servlet] val ProgramTitleParam = LchQueryParam("programTitle",
    new StringValueMatcher[ISPProgram] {
      override protected def extractor(prog: ISPProgram): Option[String] =
        prog.toSPProg.getTitle.some
    }
  )

  private[servlet] val ProgramReferenceParam = LchQueryParam("programReference",
    new StringValueMatcher[ISPProgram] {
      override protected def extractor(prog: ISPProgram): Option[String] =
        prog.getProgramID.stringValue.some
    }
  )

  private[servlet] val ProgramActiveParam = LchQueryParam("programActive",
    new BooleanValueMatcher[ISPProgram,SPProgram.Active] {
      override protected def extractor(prog: ISPProgram): Option[Active] =
        prog.toSPProg.getActive.some
    }
  )

  private[servlet] val ProgramCompletedParam = LchQueryParam("programCompleted",
    new BooleanValueMatcher[ISPProgram,YesNoType] {
      override protected def extractor(prog: ISPProgram): Option[YesNoType] =
        prog.completed.some
    }
  )

  private[servlet] val ProgramNotifyPIParam = LchQueryParam("programNotifyPi",
    new BooleanValueMatcher[ISPProgram,YesNoType] {
      override protected def extractor(prog: ISPProgram): Option[YesNoType] =
        prog.toSPProg.getNotifyPi.some
    }
  )

  private[servlet] val ProgramRolloverParam = LchQueryParam("programRollover",
    new BooleanValueMatcher[ISPProgram,YesNoType] {
      override protected def extractor(prog: ISPProgram): Option[YesNoType] =
        prog.rolloverStatus.some
    }
  )

  private[servlet] val ObservationTOOStatusParam = LchQueryParam("observationTooStatus",
    new StringValueMatcher[ISPObservation] {
      override protected def extractor(obs: ISPObservation): Option[String] =
        obs.tooStatus.getDisplayValue.some
    }
  )

  private[servlet] val ObservationNameParam = LchQueryParam("observationName",
    new StringValueMatcher[ISPObservation] {
      override protected def extractor(obs: ISPObservation): Option[String] =
        obs.toSPObs.getTitle.some
    }
  )

  private[servlet] val ObservationStatusParam = LchQueryParam("observationStatus",
    new BooleanValueMatcher[ISPObservation,ObservationStatus] {
      override protected def extractor(obs: ISPObservation): Option[ObservationStatus] =
        obs.status.some
    }
  )

  private[servlet] val ObservationInstrumentParam = LchQueryParam("observationInstrument",
    new StringValueMatcher[ISPObservation] {
      override protected def extractor(obs: ISPObservation): Option[String] =
        obs.instrumentType
    }
  )

  private[servlet] val ObservationAOParam = LchQueryParam("observationAo",
    new BooleanValueMatcher[ISPObservation, AOConstants.AO] {
      override protected def extractor(obs: ISPObservation): Option[AOConstants.AO] =
        obs.ao.some
    }
  )

  private[servlet] val ObservationClassParam = LchQueryParam("observationClass",
    new StringValueMatcher[ISPObservation] {
      override protected def extractor(obs: ISPObservation): Option[String] =
        obs.obsClass.displayValue.some
    }
  )

  val ProgramParams = List(
    ProgramSemesterParam,
    ProgramTitleParam,
    ProgramReferenceParam,
    ProgramActiveParam,
    ProgramCompletedParam,
    ProgramNotifyPIParam,
    ProgramRolloverParam
  )

  val ObservationParams = List(
    ObservationTOOStatusParam,
    ObservationNameParam,
    ObservationStatusParam,
    ObservationInstrumentParam,
    ObservationAOParam,
    ObservationClassParam
  )

  private lazy val ParamNames = (ProgramParams.map(_.name) ++ ObservationParams.map(_.name)).toSet
  def validParamName(paramName: String): Boolean = ParamNames.contains(paramName)
}

