package edu.gemini.programexport.servlet

import java.io.BufferedOutputStream
import java.security.Principal
import java.util.logging.{Level, Logger}
import javax.servlet.ServletException
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.SPProgramID
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
import edu.gemini.spModel.config.ConfigBridge
import edu.gemini.spModel.config.map.ConfigValMapInstances
import edu.gemini.spModel.seqcomp.SeqBase

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
  // Right now we just return an empty String: whether to use Strings or Argonaut to build the
  // JSON is a decision I'm leaving to the end.
  def build(id: SPProgramID, response: HttpServletResponse): Unit =
    Option(odb.lookupProgramByID(id)) match {
      case Some(ispProgram) =>
        val out: BufferedOutputStream = new BufferedOutputStream(response.getOutputStream)
        response.setContentType("text/plain")

        // TODO: Remove
        out.write(s"Program name received: $id\n".getBytes)

        // Start traversing the tree.
        process(ispProgram).foreach { case (k, j) => out.write(s"{$k : $j}\n".getBytes) }
        out.close()

      case None =>
        val error = s"Program $id is not in the database!"
        Log.severe(error)
        throw new ServletException(error)
    }

  // Recursive method to build up the JSON representation of the program.
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

  def simpleNode(n: ISPNode): Option[JsonAssoc] =
    for {
      t <- n.dataObject.map(_.getType)
      j <- componentFields(n)
    } yield (
      t.name,
      n.children.foldLeft(("key" := n.getNodeKey.toString) ->: j) { (jp, c) =>
        process(c) ->?: jp
      }
    )

  def componentFields(n: ISPNode): Option[Json] =
    n.dataObject.flatMap {
      case p: SPProgram => Some(programFields(p))
      case n: SPNote => Some(noteFields(n))
      case g: SPGroup => Some(groupFields(g))
      case o: SPObservation => Some(observationFields(o))
      case s: SPSiteQuality => Some(siteQualityFields(s))
      // etc. etc.

      case _ => None

    }

  def programFields(p: SPProgram): Json = {
    val timeAcctAllocation = p.getTimeAcctAllocation

    ("piFirstName" := p.getPIFirstName) ->:
      ("piLastName" := p.getPILastName) ->:
      ("piEmail" := p.getPIInfo.getEmail) ->:
      ("investigators" := p.getGsaPhase1Data.getCois.asScala.zipWithIndex.foldLeft(Json.jEmptyObject) { case (j, (inv, idx)) =>
        // TODO: Not sure if this is the correct format and will give us the right results?
        // TODO: I want investigatorNumber to contain an associative array with the three fields
        // TODO: and investigators to contain an associative array of investigatorNumber.
        // TODO: Including the j in the parentheses seems incorrect.
        ("investigatorNumber" := idx) ->: (
          ("investigatorFirstName" := inv.getFirst) ->:
            ("investigatorLastName" := inv.getLast) ->:
            ("investigatorEmail" := inv.getEmail) ->:
            j)
      }) ->:
      ("affiliate" :=? Option(p.getPIAffiliate).map(_.displayValue)) ->?:
      ("queueBand" := p.getQueueBand) ->:
      ("rolloverFlag" := p.getRolloverStatus) ->:
      ("isThesis" := p.isThesis.toYesNo.displayValue) ->:
      ("programMode" := p.getProgramMode.displayValue) ->:
      ("tooType" := p.getTooType.getDisplayValue) ->:
      // TODO: TimeValue: Bryan said we wanted ms?
      ("awardedTime" := p.getAwardedProgramTime.getMilliseconds) ->:
      ("timeAccountAllocationCategories" := timeAcctAllocation.getCategories.asScala.foldLeft(Json.jEmptyObject) { case (j, c) =>
        val award = timeAcctAllocation.getAward(c)
        // TODO: See above comments for investigators.
        ("category" := c.getDisplayName) ->: (
          // TODO: I think Bryan wants ms? We have Duration.
          ("programTime" := award.getProgramAward.getSeconds) ->:
            ("partnerTime" := award.getPartnerAward.getSeconds) ->:
            j)
      }) ->:
      Json.jEmptyObject
  }

  def noteFields(n: SPNote): Json =
    ("title" := n.getTitle) ->: ("text" := n.getNote) ->: jEmptyObject

  def siteQualityFields(s: SPSiteQuality): Json =
    ("cc" := s.getCloudCover.displayValue) ->:
      ("sb" := s.getSkyBackground.displayValue) ->:
      ("iq" := s.getImageQuality.displayValue) ->:
      ("elevationConstraintType" := s.getElevationConstraintType.displayValue) ->:
      ("elevationConstraintMin" := s.getElevationConstraintMin) ->:
      ("elevationConstraintMax" := s.getElevationConstraintMax) ->:
      ("timingWindows" := s.getTimingWindows.asScala.zipWithIndex.foldLeft(jEmptyObject) { case (j, (tw, idx)) =>
        ("windowIndex" := idx) ->: (
          ("start" := tw.getStart) ->:
            ("duration" := tw.getDuration) ->:
            ("period" := tw.getPeriod) ->:
            ("repeat" := tw.getRepeat) ->:
            j)
      }) ->:
      jEmptyObject

  // TODO: Here, we need the key and the observation node keys, which is only in the ISPGroup.
  def groupFields(g: SPGroup): Json =
    ("name" := g.getTitle) ->: jEmptyObject

  // TODO: Again, here we need the node key.
  def observationFields(o: SPObservation): Json =
    ("title" := o.getTitle) ->:
      ("phase2Status" := o.getPhase2Status.displayValue) ->:
      ("execStatusOverride)" :=? o.getExecStatusOverride.asScalaOpt.map(_.displayValue)) ->?:
      ("priority" := o.getPriority.displayValue) ->:
      ("tooOverrideRapid" := o.isOverrideRapidToo.toYesNo.displayValue) ->:
      // TODO: Unsure if this is the way to handle this data.
      // TODO: SPObservation only has a setup time type, which is an enum in SetupTime,
      // TODO: where the Durations are actually stored.
      // TODO: Apparently, this is in the instruments by the description.
      ("setupTime" := o.getSetupTimeType.toString) ->:
      // TODO: Also need acquisition overhead here. Where is this?
      jEmptyObject

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

//("timeAccountAllocationCategories" := timeAcctAllocation.getCategories.asScala.foldLeft(Json.jEmptyObject) { case (j, c) =>
//val award = timeAcctAllocation.getAward(c)
//// TODO: See above comments for investigators.
//("category" := c.getDisplayName) ->: (
//// TODO: I think Bryan wants ms? We have Duration.
//("programTime" := award.getProgramAward.getSeconds) ->:
//("partnerTime" := award.getPartnerAward.getSeconds) ->:
//j)
//}) ->:
//Json.jEmptyObject

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
