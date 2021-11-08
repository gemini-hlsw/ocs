package edu.gemini.phase2.template.servlet

import edu.gemini.phase2.core.model.TemplateFolderExpansion
import edu.gemini.phase2.template.factory.api.{TemplateFolderExpansionFactory, TemplateFactory}
import edu.gemini.spModel.pio.ParamSet
import edu.gemini.spModel.pio.xml.{PioXmlUtil, PioXmlFactory}
import edu.gemini.spModel.template.Phase1Folder

import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.servlet.ServletFileUpload

import java.io.{OutputStreamWriter, BufferedOutputStream}
import java.util.logging.{Level, Logger}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpServlet}

import scala.io.Source
import scala.collection.JavaConverters._
import edu.gemini.spModel.core.SPProgramID

object TemplateServlet {
  val LOG = Logger.getLogger(getClass.getName)

  def templateFolderExpansionXml(exp: TemplateFolderExpansion): String =
    PioXmlUtil.toXmlString(exp.toParamSet(new PioXmlFactory))

  def phase1Folder(xml: String): Either[Failure, Phase1Folder] =
    try {
      val pset = PioXmlUtil.read(xml).asInstanceOf[ParamSet]
      Right(Phase1Folder.fromParamSet(pset))
    } catch {
      case ex: Exception =>
        LOG.log(Level.WARNING, "Problem parsing TemplateFolder param set", ex)
        Left(Failure.badRequest("Could not read the Phase 1 template definition."))
    }
}

final class TemplateServlet(templateFactory: TemplateFactory) extends HttpServlet {
  import TemplateServlet.{LOG, phase1Folder, templateFolderExpansionXml}

  override def doGet(req: HttpServletRequest, res: HttpServletResponse) {
    send(Left(Failure.badRequest("Post a template folder to obtain the corresponding template groups and baseline calibrations")), res)
  }

  override def doPut(req: HttpServletRequest, res: HttpServletResponse) {
    doPost(req, res)
  }

  override def doPost(req: HttpServletRequest, res: HttpServletResponse) {
    send(for {
      i <- getProgramId(req).right
      l <- items(req).right
      x <- phase1FolderXml(l).right
      f <- phase1Folder(x).right
      p <- expandTemplates(f, i).right
    } yield p, res)
  }

  private val up = new ServletFileUpload(new DiskFileItemFactory)

  private def getProgramId(req: HttpServletRequest): Either[Failure, SPProgramID] =
    Option(req.getParameter("pid")) match {
      case Some(s) =>
        try Right(SPProgramID.toProgramID(s))
        catch {
          case e: Exception => Left(Failure(400, e.getMessage()))
        }
      case None => Left(Failure(400, "ProgramID parameter `pid` was not specified."))
    }

  private def items(req: HttpServletRequest): Either[Failure, List[FileItem]] =
    if (!ServletFileUpload.isMultipartContent(req))
      Left(Failure.badRequest("Template folder not found in post."))
    else
      Right(up.parseRequest(req).asScala.toList map {
        a => a.asInstanceOf[FileItem]
      })

  private def readItem[T](paramName: String, items: List[FileItem], map: FileItem => T): Either[Failure, T] =
    for {
      it <- (items find {
        _.getFieldName == paramName
      }).toRight(Failure.badRequest("Missing '%s' param.".format(paramName))).right
    } yield map(it)

  private def phase1FolderXml(items: List[FileItem]): Either[Failure, String] =
    readItem("folder", items, it => Source.fromInputStream(it.getInputStream, "UTF-8").mkString)

  private def expandTemplates(folder: Phase1Folder, pid: SPProgramID): Either[Failure, TemplateFolderExpansion] =
    TemplateFolderExpansionFactory.expand(folder, templateFactory, false, pid).left map { msg =>
      Failure.badRequest(msg)
    }

  private def send(e: Either[Failure, TemplateFolderExpansion], res: HttpServletResponse) {
    e.left foreach { failure => res.sendError(failure.code, failure.error) }
    e.right foreach { exp => send(templateFolderExpansionXml(exp), res) }
  }

  private def send(xml: String, res: HttpServletResponse) {
    res.setContentType("text/xml; charset=UTF-8")
    val w = new OutputStreamWriter(new BufferedOutputStream(res.getOutputStream), "UTF-8")
    try {
      w.write(xml)
    } catch {
      case ex: Exception => LOG.log(Level.WARNING, "problem sending response", ex)
    } finally {
      w.close()
    }
  }
}
