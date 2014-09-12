package edu.gemini.model.p1.schema

import edu.gemini.model.p1.mutable.{ ObjectFactory, Proposal }
import javax.xml.transform.Source
import javax.xml.transform.stream.{ StreamSource, StreamResult }
import javax.xml.validation.SchemaFactory
import org.xml.sax.SAXException
import javax.xml.bind.JAXBContext
import java.io.{ StringReader, StringWriter }

object Validator {

  private val schemaLang = "http://www.w3.org/2001/XMLSchema"

  private lazy val Factory = new ObjectFactory
  private lazy val ContextPackage = Factory.getClass.getPackage.getName
  private lazy val Context = JAXBContext.newInstance(ContextPackage, getClass.getClassLoader)
  private lazy val Marshaller = Context.createMarshaller

  def main(args: Array[String]) {
    require(args.size >= 2, "Params: xmlFile, xsdFile")
    try {
      validate(args(0), args(1))
      println("ok")
    } catch {
      case ex: SAXException => println(ex.getMessage)
      case ex: Exception    => ex.printStackTrace()
    }
  }

  def validate(xmlFile: String, xsdFile: String) {
    val xml = new StreamSource(xmlFile)
    val xsd = new StreamSource(xsdFile)
    validate(xml, xsd)
  }

  def validate(xml: Source, xsd: Source) {
    val factory = SchemaFactory.newInstance(schemaLang)
    val schema = factory.newSchema(xsd)
    val validator = schema.newValidator()
    validator.validate(xml)
  }

  def validate(p: Proposal) {
    val sw = new StringWriter
    Marshaller.marshal(p, sw)
    val xml = new StreamSource(new StringReader(sw.toString))
    val xsd = new StreamSource(getClass.getResourceAsStream("/Proposal.xsd"))
    validate(xml, xsd)
  }

}