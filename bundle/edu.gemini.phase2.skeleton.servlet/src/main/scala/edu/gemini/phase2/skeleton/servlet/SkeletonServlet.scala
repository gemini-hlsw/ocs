package edu.gemini.phase2.skeleton.servlet

import edu.gemini.phase2.core.model.{TemplateFolderExpansion, SkeletonStatus, SkeletonShell}
import edu.gemini.phase2.core.odb.SkeletonStoreResult._
import edu.gemini.phase2.core.odb.SkeletonStoreService
import edu.gemini.phase2.skeleton.factory.{SpProgramFactory, Phase1FolderFactory}
import edu.gemini.phase2.template.factory.api.{TemplateFolderExpansionFactory, TemplateFactory}
import edu.gemini.pot.spdb.IDBDatabaseService
import edu.gemini.spModel.core.{StandardProgramId, ProgramId, SPProgramID}
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.spModel.rich.pot.spdb._
import edu.gemini.spModel.template.Phase1Folder

import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.servlet.ServletFileUpload

import java.io.{File, BufferedOutputStream, OutputStreamWriter}
import java.util.logging.{Level, Logger}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}

import scala.collection.JavaConverters._
import scala.io.Source
import scala.xml.{XML, Elem}
import edu.gemini.model.p1.immutable.{ProposalConversion, ProposalIo, Proposal}
import edu.gemini.phase2.skeleton.auxfile.SkeletonAuxfileWriter
import edu.gemini.spModel.obscomp.SPNote
import edu.gemini.auxfile.copier.AuxFileCopier
import edu.gemini.auxfile.api.AuxFile
import scalaz.NonEmptyList
import java.security.Principal


object SkeletonServlet {
  val LOG = Logger.getLogger(getClass.getName)
}

import Result._
import SkeletonServlet.LOG

/**
 * A servlet that accepts posted proposals and adds them to the database (or
 * replaces them).
 *
 * Parameters
 * <ul>
 * <li><b>proposal</b> - proposal document which must contain the
 * ITAC acceptance information</li>
 * <li><b>attachment</b> - proposal PDF "text-part" attachment</li>
 * </ul>
 *
 * Potential HTTP status codes that should be expected are detailed in
 * Result.scala.
 */
final class SkeletonServlet(odb: IDBDatabaseService, templateFactory: TemplateFactory, auxfileRoot: File, noteText: Option[String], copier:AuxFileCopier, user: java.util.Set[Principal]) extends HttpServlet {
  override def doPut(req: HttpServletRequest, res: HttpServletResponse) {
    doPost(req, res)
  }

  override def doPost(req: HttpServletRequest, res: HttpServletResponse) {

    // Get the requested information (which generates a PDF attachment file as
    // a side-effect)
    val eSkeleton = skeletonRequest(req)

    try {
      // Try to store the skeleton and write out the pdfs
      val e = for {
        s <- eSkeleton.right
        a <- writeAuxfiles(s).right
        r <- storeSkeleton(s.shell, s.templates).right
      } yield r

      send(e, req, res)

    } finally {
      // Cleanup PDF attachment file.
      eSkeleton.right foreach { s => s.attachment.delete() }
    }
  }

  // Contains all the information required to make the requested skeleton.
  case class SkeletonRequest(id: StandardProgramId, prop: Proposal, attachment: File, shell: SkeletonShell, templates: TemplateFolderExpansion)

  private def skeletonRequest(req: HttpServletRequest): Either[Failure, SkeletonRequest] =
    for {
      l       <- items(req).right
      convert <- convertParameter(req).right
      x       <- proposalString(l).right
      p       <- proposal(x, convert).right
      i       <- programId(l, p).right
      s       <- makeSkeleton(i, p).right
      t       <- expandTemplates(s.folder).right
      a       <- pdfAttachment(l).right
    } yield SkeletonRequest(i, p, a, s, t)

  private val up = new ServletFileUpload(new DiskFileItemFactory)

  private def items(req: HttpServletRequest): Either[Failure, List[FileItem]] =
    if (!ServletFileUpload.isMultipartContent(req))
      Left(Failure.badRequest("Proposal and pdf attachment not found in post."))
    else
      Right(up.parseRequest(req).asScala.toList map {
        a => a.asInstanceOf[FileItem]
      })

  private def proposalString(items: List[FileItem]): Either[Failure, String] =
    readItem("proposal", items, it => Source.fromInputStream(it.getInputStream, "UTF-8").mkString)

  private def proposalConversionFailure(result: Either[Exception, NonEmptyList[String]]): Either[Failure, Proposal] = {
    val failureMessage = result.fold(ex => ex.getMessage, changes => changes.toString())
    LOG.log(Level.WARNING, s"Exception reading proposal: $failureMessage")

    Left(Failure.badRequest(s"Could not process proposal XML: $failureMessage"))
  }

  private def proposalConversionCheck(convert: Boolean)(conversion: ProposalConversion): Either[Failure, Proposal] =
    if (convert || !conversion.transformed) {
      Right(conversion.proposal)
    } else {
      Left(Failure.badRequest(s"Not allowed to accept proposals from semester: ${conversion.from.display}. Retry with the 'convert' flag."))
    }

  private def proposal(s: String, convert: Boolean): Either[Failure, Proposal] =
    ProposalIo.readAndConvert(s).fold(proposalConversionFailure, proposalConversionCheck(convert))

  // Determine the program id.  If an id is provided explicitly as a servlet
  // parameter, use it.  Otherwise, an itac element must be present in the
  // proposal and it must have a gemini id assigned or else we cannot continue.
  private def programId(items: List[FileItem], p: Proposal): Either[Failure, StandardProgramId] =
    for {
      e <- explicitId(items).right
      i <- (e orElse readItacGeminiId(p)).toRight(Failure.badRequest("Proposal missing Gemini program id and no explicit program id provided.")).right
    } yield i

  // An explicit id may be provided.  If present, it must be parseable or it is
  // considered an error.
  private def explicitId(items: List[FileItem]): Either[Failure, Option[StandardProgramId]] =
    items.find(_.getFieldName == "id").map(_.getString) match {
      case None    => Right(None)  // okay, not present
      case Some(s) => ProgramId.parseStandardId(s).toRight(Failure.badRequest(s"Could not parse id '$s'")).right.map {id => Some(id) }
    }

  private def readItacGeminiId(p: Proposal): Option[StandardProgramId] =
    for {
      i <- p.proposalClass.itac
      s <- i.decision.flatMap(_.right.map(_.programId).right.toOption)
      p <- ProgramId.parseStandardId(s)
    } yield p

  private def pdfAttachment(items: List[FileItem]): Either[Failure, File] =
    readItem("attachment", items, it => {
      val tmp = File.createTempFile("pdfattachment", ".pdf")
      it.write(tmp)
      tmp
    })

  private def readItem[T](paramName: String, items: List[FileItem], map: FileItem => T): Either[Failure, T] =
    for {
      it <- (items find {
        _.getFieldName == paramName
      }).toRight(Failure.badRequest(s"Missing '$paramName' param.")).right
    } yield map(it)


  // Note, this is sent to the ODB and executed there.
  def storeFunctor(s: SkeletonShell, t: TemplateFolderExpansion, note: Option[String]) = (odb: IDBDatabaseService) => {
    val rap = SkeletonStoreService.store(s, t, odb)
    val (result, program) = (rap.result, rap.program)

    // Add a welcome note.
    note foreach { txt =>
      val noteComp = odb.getFactory.createObsComponent(program, SPNote.SP_TYPE, null)
      val dobj = new SPNote()
      dobj.setNote(txt)
      dobj.setTitle("COMPLETING PHASE II")
      noteComp.dataObject = dobj
      program.addObsComponent(0, noteComp)
    }

    result
  }

  private def storeSkeleton(s: SkeletonShell, t: TemplateFolderExpansion): Either[Failure, (Success, StatusReport)] =
    odb.apply(user)(storeFunctor(s, t, noteText)).left.map(err => Failure.error(err.msg)).right flatMap {
      case REJECTED => Left(Failure.rejected)
      case CREATED  => Right((Success.CREATED, StatusReport(s.id, SkeletonStatus.INITIALIZED)))
      case UPDATED  => Right((Success.OK,      StatusReport(s.id, SkeletonStatus.INITIALIZED)))
    }

  private def makeSkeleton(id: StandardProgramId, p: Proposal): Either[Failure, SkeletonShell] =
    (for {
      s <- id.site.toRight("Program ID has no Site: " + id).right
      f <- Phase1FolderFactory.create(s, p).right
    } yield new SkeletonShell(id.toSp, SpProgramFactory.create(p), f)).left map { Failure.badRequest }

  private def expandTemplates(folder: Phase1Folder): Either[Failure, TemplateFolderExpansion] =
    TemplateFolderExpansionFactory.expand(folder, templateFactory, false).left map { msg =>
      Failure.badRequest(msg)
    }

  private def parseId(id: String): Either[Failure, SPProgramID] =
    try {
      Right(SPProgramID.toProgramID(id))
    } catch {
      case _: Exception => Left(Failure.badRequest(s"Couldn't parse id '$id'"))
    }

  private def idParameter(req: HttpServletRequest): Either[Failure, SPProgramID] =
    for {
      s <- Option(req.getParameter("id")).toRight(Failure.badRequest("Missing 'id' parameter")).right
      i <- parseId(s).right
    } yield i

  private def convertParameter(req: HttpServletRequest): Either[Failure, Boolean] =
    for {
      s <- Right(Option(req.getParameter("convert"))).right
      i <- parseConvert(s).right
    } yield i

  private def parseConvert(convert: Option[String]): Either[Failure, Boolean] = Right(convert.isDefined)

  private def writeAuxfiles(req: SkeletonRequest): Either[Failure, List[AuxFile]] = {
    val dir = new File(auxfileRoot, req.id.toString)
    SkeletonAuxfileWriter.write(req.id.toSp, req.prop, req.attachment, dir, copier).left map { err =>
      Failure.error(s"Problem writing auxiliary file ${err.file.getName}: ${err.exception.getMessage}")
    }
  }

  import edu.gemini.phase2.core.odb.SkeletonStatusService.getStatus

  override def doGet(req: HttpServletRequest, res: HttpServletResponse) {
    send(for {
      id     <- idParameter(req).right
      status <- odb.apply(user)(getStatus(_, id)).left.map(err => Failure.error(err.msg)).right
    } yield (Success.OK, StatusReport(id, status)), req, res)
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
          res.setHeader("Location", s"${req.getRequestURL.toString}?id=${prog.id}")
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
