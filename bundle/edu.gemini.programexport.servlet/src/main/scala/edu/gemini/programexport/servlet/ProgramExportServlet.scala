package edu.gemini.programexport.servlet

import java.io.BufferedOutputStream
import java.security.Principal
import java.util.logging.{Level, Logger}
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.{ProgramId, SPProgramID}
import scalaz._
import Scalaz._
import edu.gemini.pot.sp.{ISPProgram, SPComponentType}
import edu.gemini.spModel.gemini.obscomp.SPProgram
import edu.gemini.spModel.obscomp.{ProgramNote, SPGroup, SPNote, SchedNote}

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
  def build(id: SPProgramID, response: HttpServletResponse): String = {
    val out: BufferedOutputStream = new BufferedOutputStream(response.getOutputStream)
    response.setContentType("text/plain")
    out.write(s"Program name received: $id\n".getBytes)

    val ispProgram = odb.lookupProgramByID(id)
    Option(odb.lookupProgramByID(id)).flatMap(p => Option(p.getDataObject.asInstanceOf[SPProgram])) match {
      case None =>
        Log.severe(s"Program $id is not in the database!\n")
        throw new ServletException(s"Program $id does not exist in database.")
      case Some(spProg)
    "Hello cruel world."
    }
  }
  def doRequest(id: SPProgramID, response: HttpServletResponse): Unit = {
    val out: BufferedOutputStream = new BufferedOutputStream(response.getOutputStream)
    response.setContentType("text/plain")
    out.write(s"Program name received: $id\n".getBytes)

    val ispProgram = odb.lookupProgramByID(id)
    Option(odb.lookupProgramByID(id)).flatMap(p => Option(p.getDataObject.asInstanceOf[SPProgram])) match {
      case None =>
        Log.severe(s"Program $id is not in the database!\n")
        throw new ServletException(s"Program $id does not exist in database.")
      case Some(spProg) =>
        // NOTE: Some of these can be null. Indicated for each field.
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

        // Program-specific notes: must go through the components and check type.
        // The obs components for all of these types are SPNotes or subclasses.
        val ProgramNotes: Set[SPComponentType] = Set(
          SPComponentType.INFO_NOTE,
          SPComponentType.INFO_PROGRAMNOTE,
          SPComponentType.INFO_SCHEDNOTE
        )
        ispProgram
          .getObsComponents.asScala
          .filter(note => ProgramNotes.contains(note.getType))
          .map(_.getDataObject.asInstanceOf[SPNote])
          .foreach { note =>
            val noteTitle = note.getTitle
            val noteText  = note.getNote
          }

        // Scheduling groups and their observations.
        ispProgram
          .getGroups.asScala
          .map(_.getDataObject.asInstanceOf[SPGroup])
          .foreach { group =>
            val groupKey   = group.
            val groupTitle = group.getTitle
          }


        out.write(s"ToO type: ${spProg.getTooType}".getBytes)
    }

    out.close()
  }

  def processProgram(ispProg: ISPProgram, out: BufferedOutputStream, response: HttpServletResponse): Unit = {

  }

    // Handle an illegal program name.
//  def illegalArgument(ex: IllegalArgumentException): Unit = {
//      response.setStatus(ProgramExportServlet.ResponseCode.InvalidProgram.code)
//      out.write(s"ERROR: ${ex.getMessage}".getBytes)
//    }
//
//    val pathInfo = request.getPathInfo
//    out.write("Hello world".getBytes)
//
//    out.close()
//  }
}

object ProgramExportServlet {
  val Log: Logger = Logger.getLogger(getClass.getName)

  val IdParam: String = "id"
  case class ParamParser(req: HttpServletRequest) {
    def id: Throwable \/ SPProgramID = {
      \/.fromTryCatchNonFatal(SPProgramID.toProgramID(req.getParameter(IdParam))).leftMap { t =>
        Log.log(Level.SEVERE, "Problem running ProgramExportServlet", t)
        t
        //    } match {
        //        case \/-(id) => \/.right(id)
        //        case -\/(t) =>
        //          Log.log(Level.SEVERE, "Problem running ProgramExportServlet", t)
        //          throw new RuntimeException(t)
        //      }
      }
    }
  }

  sealed abstract class ResponseCode(val code: Int)
  object ResponseCode {
    case object OK extends ResponseCode(1)
    case object InvalidProgram extends ResponseCode(-1)
  }
//
//  private[ProgramExportServlet] implicit class QueryResultToJSON(val queryResult: QueryResult) extends AnyVal {
//    def toJSON: String = "test"
//  }
}
