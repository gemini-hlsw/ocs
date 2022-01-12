package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{mutable => M}

import java.io._
import javax.xml.bind.{UnmarshalException, Unmarshaller, Marshaller, JAXBContext}
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import scala.io.Source
import xml.{Node, XML}
import java.util.logging.Logger
import transform.{UpConverter, ConversionResult}
import scalaz._
import Scalaz._

case class ProposalConversion(transformed: Boolean, from: Semester, changes: Seq[String], proposal:Proposal)

object ProposalIo {
  private val LOG = Logger.getLogger(getClass.getName)

  private val context: JAXBContext = {
    val factory        = new M.ObjectFactory
    val contextPackage = factory.getClass.getName.reverse.dropWhile(_ != '.').drop(1).reverse
    JAXBContext.newInstance(contextPackage, ProposalIo.getClass.getClassLoader)
  }

  private def marshaller: Marshaller = {
    val m = context.createMarshaller
    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, java.lang.Boolean.TRUE)
    m
  }

  private def unmarshaller: Unmarshaller = context.createUnmarshaller

  def write(p: Proposal, f: File): Unit = {
    marshaller.marshal(p.mutable, f)
  }

  def write(p: Proposal, w: java.io.Writer): Unit = {
    marshaller.marshal(p.mutable, w)
  }

  def writeToString(p: Proposal): String = {
    val w = new StringWriter
    write(p, w)
    w.toString
  }

  def writeToXml(p: Proposal): Node =
    XML.loadString(writeToString(p)) // there is probably a more direct route ...

  private def toProposal(r: ConversionResult): ProposalConversion = ProposalConversion(r.transformed, r.from, r.changes, toProposal(unmarshaller.unmarshal(new StringReader(r.root.toString))))

  // Not all UFT-8 characters are valid in XML, strip them if needed
  // Reference http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char
  private def stripNonValidXMLCharacters(in: String) = in.filter {
    case 0x9 | 0xA | 0xD                    => true
    case c if c >= 0x20 && c <= 0xD7FF      => true
    case c if c >= 0xE000 && c <= 0xFFFD    => true
    case c if c >= 0x10000 && c <= 0x10FFFF => true
    case _                                  => false
  }

  def readAndConvert(s: String):Validation[Either[Exception, NonEmptyList[String]], ProposalConversion] =
    try {
      UpConverter.upConvert(XML.loadString(stripNonValidXMLCharacters(s))).fold(Right(_).failure, toProposal(_).success)
    } catch {
      case ex: UnmarshalException => ex.printStackTrace();Left(ex).failure
      case ex: Exception          => ex.printStackTrace();Left(ex).failure
    }

  def readAndConvert(f:File):Validation[Either[Exception, NonEmptyList[String]], ProposalConversion] = try {
    if (f.exists()) {
      UpConverter.upConvert(XML.loadFile(f)).fold(Right(_).failure, toProposal(_).success)
    } else {
      Left(new FileNotFoundException(s"File '${f.getName}' not found")).failure
    }
  } catch {
    case ex: UnmarshalException => Left(ex).failure
    case ex: Exception          => tryLatin1AndConvert(f)
  }

  private def toProposal(a: AnyRef) = Proposal(a.asInstanceOf[M.Proposal])

  def read(f: File): Proposal   =
    try {
      toProposal(unmarshaller.unmarshal(f))
    } catch {
      case ex: UnmarshalException => tryLatin1(f)
    }

  // This is a workaround for an issue with our Perl proposal submission server.
  // Inexplicably, it sometimes writes proposals out in "latin1" encoding.
  private def tryLatin1(f: File):Proposal = {
    LOG.info("Problem opening the proposal file, trying 'latin1' encoding.")
    read(Source.fromFile(f, "latin1").mkString)
  }
  // This is a workaround for an issue with our Perl proposal submission server.
  // Inexplicably, it sometimes writes proposals out in "latin1" encoding.
  private def tryLatin1AndConvert(f: File):Validation[Either[Exception, NonEmptyList[String]], ProposalConversion]= {
    LOG.info("Problem opening the proposal file, trying 'latin1' encoding.")
    readAndConvert(Source.fromFile(f, "latin1").mkString)
  }

  def read(r: java.io.Reader): Proposal = toProposal(unmarshaller.unmarshal(r))
  def read(s: String): Proposal = read(new StringReader(s))

  lazy val schema = {
    val schemaLang = "http://www.w3.org/2001/XMLSchema"
    val factory    = SchemaFactory.newInstance(schemaLang)
    factory.newSchema(getClass.getResource("/Proposal.xsd"))
  }

  // I suppose we could return an Option[String] and just know that None is
  // success?  Seemed to express the intent better to make it an Either.
  def validate(p: Proposal): Either[String, Unit] = {
    val validator = schema.newValidator()
    try {
      val xml = writeToString(p)
      Right(validator.validate(new StreamSource(new StringReader(xml))))
    } catch {
      case ex: Exception => Left(Option(ex.getMessage).getOrElse("Validation failed."))
    }
  }
}
