package edu.gemini.lchquery.servlet

import java.io.Serializable
import java.security.Principal
import java.text.SimpleDateFormat
import java.util
import java.util.{Date, TimeZone}
import java.util.logging.{Level, Logger}

import edu.gemini.odb.browser._
import edu.gemini.pot.sp.{ISPNode, ISPObservation, ISPProgram}
import edu.gemini.pot.spdb.{DBAbstractQueryFunctor, IDBDatabaseService, IDBFunctor, IDBParallelFunctor}
import edu.gemini.skycalc.{DDMMSS, HHMMSS}
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality
import edu.gemini.spModel.obs.{ObsClassService, ObservationStatus}
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.too.Too

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


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

    Try {
      // See if an ISPProgram matches the query specifications.
      if (programMatches) {
        val matchingObs = prog.getAllObservations.asScala.toList.filter(observationMatches)
        if (matchingObs.nonEmpty) {
          addProgram(prog, if (queryType == LchQueryFunctor.QueryType.ProgramQuery) Nil else matchingObs)
        }
      }
    } match {
      case Success(_) =>
      case Failure(t) =>
        LchQueryFunctor.Log.log(Level.SEVERE, "problem running LchQueryFunctor", t)
        throw new RuntimeException(t)
    }
  }


  private def addProgram(prog: ISPProgram, obsList: List[ISPObservation]): Unit = {
    import LchQueryParam.{ISPProgramExtractors, ISPObservationExtractors, ToYesNo}

    def makeTargetNode(target: SPTarget, env: TargetEnvironment): Option[Serializable] = {
      def targetType: Option[String] = {
        if (env.isBasePosition(target)) Some("Base")
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
        LchQueryFunctor.dateFormat.format(new Date(spTW.getStart))

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
      val spProg = prog.toSPProg.get
      new Program() {
        setActive(spProg.getActive.displayValue)
        setCompleted(spProg.isCompleted.toYesNo.displayValue)
        Option(prog.getProgramID).foreach(id => setReference(id.stringValue()))
        setSemester(prog.semester.orNull)
        setTitle(spProg.getTitle)
        setContactScientistEmail(spProg.getContactPerson)
        setNgoEmail(spProg.getNGOContactEmail)
        setNotifyPi(spProg.getNotifyPi.displayValue)
        setPiEmail(Option(spProg.getPIInfo).map(_.getEmail).orNull)
        setRollover(spProg.getRolloverStatus.toYesNo.displayValue)

        if (queryType != LchQueryFunctor.QueryType.ProgramQuery) {
          setObservationsNode(new ObservationsNode() {
            obsList.map { obs =>
              new Observation() {
                val spObs = obs.toSPObs.get
                setAo(obs.ao.displayValue)
                obs.instrumentType.foreach(setInstrument)
                setName(spObs.getTitle)
                Option(ObsClassService.lookupObsClass(obs)).foreach(c => setObsClass(c.displayValue))
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

                if (queryType == LchQueryFunctor.QueryType.TargetQuery) {
                  obs.targetEnvironment.foreach { env =>
                    Option(env.getTargets) collect {
                      case lst if lst.nonEmpty => lst.asScalaList
                    } foreach { tgts =>
                      setTargetsNode(new TargetsNode() {
                        tgts.map(tgt => makeTargetNode(tgt, env)).foreach(getTargets.add)
                      })
                    }
                  }

                }
              }
            } foreach getObservations.add
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

  private[LchQueryFunctor] val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z") {
    setTimeZone(TimeZone.getTimeZone("UTC"))
  }

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