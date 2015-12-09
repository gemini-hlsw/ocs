package edu.gemini.pit.util

import org.specs2.mutable.{After, Specification}
import java.io.{FileInputStream, FileOutputStream, File}

import scalaz._
import Scalaz._

class PDFSpec extends Specification {
  val tempDir = System.getProperty("java.io.tmp")

  "The PDF Object" should {
    "read absolute path PDF files" in new proposalsFiles {
      PDF.isPDF(Option(xml), pdf.getAbsoluteFile) must beTrue
    }
    "read relative files on the same dir if absolute is missing" in new proposalsFiles {
      // The absolute file is missing but should try on the same dir as the xml
      val badLocation = new File(new File(tempDir, "nested"), pdf.getName)
      PDF.isPDF(Option(xml), badLocation.getAbsoluteFile) must beTrue
    }
    "read relative files" in new proposalsFiles {
      PDF.isPDF(Option(xml), new File(pdf.getName)) must beTrue
    }
    "relocate to the existing absolute file" in new proposalsFiles {
      PDF.relocatedPdf(Option(xml), pdf.getAbsoluteFile.some) must beSome(pdf.getAbsoluteFile)
    }
    "relocate to the existing relative file" in new proposalsFiles {
      PDF.relocatedPdf(Option(xml), new File(pdf.getName).some) must beSome(pdf.getAbsoluteFile)
    }
    "relocate to the file on the same dir if absolute is missing" in new proposalsFiles {
      val badLocation = new File(new File(tempDir, "nested"), pdf.getName)
      // The absolute file is missing but should try on the same dir as the xml
      PDF.relocatedPdf(Option(xml), badLocation.getAbsoluteFile.some) must beSome(pdf.getAbsoluteFile)
    }
    "relocate to none if none is passed" in new proposalsFiles {
      PDF.relocatedPdf(Option(xml), None) must beNone
    }
  }

  trait proposalsFiles extends After {
    val pdf = new File(getClass.getResource("sample.pdf").getFile)
    // We can reuse the pdf location as xml as we are only interested on the path to the xml
    val xml = new File(pdf.getParentFile.getAbsolutePath, "sample.xml")

    val movedXml = copyFile(pdf, new File(tempDir, "sample.xml"))
    val movedPdf = copyFile(pdf, new File(tempDir, "sample.pdf"))
    def after = {
      movedXml.delete()
      movedPdf.delete()
    }
  }

  def copyFile(src: File, dest: File):File = {
    new FileOutputStream(dest) getChannel() transferFrom(new FileInputStream(src) getChannel, 0, Long.MaxValue)
    dest
  }
}
