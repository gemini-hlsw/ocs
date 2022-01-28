package edu.gemini.model.p1.pdf

import scala.util.Try
import xml.{Node, XML}
import java.io._

import javax.xml.transform.URIResolver
import javax.xml.transform.stream.StreamSource
import edu.gemini.util.pdf.PDF

import io.Source
import scalaz._
import Scalaz._

/**
 * Creator object for pdf creation for Phase1 documents.
 */
object P1PDF {

  sealed trait PartnerLeadDisplayOption {
    def param: String
  }

  object PartnerLeadDisplayOption {
    val PartnerLeadParam = "partnerLeadDisplay"
    case object DefaultDisplay extends PartnerLeadDisplayOption {
      val param = "default"
    }
    case object NoDisplay extends PartnerLeadDisplayOption {
      val param = "no"
    }
  }

  sealed trait AttachmentId {
    def xmlName: String
  }

  object AttachmentId {
    case object FirstAttachment extends AttachmentId {
      override val xmlName = "firstAttachment"
    }
    case object SecondAttachment extends AttachmentId {
      override val xmlName = "secondAttachment"
    }
  }

  sealed trait InvestigatorsListOption {
    def param: String
  }

  object InvestigatorsListOption {
    val InvestigatorsListParam = "investigatorsList"
    case object DefaultList extends InvestigatorsListOption {
      val param = "default"
    }
    case object AtTheEndList extends InvestigatorsListOption {
      val param = "atTheEnd"
    }
    case object NoList extends InvestigatorsListOption {
      val param = "no"
    }
  }

  sealed case class Template(name: String,
                             location: String,
                             pageSize: PDF.PageSize,
                             investigatorsList: InvestigatorsListOption,
                             partnerLead: PartnerLeadDisplayOption,
                             params: Map[String, String],
                             attachments: List[AttachmentId]) {
    def value(): String = name
    val parameters: Map[String, String] = params + (InvestigatorsListOption.InvestigatorsListParam -> investigatorsList.param) + (PartnerLeadDisplayOption.PartnerLeadParam -> partnerLead.param)
  }

  object GeminiDARP extends Template(
    "Gemini DARP",
    "templates/xsl-default.xml",
    PDF.Letter,
    InvestigatorsListOption.DefaultList,
    PartnerLeadDisplayOption.DefaultDisplay,
    Map("partner"->"gs", "pageLayout" -> "default-us-letter", "title" -> "GEMINI OBSERVATORY"),
    List(AttachmentId.FirstAttachment)
  )

  object GeminiDefault extends Template(
    "Gemini Default",
    "templates/xsl-default.xml",
    PDF.Letter,
    InvestigatorsListOption.DefaultList,
    PartnerLeadDisplayOption.DefaultDisplay,
    Map("partner"->"gs", "pageLayout" -> "default-us-letter", "title" -> "GEMINI OBSERVATORY"),
    List(AttachmentId.FirstAttachment, AttachmentId.SecondAttachment)
  )

  object GeminiDefaultNoInvestigatorsList extends Template(
    "Gemini No CoIs",
    "templates/xsl-default.xml",
    PDF.Letter,
    InvestigatorsListOption.NoList,
    PartnerLeadDisplayOption.NoDisplay,
    Map("partner"->"gs", "pageLayout" -> "default-us-letter", "title" -> "GEMINI OBSERVATORY"),
    List(AttachmentId.FirstAttachment, AttachmentId.SecondAttachment)
  )

  object GeminiDefaultListAtTheEnd extends Template(
    "Gemini CoIs at End",
    "templates/xsl-default.xml",
    PDF.Letter,
    InvestigatorsListOption.AtTheEndList,
    PartnerLeadDisplayOption.NoDisplay,
    Map("partner"->"gs", "pageLayout" -> "default-us-letter", "title" -> "GEMINI OBSERVATORY"),
    List(AttachmentId.FirstAttachment, AttachmentId.SecondAttachment)
  )

  object CL extends Template(
    "Chilean NGO",
    "templates/xsl-default.xml",
    PDF.Letter,
    InvestigatorsListOption.DefaultList,
    PartnerLeadDisplayOption.DefaultDisplay,
    Map("partner"->"cl", "pageLayout" -> "default-us-letter", "title" -> "PROPUESTA CONICYT-Gemini"),
    List(AttachmentId.FirstAttachment, AttachmentId.SecondAttachment)
  )

  object NOIRLabDARP extends Template(
    "NOIRLab DARP",
    "templates/xsl-NOIRLAB.xml",
    PDF.Letter,
    InvestigatorsListOption.DefaultList,
    PartnerLeadDisplayOption.DefaultDisplay,
    Map("partner"->"us", "pageLayout" -> "default-us-letter"),
    List(AttachmentId.FirstAttachment)
  )

  /** Gets a list with all templates that are currently available. */
  def templates = List(GeminiDARP, GeminiDefault, GeminiDefaultNoInvestigatorsList, GeminiDefaultListAtTheEnd,  CL, NOIRLabDARP)

  def templatesMap: Map[String, Template] = templatesList.toMap

  def templatesList = List(
    "ar"     -> GeminiDefault,
    "br"     -> GeminiDefault,
    "ca"     -> GeminiDefaultListAtTheEnd,
    "cl"     -> CL,
    "kr"     -> GeminiDefault,
    "uh"     -> GeminiDefault,
    "da"     -> GeminiDARP,
    "gs"     -> GeminiDefault,
    "gsiend" -> GeminiDefaultListAtTheEnd,
    "gsnoi"  -> GeminiDefaultNoInvestigatorsList,
    "us"     -> NOIRLabDARP)

  /**
   * Creates a pdf from a given xml file and template and writes the resulting pdf file to the output folder.
   * This method also merges the attached pdf file to the end of the resulting pdf.
   */
  def createFromFile(xmlFile: File, template: Template, pdfFile: File): Unit = {
    Try(createFromNode(XML.loadFile(xmlFile), template, pdfFile, Option(xmlFile.getParentFile)))
      .getOrElse {
        val source = Source.fromFile(xmlFile, "latin1")
        createFromNode(XML.loadString(source.mkString), template, pdfFile, Option(xmlFile.getParentFile))
        source.close()
      }
  }

  /**
   * Creates a pdf from a given xml element and template and writes the resulting pdf file to the output folder.
   * This method also merges the attached pdf file to the end of the resulting pdf.
   */
  def createFromNode(xml: Node, template: Template, pdfFile: File, workingDir: Option[File] = None): Unit = {
    val attachments = template.attachments.map { a =>
      val f = new File((xml \ "meta" \ a.xmlName).text)
      if (f.isAbsolute) f else workingDir.map(new File(_, f.getPath)).getOrElse(f)
    }
    createFromNode(xml, attachments, template, pdfFile, workingDir)
  }


  def createFromNode(xml: Node, attachments: List[File], template: Template, out: File, workingDir: Option[File]): Unit = {
    val pdf = new PDF(Some(P1PdfUriResolver))

    def using[A, B](resource: => A)(cleanup: A => Unit)(code: A => B): Option[B] = {
      try {
        val r = resource
        try { Some(code(r)) }
        finally {
            cleanup(r)
        }
      } catch {
        case _: Exception => None
      }
    }

    def runTransformation(destination: File, template: Template): Option[File] = {
      using(getClass.getResourceAsStream(template.location))(_.close) { xslStream =>
        val xmlSource = new StreamSource(new StringReader(xml.toString()))
        val xslSource = new StreamSource(xslStream)
        pdf.transformXslFo(xmlSource, xslSource, destination, template.parameters)
        destination
      }
    }

    val parentFilePath = Option(out.getParentFile).getOrElse("")
    val intermediateOutputFile = new File(parentFilePath + File.separator + "_" + out.getName)
    val intermediateILFile = new File(parentFilePath + File.separator + "_" + out.getName + "_il")

    val maybeAttachment = attachments.filter(_.isFile)
    val filesToMerge: Option[List[File]] = template.investigatorsList match {
      case InvestigatorsListOption.AtTheEndList =>
        val main = runTransformation(intermediateOutputFile, template.copy(investigatorsList = InvestigatorsListOption.NoList))
        val investigatorsList = runTransformation(intermediateILFile, template)
        (investigatorsList |@| main)((m, i) => m :: (maybeAttachment :+ i))
      case _                                   =>
        val main = runTransformation(intermediateOutputFile, template)
        main.map(_ :: maybeAttachment)
    }
    pdf.merge(filesToMerge.getOrElse(Nil), out, template.pageSize)
    intermediateOutputFile.delete()
    intermediateILFile.delete()
  }


  /**
   * This is a very crappy single purpose URI resolver. It will allow the XML libs used in the PDF bundle
   * to resolve resources imported with <xsl:import/>. This is necessary since the PDF bundle does not have
   * access to the resources in this bundle.
   */
  case object P1PdfUriResolver extends URIResolver {
    override def resolve(href: String,  base: String): StreamSource = {
      val r = Option(getClass.getResourceAsStream(href))
      // try to resolve as a normal resource (assuming that caller closes stream?)
      r.map(new StreamSource(_)).getOrElse {
        if (new File(href).exists) {
          new StreamSource(new File(href)) // try to resolve as a file
        } else {
          new StreamSource(href)           // try to resolve as a URL
        }
      }
    }
  }

  def main(args:Array[String]): Unit = {
    val home = System.getProperty("user.home")
    val in = new File(s"$home/pitsource.xml")
    val out = new File(s"$home/pittarget.pdf")
    createFromFile(in, GeminiDARP, out)

    val ok = Runtime.getRuntime.exec(Array("open", out.getAbsolutePath)).waitFor
    println("Exec returned " + ok)
    System.exit(0)
  }
}
