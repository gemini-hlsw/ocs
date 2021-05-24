package edu.gemini.phase2.skeleton.servlet

import edu.gemini.phase2.core.model.{TemplateFolderExpansion, SkeletonStatus}
import edu.gemini.phase2.core.odb.TemplateFolderService
import edu.gemini.phase2.core.odb.TemplateFolderService.BaselineOption.ADD
import edu.gemini.phase2.core.odb.TemplateFolderService.TemplateOption.REPLACE
import edu.gemini.phase2.template.factory.api.{TemplateFolderExpansionFactory, TemplateFactory}
import edu.gemini.pot.sp._
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.spModel.core.{StandardProgramId, ProgramId}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.rich.pot.spdb._
import edu.gemini.spModel.template.Phase1Folder

import java.io.{BufferedOutputStream, OutputStreamWriter}
import java.util.logging.{Logger, Level}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}

import scala.collection.JavaConverters._
import scala.xml.{XML, Elem}
import java.security.Principal
import edu.gemini.spModel.core.SPProgramID


object SkeletonResetServlet {
  val LOG = Logger.getLogger(getClass.getName)
}

import Result._
import SkeletonResetServlet.LOG

/**
 */
final class SkeletonResetServlet(odb: IDBDatabaseService, templateFactory: TemplateFactory, noteText: Option[String], user: java.util.Set[Principal]) extends HttpServlet {
  override def doPut(req: HttpServletRequest, res: HttpServletResponse) {
    doPost(req, res)
  }

  override def doPost(req: HttpServletRequest, res: HttpServletResponse) {
    send(for {
      sr <- skeletonRequest(req).right
      r  <- storeSkeleton(sr).right
    } yield r, req, res)
  }

  // Contains all the information required to make the requested skeleton.
  case class SkeletonRequest(id: StandardProgramId, prog: ISPProgram, templates: TemplateFolderExpansion)

  private def skeletonRequest(req: HttpServletRequest): Either[Failure, SkeletonRequest] =
    for {
      i <- programId(req).right
      p <- lookup(i).right
      t <- expandTemplates(p).right
    } yield SkeletonRequest(i, p, t)


  private def programId(req: HttpServletRequest): Either[Failure, StandardProgramId] =
    for {
      s <- Option(req.getParameter("id")).toRight(Failure.badRequest("Missing 'id' parameter.")).right
      i <- ProgramId.parseStandardId(s).toRight(Failure.badRequest("Could not parse id '%s'.".format(s))).right
    } yield i

  private def lookup(id: StandardProgramId): Either[Failure, ISPProgram] =
    try {
      Option(odb.lookupProgramByID(id.toSp)).toRight(Failure.badRequest(s"Could not find a program with id ${id} in the database."))
    } catch {
      case ex: Exception => Left(Failure.error(ex))
    }

  private def expandTemplates(prog: ISPProgram): Either[Failure, TemplateFolderExpansion] =
    try {
      for {
        folderShell <- Option(prog.getTemplateFolder).toRight(Failure.badRequest("Program doesn't have a template folder")).right
        expansion   <- expandTemplates(Phase1Folder.extract(folderShell), prog.getProgramID()).right
      } yield expansion
    } catch {
      case ex: Exception => Left(Failure.error(ex))
    }

  private def expandTemplates(folder: Phase1Folder, pid: SPProgramID): Either[Failure, TemplateFolderExpansion] =
    TemplateFolderExpansionFactory.expand(folder, templateFactory, false, pid).left map {
      msg => Failure.badRequest(msg)
    }


  // Note, this is sent to the ODB and executed there.
  def storeFunctor(p: ISPProgram, t: TemplateFolderExpansion, note: Option[String]) = (odb: IDBDatabaseService) => {
    // Wipe out anything but the template folder.
    p.setObsComponents(List.empty[ISPObsComponent].asJava)
    p.setObservations(List.empty[ISPObservation].asJava)
    p.setGroups(List.empty[ISPGroup].asJava)
    TemplateFolderService.store(t, REPLACE, ADD, p, odb.getFactory)

    // Add a welcome note.
    note foreach {
      txt =>
        val noteComp = odb.getFactory.createObsComponent(p, SPNote.SP_TYPE, null)
        val dobj = new SPNote()
        dobj.setNote(txt)
        dobj.setTitle("COMPLETING PHASE II")
        noteComp.dataObject = dobj
        p.addObsComponent(0, noteComp)
    }
  }

  private def storeSkeleton(sr: SkeletonRequest): Either[Failure, (Success, StatusReport)] =
    odb.apply(user)(storeFunctor(sr.prog, sr.templates, noteText)).left.map(err => Failure.error(err.msg)).right map {
        _ => (Success.OK, StatusReport(sr.id.toSp, SkeletonStatus.INITIALIZED))
    }


  private def send(e: Either[Failure, (Success, StatusReport)], req: HttpServletRequest, res: HttpServletResponse) {
    e.left foreach {
      failure =>
        val msg = failure.display + failure.msg.map(m => ": " + m).getOrElse("")
        res.sendError(failure.code, msg)
    }

    e.right foreach {
      case (success, prog) =>
        res.setStatus(success.code)
        if (success == Success.CREATED) {
          res.setHeader("Location", "%s?id=%s".format(req.getRequestURL.toString, prog.id))
        }
        send(prog.toXML, res)
    }
  }

  private def send(xml: Elem, res: HttpServletResponse) {
    res.setContentType("text/xml; charset=UTF-8")
    val w = new OutputStreamWriter(new BufferedOutputStream(res.getOutputStream), "UTF-8")
    try {
      XML.write(w, xml, "UTF-8", xmlDecl = true, doctype = null)
    } catch {
      case ex: Exception => LOG.log(Level.WARNING, "problem sending response", ex)
    } finally {
      w.close()
    }
  }
}
