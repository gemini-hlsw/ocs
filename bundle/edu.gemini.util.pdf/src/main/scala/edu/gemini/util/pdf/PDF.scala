package edu.gemini.util.pdf

import org.apache.fop.apps.{FopFactory}
import org.apache.xmlgraphics.util.MimeConstants
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.{URIResolver, Source, TransformerFactory}
import java.io._
import com.itextpdf.text.pdf.{PdfReader, PdfWriter}
import com.itextpdf.text.{Document, PageSize => ItextPageSize}

object PDF {
  sealed trait PageSize
  case object A4 extends PageSize
  case object Letter extends PageSize
}

/** Utility functions for handling PDF files. */
class PDF(uriResolver: Option[URIResolver] = None) {

  /**
   * Transforms an xml to a pdf file using xsl-fo templates.
   */
  def transformXslFo(xmlSource: Source, xslFoSource: Source,  pdfFile: File, parameters: Map[String, AnyRef] = Map()) {
    val fopFactory = FopFactory.newInstance
    val foUserAgent = fopFactory.newFOUserAgent

    val out = new BufferedOutputStream(new FileOutputStream(pdfFile))
    try {
      // Construct fop with desired output format
      val fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out)

      // Setup XSLT
      val factory = TransformerFactory.newInstance()
      if (uriResolver.isDefined) factory.setURIResolver(uriResolver.get)

      val transformer = factory.newTransformer(xslFoSource)
      if (uriResolver.isDefined) transformer.setURIResolver(uriResolver.get)
      parameters.foreach (p => transformer.setParameter(p._1, p._2))

      // Resulting SAX events (the generated FO) must be piped through to FOP
      val  res = new SAXResult(fop.getDefaultHandler())

      // Start XSLT transformation and FOP processing
      transformer.transform(xmlSource, res)

    } finally {
      out.close()
    }
  }

  /**
   * Merges using itext. Lean and mean merging machine.
   */
  def merge(srcFiles: List[File], target: File, pageSize: PDF.PageSize) {
    val document = new Document(if (pageSize == PDF.Letter) ItextPageSize.LETTER else ItextPageSize.A4)
    val out = new FileOutputStream(target)
    try {
      val writer = PdfWriter.getInstance(document, out)
      document.open
      val cb = writer.getDirectContent
      srcFiles.foreach (file => {
        val in = new FileInputStream(file)
        try {
          val reader = new PdfReader(in)
          for (i <- 1 to reader.getNumberOfPages) {
            document.newPage
            val page = writer.getImportedPage(reader, i)
            cb.addTemplate(page, 0, 0)
          }
        }
        finally {
          in.close
        }
      })
    }
//    catch {
//      case e => System.out.print(e)
//
//    }
    finally {
      out.flush
      document.close
      out.close
    }
  }

}
