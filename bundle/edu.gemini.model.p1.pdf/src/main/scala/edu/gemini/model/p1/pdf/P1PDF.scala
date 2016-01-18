package edu.gemini.model.p1.pdf

import xml.{Node, XML}
import java.io._
import javax.xml.transform.URIResolver
import javax.xml.transform.stream.StreamSource
import edu.gemini.util.pdf.PDF
import io.Source

/**
 * Creator object for pdf creation for Phase1 documents.
 */
object P1PDF {

  object DEFAULT extends Template(
    "Gemini Default", "templates/xsl-default.xml",  PDF.Letter,
    Map("partner"->"gs", "pageLayout" -> "default-us-letter", "title" -> "GEMINI OBSERVATORY"))

  object AU extends Template(
    "Australian NGO", "templates/xsl-default.xml",  PDF.Letter,
    Map("partner"->"au", "pageLayout" -> "default-us-letter", "title" -> "GEMINI OBSERVATORY"))

  object CL extends Template(
    "Chilean NGO", "templates/xsl-default.xml",  PDF.Letter,
    Map("partner"->"cl", "pageLayout" -> "default-us-letter", "title" -> "PROPUESTA CONICYT-Gemini"))

  object NOAO extends Template(
    "NOAO",   "templates/xsl-NOAO.xml",    PDF.Letter,
    Map("partner"->"us"))

  case class Template(name: String, location: String, pageSize: PDF.PageSize, parameters: Map[String, String]) {
    def value(): String = name
  }

  /** Gets a list with all templates that are currently available. */
  def templates = List(DEFAULT, AU, CL, NOAO)

  def templatesMap = Map(
    "ar" -> DEFAULT,
    "au" -> AU,
    "br" -> DEFAULT,
    "ca" -> DEFAULT,
    "cl" -> CL,
    "gs" -> DEFAULT,
    "us" -> NOAO)

  /**
   * Creates a pdf from a given xml file and template and writes the resulting pdf file to the output folder.
   * This method also merges the attached pdf file to the end of the resulting pdf.
   */
  def createFromFile (xmlFile: File, template: Template, pdfFile: File) {
    try {
      createFromNode (XML.loadFile(xmlFile), template, pdfFile, Option(xmlFile.getParentFile))
    } catch {
      case ex:Exception => try {
        createFromNode (XML.loadString(Source.fromFile(xmlFile, "latin1").mkString), template, pdfFile, Option(xmlFile.getParentFile))
      }
    }
  }

  /**
   * Creates a pdf from a given xml element and template and writes the resulting pdf file to the output folder.
   * This method also merges the attached pdf file to the end of the resulting pdf.
   */
  def createFromNode (xml: Node, template: Template, pdfFile: File, workingDir:Option[File] = None) {
    val attachment = {
      val f = new File((xml \ "meta" \ "attachment").text)
      if (f.isAbsolute) f else workingDir.map(new File(_, f.getPath)).getOrElse(f)
    }
    createFromNode(xml, attachment, template, pdfFile, workingDir)
  }


  def createFromNode(xml: Node, attachment: File, template: Template, out: File, workingDir: Option[File]) {
    val xslStream = getClass.getResourceAsStream(template.location)

    try {
      val parentFilePath = Option(out.getParentFile).getOrElse("")
      val xslSource = new StreamSource(xslStream)
      val xmlSource = new StreamSource(new StringReader(xml.toString()))

      val pdf = new PDF(Some(P1PdfUriResolver))
      if (attachment.isFile) {
        val intermediateOutputFile = new File(parentFilePath + File.separator + "_" + out.getName)
        pdf.transformXslFo(xmlSource, xslSource, intermediateOutputFile, template.parameters)
        pdf.merge(List(intermediateOutputFile, attachment), out, template.pageSize)
        intermediateOutputFile.delete()
      } else {
        pdf.transformXslFo(xmlSource, xslSource, out, template.parameters)
      }
    } finally {
      xslStream.close()
    }
  }


  /**
   * This is a very crappy single purpose URI resolver. It will allow the XML libs used in the PDF bundle
   * to resolve resources imported with <xsl:import/>. This is necessary since the PDF bundle does not have
   * access to the resources in this bundle.
   */
  case object P1PdfUriResolver extends URIResolver {
    override def resolve(href: String,  base: String) = {
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

  def main(args:Array[String]) {
    val home = System.getProperty("user.home")
    val in = new File(s"$home/pitsource.xml")
    val out = new File(s"$home/pittarget.pdf")
    createFromFile(in, DEFAULT, out)

    val ok = Runtime.getRuntime.exec(Array("open", out.getAbsolutePath)).waitFor
    println("Exec returned " + ok)
    System.exit(0)
  }
}
