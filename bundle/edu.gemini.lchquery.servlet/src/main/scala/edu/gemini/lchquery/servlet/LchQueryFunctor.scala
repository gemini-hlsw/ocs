package edu.gemini.lchquery.servlet

import java.io.Serializable
import java.security.Principal
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util
import java.util.logging.{Level, Logger}

import edu.gemini.odb.browser._
import edu.gemini.pot.sp.{ISPNode, ISPObservation, ISPProgram}
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService}
import edu.gemini.skycalc.{DDMMSS, HHMMSS}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.obs.ObservationStatus
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.too.Too

import scala.collection.JavaConverters._

import scalaz._, Scalaz._

class LchQueryFunctor(queryType: LchQueryFunctor.QueryType,
                      programParams: List[(LchQueryParam[ISPProgram], String)],
                      observationParams: List[(LchQueryParam[ISPObservation], String)]) extends DBAbstractQueryFunctor {

  val queryResult: QueryResult = new QueryResult() {
    setProgramsNode(new ProgramsNode())
  }

  // Called once per program by IDBQueryRunner implementation.
  override def execute(db: IDBDatabaseService, node: ISPNode, principals: util.Set[Principal]): Unit = {
    val prog = node.asInstanceOf[ISPProgram]

    def programMatches: Boolean = programParams.forall {
      case (LchQueryParam(_, valueMatcher), paramValue) => valueMatcher.matches(paramValue, prog)
    }
    def observationMatches(obs: ISPObservation): Boolean = observationParams.forall {
      case (LchQueryParam(_, valueMatcher), paramValue) => valueMatcher.matches(paramValue, obs)
    }

    \/.fromTryCatchNonFatal {
      // See if an ISPProgram matches the query specifications.
      if (programMatches) {
        import LchQueryFunctor.QueryType.ProgramQuery
        addProgram(prog, (queryType == ProgramQuery) ? List.empty[ISPObservation] | prog.getAllObservations.asScala.toList.filter(observationMatches))
      }
    } match {
      case \/-(s) =>
      case -\/(t) =>
        LchQueryFunctor.Log.log(Level.SEVERE, "problem running LchQueryFunctor", t)
        throw new RuntimeException(t)
    }
  }


  private def addProgram(prog: ISPProgram, obsList: List[ISPObservation]): Unit = {
    import LchQueryParam.{ISPProgramExtractors, ISPObservationExtractors}

    def makeTargetNode(target: SPTarget, env: TargetEnvironment): Option[Serializable] = {
      def targetType: Option[String] = {
        if (env.getArbitraryTargetFromAsterism eq target) Some("Base")
        else if (env.isUserPosition(target)) Some("User")
        else if (env.isGuidePosition(target)) {
          for {
            gg  <- env.getGroups.asScala
            gpt <- gg.getAll.asScala
            if gpt.containsTarget(target)
          } yield gpt.getGuider.getKey
        }.headOption
        else None
      }

      if (target.isNonSidereal) {
        Some(new NonSidereal() {
          setName(target.getName)
          setType(targetType.orNull)
          for {
            n <- target.getNonSiderealTarget
            h <- n.horizonsDesignation
          } {
            setHorizonsObjectId(h.queryString)
          }
        })
      } else if (target.isSidereal) {
        target.getSkycalcCoordinates(None.asGeminiOpt).asScalaOpt.map { coords =>
          new Sidereal() {
            setName(target.getName)
            setType(targetType.orNull)
            setHmsDms(new HmsDms() {
              setRa(HHMMSS.valStr(coords.getRa.getMagnitude))
              setDec(DDMMSS.valStr(coords.getDec.getMagnitude))
            })
          }
        }
      } else None
    }


    def makeTimingWindow(spTW: SPSiteQuality.TimingWindow): TimingWindow = {
      def timingString(ms: Long) =
        f"${ms / LchQueryFunctor.MsPerHour}%d:${(ms % LchQueryFunctor.MsPerHour) / LchQueryFunctor.MsPerMinute }%02d"

      def formatDuration: Option[String] =
        Option(spTW.getDuration).
          filterNot(_ == SPSiteQuality.TimingWindow.WINDOW_REMAINS_OPEN_FOREVER).
          map(timingString)

      def formatWindow: String =
        LchQueryFunctor.dateFormat.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(spTW.getStart), ZoneId.of("UTC")))

      def formatPeriod: Option[String] =
        Option(spTW).
          filterNot(_.getDuration == SPSiteQuality.TimingWindow.WINDOW_REMAINS_OPEN_FOREVER).
          filterNot(_.getRepeat == SPSiteQuality.TimingWindow.REPEAT_NEVER).
          map(t => timingString(t.getPeriod))

      def formatTimes: Option[String] =
        Option(spTW).
          filterNot(_.getDuration == SPSiteQuality.TimingWindow.WINDOW_REMAINS_OPEN_FOREVER).
          filterNot(_.getRepeat == SPSiteQuality.TimingWindow.REPEAT_FOREVER).
          filterNot(_.getRepeat == SPSiteQuality.TimingWindow.REPEAT_NEVER).
          map(tw => String.valueOf(tw.getRepeat))

      new TimingWindow() {
        setDuration(formatDuration.orNull)
        setTime(formatWindow)
        if (spTW.getRepeat != SPSiteQuality.TimingWindow.REPEAT_NEVER) {
          setRepeats(new TimingWindowRepeats() {
            setPeriod(formatPeriod.orNull)
            setTimes(formatTimes.orNull)
          })
        }
      }
    }

    queryResult.getProgramsNode.getPrograms.add {
      val spProg = prog.toSPProg
      new Program() {
        setActive(spProg.getActive.displayValue)
        setCompleted(prog.completed.displayValue)
        Option(prog.getProgramID).foreach(id => setReference(id.stringValue()))
        setSemester(prog.scienceSemester.orNull)
        setTitle(spProg.getTitle)
        setContactScientistEmail(spProg.getContactPerson)
        setNgoEmail(spProg.getPrimaryContactEmail)
        setNotifyPi(spProg.getNotifyPi.displayValue)
        setRollover(prog.rolloverStatus.displayValue)

        setInvestigatorsNode(new InvestigatorsNode() {
          prog.investigatorInfo.foreach(i => getInvestigators.add(i.toInvestigator))
        })

        val progAbstrakt = prog.abstrakt
        if (!progAbstrakt.isEmpty) setAbstrakt(progAbstrakt)

        setScienceBand(prog.scienceBand)
        prog.supportPartner.foreach(setSupportPartner)

        val partnerList = prog.partnerList
        if (partnerList.nonEmpty) {
          setPartnersNode(new PartnersNode() {
            partnerList.foreach(p => getPartners.add(p.toPartner))
          })
        }

        setTooStatus(prog.tooStatus.getDisplayValue)
        setThesis(prog.thesis.displayValue)
        prog.proprietaryMonths.map(_.toString).foreach(setProprietaryMonths)
        setAllocatedTime(prog.allocatedTime.toString)
        setRemainingTime(prog.remainingTime.toString)

        if (queryType != LchQueryFunctor.QueryType.ProgramQuery) {
          setObservationsNode(new ObservationsNode() {
            obsList.map { obs =>
              new Observation() {
                val spObs = obs.toSPObs
                setAo(obs.ao.displayValue)
                obs.instrumentType.foreach(setInstrument)
                setName(spObs.getTitle)
                setObsClass(obs.obsClass.displayValue)
                setId(obs.getObservationID.stringValue)
                setStatus(ObservationStatus.computeFor(obs).displayValue)
                setTooPriority(Too.get(obs).getDisplayValue)

                // Create the timing windows.
                obs.timingWindows.filter(_.nonEmpty).foreach { tws =>
                  setConditions(new Conditions() {
                    setTimingWindowsNode(new TimingWindowsNode() {
                      tws.map(makeTimingWindow).foreach(getTimingWindows.add)
                    })
                  })
                }

                // Set the obs log comments.
                // If there are none, no setObsLogNode should be called.
                obs.obsLogComments.filter(_.nonEmpty).foreach { log =>
                  setObsLogNode(new ObsLogNode() {
                    log.toList.foreach { case (lb,rc) =>
                      getObsLog.add(new ObsLogRecord() {
                        setId(lb.toString)
                        setRecord(rc.comment)
                      })
                    }
                  })
                }

                if (queryType == LchQueryFunctor.QueryType.TargetQuery) {
                  obs.targetEnvironment.foreach { env =>
                    setTargetsNode(new TargetsNode() {
                      getTargets.addAll(env.getTargets.asScalaList.flatMap(t => makeTargetNode(t, env)).asJavaCollection)
                    })
                  }
                }
              }
            }.foreach(getObservations.add)
          })
        }
      }
    }
  }

}

object LchQueryFunctor {
  private[LchQueryFunctor] val Log = Logger.getLogger(classOf[LchQueryFunctor].getName)
  private[LchQueryFunctor] val MsPerSecond = 1000
  private[LchQueryFunctor] val MsPerMinute = MsPerSecond * 60
  private[LchQueryFunctor] val MsPerHour   = MsPerMinute * 60

  private[LchQueryFunctor] val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC"))

  sealed abstract class QueryType(val prefix: String)
  object QueryType {
    case object ProgramQuery extends QueryType("/programs")
    case object ObservationQuery extends QueryType("/observations")
    case object TargetQuery extends QueryType("/targets")
  }

  val QueryTypes = List(
    QueryType.ProgramQuery,
    QueryType.ObservationQuery,
    QueryType.TargetQuery
  )

  def queryType(pathInfo: String): Option[QueryType] =
    QueryTypes.find(q => pathInfo.startsWith(q.prefix))
}