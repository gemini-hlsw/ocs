package edu.gemini.phase2.skeleton.auxfile

import edu.gemini.auxfile.api.AuxFile
import edu.gemini.auxfile.copier.AuxFileCopier
import edu.gemini.model.p1.immutable.{Meta, ProposalIo, Proposal}
import edu.gemini.model.p1.pdf.P1PDF
import edu.gemini.shared.util.immutable.ImOption
import edu.gemini.spModel.core.SPProgramID

import java.io.File
import java.time.Instant
import scala.xml.XML

object SkeletonAuxfileWriter {

  case class ProposalFiles(progId: SPProgramID, auxfileDir: File) {
    val proposalXml           = mkFile("proposal",   "xml")
    val proposalAttachmentPdf = mkFile("attachment", "pdf")
    val proposalSummaryPdf    = mkFile("summary",    "pdf")

    def proposalAuxfile: AuxFile =
      auxfile(proposalXml, "Proposal Document")

    def proposalAttachmentAuxfile: AuxFile =
      auxfile(proposalAttachmentPdf, "Proposal Attachment")

    def proposalSummaryAuxfile: AuxFile =
      auxfile(proposalSummaryPdf, "Proposal Summary")

    def auxfiles: List[AuxFile] =
      List(proposalAuxfile, proposalAttachmentAuxfile, proposalSummaryAuxfile)

    private def auxfile(f: File, desc: String): AuxFile =
      new AuxFile(progId, f.getName, desc, f.length(), f.lastModified(), false, ImOption.empty[Instant])

    private def mkFile(suffix: String, extension: String): File =
      new File(auxfileDir, "%s_%s.%s".format(progId.toString, suffix, extension))
  }

  def write(id: SPProgramID, proposal: Proposal, pdfAttachment: File, auxfileDir: File, copier:AuxFileCopier): Either[FileError, List[AuxFile]] = {
    // Get the proposal files that will be written.
    val files = ProposalFiles(id, auxfileDir)

    // Update the proposal attachment name.  Use a path relative to the cwd.
    val updatedProposal = proposal//(Proposal.meta andThen Meta.attachment).set(proposal, Some(new File(files.proposalAttachmentPdf.getName)))

    // Write the files to the auxfile dir.
    for {
      _ <- auxfileDir.eCreateWritableDir.right
      _ <- new File(auxfileDir, "meta").eCreateWritableDir.right

      _ <- writeProposal(updatedProposal, files.proposalXml).right
      _ <- writeMeta(files.proposalXml, files.proposalAuxfile).right

      _ <- writeAttachment(pdfAttachment, files.proposalAttachmentPdf).right
      _ <- writeMeta(files.proposalAttachmentPdf, files.proposalAttachmentAuxfile).right

      _ <- writeSummary(updatedProposal, pdfAttachment, files.proposalSummaryPdf).right
      _ <- writeMeta(files.proposalSummaryPdf, files.proposalSummaryAuxfile).right

      _ <- sftp(id,files,copier).right
    } yield files.auxfiles


  }

  private def sftp(id: SPProgramID, files: ProposalFiles, copier: AuxFileCopier): Either[FileError, Unit] = {
    for {
      _ <- sftp(id, files.proposalXml, copier).right
      _ <- sftp(id, files.proposalAttachmentPdf, copier).right
      _ <- sftp(id, files.proposalSummaryPdf, copier).right
    } yield Unit
  }

  private def sftp(id: SPProgramID, file: File, copier: AuxFileCopier): Either[FileError, Unit] = {
    if (copier.copy(id, file)) {
      Right(())
    } else {
      Left(FileError(file, "Error sftp'ing file"))
    }
  }

  private def writeProposal(p: Proposal, f: File): Either[FileError, File] =
    writeTo(f) { tmp => tryWriteProposal(p, tmp) }

  // Wraps the PropoalIo call in a try/catch block.
  private def tryWriteProposal(p: Proposal, f: File): Either[FileError, Unit] =
    try {
      Right(ProposalIo.write(p, f))
    } catch {
      case ex: Exception => Left(FileError(f, ex))
    }

  private def writeAttachment(in: File, out: File): Either[FileError, File] =
    writeTo(out) { tmp => in.eCopyTo(tmp) }

  private def writeSummary(p: Proposal, attachment: File, out: File): Either[FileError, File] = {
    writeTo(out) { tmp =>
      try {
        val xml = ProposalIo.writeToXml(p)
        P1PDF.createFromNode(xml, attachment, P1PDF.GeminiDefault, tmp, None)
        Right(())
      } catch {
        case ex: Exception => Left(FileError(out, ex))
      }
    }
  }

  // Performs the given file op on a new temporary file, which is only moved
  // to the final destination if successful.
  private def writeTo(dest: File)(fileOp: File => Either[FileError, Unit]): Either[FileError, File] = {
    val tmp = File.createTempFile("auxfile", "tmp", dest.getParentFile)
    try {
      for {
        _ <- fileOp(tmp).right
        _ <- tmp.eMoveTo(dest).right
      } yield dest
    } finally {
      if (tmp.exists()) tmp.delete()
    }
  }

  private def meta(aux: AuxFile): xml.Node =
<paramset name="meta">
  <param name="checked" value="false"/>
  <param name="description" value={aux.getDescription}/>
</paramset>

  private def writeMeta(f: File, aux: AuxFile): Either[FileError, Unit] = {
    val metadir  = new File(f.getParentFile, "meta")
    assume(metadir.exists)

    val metafile = new File(metadir, f.getName + ".meta")
    try {
      Right(XML.save(metafile.getPath, meta(aux), "UTF-8"))
    } catch {
      case ex: Exception => Left(FileError(metafile, ex))
    }
  }


}
