package edu.gemini.catalog.votable

import java.io.{ByteArrayInputStream, InputStream}

import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target.SiderealTarget

import scala.io.Source
import scala.util.Try
import scala.xml.{XML, Node}

import scalaz._
import Scalaz._

case class UcdWord(token: String)
case class Ucd(tokens: List[UcdWord]) {
  def includes(ucd: UcdWord): Boolean = tokens.find(_ == ucd).nonEmpty
}

object Ucd {
  def parseUcd(v: String): Ucd = Ucd(v.split(";").filter(_.nonEmpty).map(_.toLowerCase).map(UcdWord).toList)

  def apply(ucd: String):Ucd = parseUcd(ucd)
}

/** Describes a field */
case class FieldDescriptor(id: String, name: String, ucd: Ucd)

case class TableRowItem(field: FieldDescriptor, data: String)
case class TableRow(items: List[TableRowItem]) {
  def itemsMap = (for {
    i <- items
  } yield i.field -> i.data).toMap
}

/** ParsedTable and ParsedResources contains a list of problems */
case class ParsedTable(rows: List[CatalogProblem \/ SiderealTarget]) {
  def containsError: Boolean = rows.exists(_.isLeft)
}
case class ParsedResource(tables: List[ParsedTable]) {
  def containsError: Boolean = tables.exists(_.containsError)
}

/** The result of parsing a Catalog Query is a list of targets */
case class TargetsTable(rows: List[SiderealTarget])

object TargetsTable {
  def apply(t: ParsedTable): TargetsTable = TargetsTable(t.rows.filter(_.isRight).map(_.toOption).flatten)
}

case class Resource(tables: List[TargetsTable])

object Resource {
  def apply(r: ParsedResource):Resource = Resource(r.tables.map(TargetsTable.apply))
}

/** Indicates an issue parsing the targets, e.g. missing values, bad format, etc. */
sealed trait CatalogProblem

case class ValidationError(url: String) extends CatalogProblem
case class MissingValues(fields: List[FieldDescriptor]) extends CatalogProblem
case class FieldValueProblem(field: FieldDescriptor, value: String) extends CatalogProblem
case class UnmatchedField(field: FieldDescriptor) extends CatalogProblem

object VoTableParser extends VoTableParser {

  val UCD_OBJID = Ucd("meta.id;meta.main")
  val UCD_RA = Ucd("pos.eq.ra;meta.main")
  val UCD_DEC = Ucd("pos.eq.dec;meta.main")

  val OBJID = FieldDescriptor("objid", "objid", UCD_OBJID)
  val RA = FieldDescriptor("raj2000", "raj2000", UCD_RA)
  val DEC = FieldDescriptor("dej2000", "dej2000", UCD_DEC)

  val UCD_MAG = UcdWord("phot.mag")
  val STAT_ERR = UcdWord("stat.error")

  val xsd = "/votable-1.2.xsd"
  
  private def validate(xmlFile: InputStream):Try[String] = Try {
    import javax.xml.transform.stream.StreamSource
    import javax.xml.validation.SchemaFactory

    val schemaLang = "http://www.w3.org/2001/XMLSchema"
    val factory = SchemaFactory.newInstance(schemaLang)
    val schema = factory.newSchema(new StreamSource(getClass.getResourceAsStream(xsd)))
    val validator = schema.newValidator()

    // Load in memory (Could be a problem for large responses)
    val xmlText = Source.fromInputStream(xmlFile, "UTF-8").getLines().mkString

    validator.validate(new StreamSource(new ByteArrayInputStream(xmlText.getBytes(java.nio.charset.Charset.forName("UTF-8")))))
    xmlText
  }

  /**
   * parse takes an input stream and attempts to read the xml content and convert it to a VoTable resource
   */
  def parse(url: String, is: InputStream): CatalogProblem \/ ParsedResource = {
    validate(is) match {
      case scala.util.Success(s) => \/-(parse(XML.loadString(s)))
      case scala.util.Failure(_) => -\/(ValidationError(url))
    }
  }
}

trait VoTableParser {

  import scala.xml.Node

  val REQUIRED = List(VoTableParser.OBJID, VoTableParser.RA, VoTableParser.DEC)

  protected def parseFieldDescriptor(xml: Node): Option[FieldDescriptor] = xml match {
    case f @ <FIELD/> =>
      (for {
        id   <- f \ "@ID"
        name <- f \ "@name"
        ucd  <- f \ "@ucd"
      } yield FieldDescriptor(id.text, name.text, Ucd(ucd.text))).headOption
    case _            => None
  }

  protected def parseFields(xml: Node): List[FieldDescriptor] = (for {
      f <- xml \\ "FIELD"
    } yield parseFieldDescriptor(f)).flatten.toList

  protected def parseTableRow(fields: List[FieldDescriptor], xml: Node): TableRow = {
    val rows = for {
      tr <-  xml \\ "TR"
      td =   tr  \  "TD"
      if td.length == fields.length
    } yield for {
        f <- fields.zip(td)
      } yield TableRowItem(f._1, f._2.text)
    TableRow(rows.flatten.toList)
  }

  protected def parseTableRows(fields: List[FieldDescriptor], xml: Node)  =
    for {
      table <-  xml   \\ "TABLEDATA"
      tr    <-  table \\ "TR"
    } yield parseTableRow(fields, tr)

  protected def parseMagnitude(p: (FieldDescriptor, String)): CatalogProblem \/ Magnitude = {
    val (field: FieldDescriptor, value: String) = p

    def parseValue(f: FieldDescriptor, s: String): CatalogProblem \/ Double =
      Try(s.toDouble) match {
        case scala.util.Success(d) => \/-(d)
        case scala.util.Failure(e) => -\/(FieldValueProblem(f, s))
      }

    val magRegex = """em.opt.(\w)""".r

    val band = for {
      t <- field.ucd.tokens
      m <- magRegex.findFirstMatchIn(t.token)
      l  = m.group(1).toUpperCase
    } yield MagnitudeBand.all.find(_.name == l)

    for {
      b <- band.headOption.flatten \/> UnmatchedField(field)
      v <- parseValue(field, value)
    } yield new Magnitude(v, b)
  }
  
  /**
   * Takes an XML Node and attempts to extract the resources and targets from a VOBTable
   */
  protected def parse(xml: Node): ParsedResource = {
    val tables = for {
      table <- xml \\ "TABLE"
      fields = parseFields(table)
      tr = parseTableRows(fields, table)
    } yield ParsedTable(tr.map(tableRow2Target).toList)
    ParsedResource(tables.toList)
  }

  /**
   * Convert a table row to a sidereal target or CatalogProblem
   */
  protected def tableRow2Target(row: TableRow): CatalogProblem \/ SiderealTarget = {
    val entries = row.itemsMap

    def missing = REQUIRED.filterNot(entries.contains)

    def magnitudeField(v: (FieldDescriptor, String)) = v._1.ucd.includes(VoTableParser.UCD_MAG) && !v._1.ucd.includes(VoTableParser.STAT_ERR)

    val result = for {
      id  <- entries.get(VoTableParser.OBJID)
      ra  <- entries.get(VoTableParser.RA)
      dec <- entries.get(VoTableParser.DEC)
      mag =  entries.filter(magnitudeField).map(parseMagnitude).toList.partition(p => p.isRight)
    } yield for {
        r           <- Angle.parseDegrees(ra).leftMap(_ => FieldValueProblem(VoTableParser.RA, ra))
        d           <- Angle.parseDegrees(dec).leftMap(_ => FieldValueProblem(VoTableParser.DEC, dec))
        declination <- Declination.fromAngle(d) \/> FieldValueProblem(VoTableParser.DEC, dec)
        coordinates  = Coordinates(RightAscension.fromAngle(r), declination)
        _           <- mag._2.headOption.getOrElse(\/-(true)) // Will stop the loop if an error is present
        magnitudes   = mag._1.map(_.toOption).flatten
      } yield SiderealTarget(id, coordinates, Equinox.J2000, None, magnitudes.sorted, None)

    result.getOrElse(-\/(MissingValues(missing)))
  }
}
