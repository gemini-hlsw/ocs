package edu.gemini.p1monitor

import config.P1MonitorConfig
import java.io.{File, FileInputStream, FileOutputStream}
import java.util.logging.{Level, Logger}

import edu.gemini.model.p1.pdf.P1PDF
import edu.gemini.model.p1.immutable.{FastTurnaroundProgramClass, Proposal, ProposalIo}
import edu.gemini.model.p1.pdf.P1PDF.InvestigatorsListOption

class P1MonitorDirMonitor(cfg: P1MonitorConfig) extends DirListener {
  val LOG: Logger = Logger.getLogger(this.getClass.getName)
  val mailer: P1MonitorMailer = new P1MonitorMailer(cfg)

  //One directory scanner per directory
  val dirScanner: Traversable[DirScanner] = cfg.getDirectories map {
    monDir => new DirScanner(monDir)
  }

  def startMonitoring(): Unit = {
    dirScanner.foreach {
      _.startMonitoring(this)
    }
  }

  def stopMonitoring(): Unit = {
    dirScanner.foreach {
      _.stopMonitoring()
    }
  }

  def dirChanged(evt: DirEvent): Unit = {
    // Make a original dir in not existing
    val originalsDir = new File(evt.dir.dir, "original")
    if (!originalsDir.exists()) {
      originalsDir.mkdirs()
    }

    def writeUpdated(attachment1: File, attachment2: Option[File], proposal: Proposal, f: File):Proposal = {
      // Use just the name to avoid hardcoding the file
      val attachedFileName1 = attachment1.getName
      LOG.info(s"First attachment file changed to $attachedFileName1")
      val attachedFileName2 = attachment2.map(_.getName)
      attachedFileName2.map(a => LOG.info(s"First attachment file changed to $a"))
      val p = proposal.copy(meta = proposal.meta.copy(firstAttachment = Some(new File(attachedFileName1)), secondAttachment = attachedFileName2.map(new File(_))))
      ProposalIo.write(p, f)
      p
    }

    def updateAttachmentLink(newPDFile1: Option[File], newPDFile2: Option[File], f: File):Option[Proposal] = {
      // If we have a PDF we must replace the attachment name
      newPDFile1.flatMap {
        p =>
          // This sounds like a good idea, let Proposal to do the IO, but this will need some help to avoid writing
          // the absolute path of the file
          (for {
            conversion <- ProposalIo.readAndConvert(f)
          } yield writeUpdated(p, newPDFile2, conversion.proposal, f)).toOption
      }
    }

    def copyAndProcessFile(xml: File): Iterable[ProposalFileGroup] =
      cfg.translations.collect {
        case (s, d) if xml.getName.startsWith(s) => (s, d)
      }.flatMap {t =>
        val pdfName1 = xml.getName.replaceAll(".xml", ".pdf")
        val pdfOpt1 = evt.newFiles.find(_.getName.equalsIgnoreCase(pdfName1))
        val pdfName2 = xml.getName.replaceAll(".xml", "_stage2.pdf")
        val pdfOpt2 = evt.newFiles.find(_.getName.equalsIgnoreCase(pdfName2))
        val replacedName = new File(xml.getParentFile.getAbsolutePath, xml.getName.replaceAll(t._1, t._2))

        val newXMLFile = copyFile(xml, replacedName)
        val newPDFile1 = pdfOpt1.flatMap {
          pdf => copyFile(pdf, new File(pdf.getParentFile.getAbsolutePath, pdf.getName.replaceAll(t._1, t._2)))
        }
        val newPDFile2 = pdfOpt2.flatMap {
          pdf => copyFile(pdf, new File(pdf.getParentFile.getAbsolutePath, pdf.getName.replaceAll(t._1, t._2)))
        }

        val fg: Option[ProposalFileGroup] = newXMLFile.flatMap {
          f: File =>
            val proposal = updateAttachmentLink(newPDFile1, newPDFile2, f)
            val isFT: Boolean = proposal.exists(_.proposalClass.isInstanceOf[FastTurnaroundProgramClass])

            val r: Option[ProposalFileGroup] = try {
              val summaryFile = new File(f.getAbsolutePath.substring(0, f.getAbsolutePath.length - 4) + "_summary.pdf")
              // Find the template to use
              val template = cfg.map.find(_._2.dir == f.getParentFile.getAbsoluteFile).map(_._2.template).getOrElse(P1PDF.GeminiStandard)
              // REL-3772: Do not display PI information for FT proposals.
              val workingTemplate = if (isFT) template.copy(investigatorsList = InvestigatorsListOption.NoList) else template
              P1PDF.createFromFile(f, workingTemplate, summaryFile)
              LOG.info(s"Build summary report of $f at $summaryFile with template $template")
              Some(ProposalFileGroup(Some(f), newPDFile1, newPDFile2, Some(summaryFile)))
            } catch {
              case ex: Exception =>
                LOG.log(Level.SEVERE, "Problem processing file " + xml.getName, ex)
                None
            }
            r
        }

        // Move originals out of the way
        xml.renameTo(new File(originalsDir, xml.getName))
        pdfOpt1.foreach {
          f => f.renameTo(new File(originalsDir, f.getName))
        }
        pdfOpt2.foreach {
          f => f.renameTo(new File(originalsDir, f.getName))
        }
        fg
      }

    //For every XML file, try to get the matching PDF and create the PDF summary, if something fails, just continue
    //with just the XML
    val fileGroups: Traversable[ProposalFileGroup] = evt.newFiles.collect {
      case xml if xml.getName.endsWith(".xml") => copyAndProcessFile(xml)
    }.flatten

    //notify of all the new proposals
    fileGroups.foreach {
      mailer.notify(evt.dir.name, _)
    }

  }

  def copyFile(src:File, dest:File):Option[File] = {
    require (src != null)
    require (dest != null)
    src match {
      case _ if src.exists() && src != dest =>
        LOG.info(s"Copy file $src to $dest")
        new FileOutputStream(dest).getChannel.transferFrom(new FileInputStream(src).getChannel, 0, Long.MaxValue)
        Some(dest)
      case s if src.exists() && src == dest =>
        LOG.info(s"Destination for $src, $dest already in place")
        Some(s)
      case _                                =>
        LOG.info(s"It seems src does not exist: $src")
        None
    }
  }


}

case class ProposalFileGroup(xml: Option[File], pdf1: Option[File], pdf2: Option[File], summary: Option[File])
