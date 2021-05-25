package edu.gemini.programexport.servlet

import java.io.BufferedOutputStream
import java.security.Principal
import java.util.logging.{Level, Logger}
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.{ProgramId, SPProgramID}
import scalaz._
import Scalaz._
import edu.gemini.pot.sp.{ISPGroup, ISPNode, ISPObsComponent, ISPObservation, ISPProgram, SPComponentType}
import edu.gemini.shared.util.immutable.ScalaConverters._
import edu.gemini.spModel.data.YesNoType
import edu.gemini.spModel.gemini.obscomp.{SPProgram, SPSiteQuality}
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obscomp.{ProgramNote, SPGroup, SPNote, SchedNote}
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.util.SPTreeUtil

import javax.servlet.ServletException
import scala.collection.JavaConverters._

final case class ProgramExportServlet(odb: IDBDatabaseService, user: Set[Principal]) extends HttpServlet {

  import ProgramExportServlet._

  override def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit =
    doRequest(request, response)

  override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit =
    doRequest(request, response)

  def doRequest(request: HttpServletRequest, response: HttpServletResponse): Unit =
  // Attempt to retrieve the program name: ProgramID instead?
    ParamParser(request).id.fold(t => throw new ServletException(t), build(_, response))

  // Build the JSON in a bottom-up approach starting at ISPProgram.
  // Traverse a node's children and on the way back up, add the information for the node.
  // Right now we just return an empty String: whether to use Strings or Argonaut to build the
  // JSON is a decision I'm leaving to the end.
  def build(id: SPProgramID, response: HttpServletResponse): Unit =
    Option(odb.lookupProgramByID(id)) match {
      case Some(ispProgram) =>
        val out: BufferedOutputStream = new BufferedOutputStream(response.getOutputStream)
        response.setContentType("text/plain")

        // TODO: Remove
        out.write(s"Program name received: $id\n".getBytes)

        val json = process(ispProgram)
        out.write(json.getBytes)
        out.close()
      case None =>
        val error = s"Program $id is not in the database!"
        Log.severe(error)
        throw new ServletException(error)
    }

  // Recursive method to build up the JSON representation of the program.
  def process(node: ISPNode): String = {
    node match {
      case p: ISPProgram => processProgram(p)
      case g: ISPGroup => processGroup(g)

      // Other obs component.
      case n: ISPObsComponent => processObsComponent(n)
      case _ => ""
    }
  }

  // Combine components at the same level into a JSON list.
  def combine(lst: List[String]): String =
    ""

  // Top-level program information.
  def processProgram(ispProgram: ISPProgram): String = {
    val result = combine(ispProgram.getObsComponents.asScala.map(process).toList)

    // NOTE: Some of these can be null. Indicated for each field.
    val spProg = ispProgram.getDataObject.asInstanceOf[SPProgram]
    val gsa = spProg.getGsaAspect

    // Is this all needed? Any other PI info? Not specified in doc.
    //        val piFirstName = spProg.getPIFirstName
    //        val piLastName  = spProg.getPILastName
    //        val piEmail     = spProg.getPIInfo.getEmail // Needed?

    // Investigator data
    val gsaPhase1Data = spProg.getGsaPhase1Data // not null
    gsaPhase1Data.getCois.asScala.foreach { inv =>
      val first = inv.getFirst
      val last = inv.getLast
      val email = inv.getEmail
    }

    val affiliate = spProg.getPIAffiliate.displayValue // could be null
    val queueBand = spProg.getQueueBand // String, empty if classical
    val rolloverFlag = spProg.getRolloverStatus // boolean
    val isThesis = spProg.isThesis // boolean
    val programMode = spProg.getProgramMode // isQueue / isClassical // not null
    val tooType = spProg.getTooType.getDisplayValue // not null
    // TODO: In what format?
    val awardedTime = spProg.getAwardedProgramTime // TimeValue, not null

    // All non-null
    val timeAcctAllocation = spProg.getTimeAcctAllocation
    val timeAcctCategories = timeAcctAllocation.getCategories
    timeAcctCategories.asScala.foreach { category =>
      // Times are Durations. Bryan wants int seconds or nights if nights are specified.
      // Not sure how possible this is?
      val timeAcctAward = timeAcctAllocation.getAward(category)
      val programTime = timeAcctAward.getProgramAward
      val partnerTime = timeAcctAward.getPartnerAward
    }

    ""
  }

  // Handling of obs components that don't have an ISP.
  def processObsComponent(n: ISPObsComponent): String = n.getType match {
      // TODO: Do we need all of these?
      case SPComponentType.INFO_PROGRAMNOTE => //| SPComponentType.INFO_SCHEDNOTE | SPComponentType.INFO_NOTE =>
        processProgramNote(n.getDataObject.asInstanceOf[SPNote])
    }

  // Program-specific notes: must go through the components and check type.
  // The obs components for all of these types are SPNotes or subclasses.
  def processProgramNote(note: SPNote): String = {
    val title = note.getTitle
    val text  = note.getNote
    ""
  }

  def processGroup(ispGroup: ISPGroup): String = {
    val spGroup = ispGroup.getDataObject.asInstanceOf[SPGroup]
    val key = ispGroup.getNodeKey
    val name = spGroup.getTitle

    // Get a list of observation node keys for the scheduling group
    val observationIds = combine(ispGroup.getObservations.asScala.map {
      _.getNodeKey.toString
    }.toList)

    ""
  }

  def processObservation(ispObservation: ISPObservation): String = {
    val spObservation = ispObservation.getDataObject.asInstanceOf[SPObservation]
    val id = ispObservation.getNodeKey
    val title = spObservation.getTitle
    val phase2Status = spObservation.getPhase2Status.displayValue

    // TODO: If this is None, which it could be, do we want to display it?
    val execStatusOverride = spObservation.getExecStatusOverride.asScalaOpt.map(_.displayValue)

    val priority = spObservation.getPriority.displayValue
    val tooOverrideRapid = spObservation.isOverrideRapidToo.toYesNo.displayValue

    // TODO: Don't know if this is correct way to handle this.
    val setupTimeType = spObservation.getSetupTimeType.toString

    // TODO: Also need acq overhead. Where is this? In setup time type?

    // Site quality is an obscomponent but not wrapped in an ISP object.
    // Is this the proper way to get at it? Otherwise we'll have to match on
    // SPComponentType instead of ISPNode, I think.
    val conditions = SPTreeUtil.findObsCondNode(ispObservation).getDataObject.asInstanceOf[SPSiteQuality]
    val cc = conditions.getCloudCover.displayValue
    val sb = conditions.getSkyBackground.displayValue
    val iq = conditions.getImageQuality.displayValue
    val wv = conditions.getWaterVapor.displayValue

    val ecType = conditions.getElevationConstraintType.displayValue
    val ecMin = conditions.getElevationConstraintMin
    val ecMax = conditions.getElevationConstraintMax

    val timingWindows = combine(conditions.getTimingWindows.asScala.map { tw =>
      // TODO: in ms. Is this what we want?
      val start = tw.getStart
      val duration = tw.getDuration
      val period = tw.getPeriod

      val repeat = tw.getRepeat
      ""
    }.toList)

    // TODO: Now I need a TargetEnvironment... Do I go through ObsContext?

    ""
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
}
