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
case class Ucd(tokens: List[UcdWord])

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
case class FormattingProblem(field: FieldDescriptor, value: String) extends CatalogProblem

object VoTableParser extends VoTableParser {

  val UCD_OBJID = Ucd("meta.id;meta.main")
  val UCD_RA = Ucd("pos.eq.ra;meta.main")
  val UCD_DEC = Ucd("pos.eq.dec;meta.main")

  val OBJID = FieldDescriptor("objid", "objid", UCD_OBJID)
  val RA = FieldDescriptor("raj2000", "raj2000", UCD_RA)
  val DEC = FieldDescriptor("dej2000", "dej2000", UCD_DEC)

  val xsd = "/votable-1.2.xsd"

  /**
   * Takes an XML Node and attempt to extract the resources and targets from a VOBTable
   */
  def parse(xml: Node): ParsedResource = {
    val tables = for {
      table <- xml \\ "TABLE"
      fields = parseFields(table)
      tr = parseTableRows(fields, table)
    } yield ParsedTable(tr.map(tableRow2Target).toList)
    ParsedResource(tables.toList)
  }

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

  def parse(url: String, is: InputStream): CatalogProblem \/ ParsedResource = {
    validate(is) match {
      case scala.util.Success(s) => \/-(parse(XML.loadString(s)))
      case scala.util.Failure(e) => -\/(ValidationError(url))
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

  /**
   * Convert a table row to a sidereal target or a CatalogpProblem
   */
  protected def tableRow2Target(row: TableRow): CatalogProblem \/ SiderealTarget = {
    val entries = row.itemsMap

    def missing = REQUIRED.filterNot(entries.contains)

    val result = for {
      id  <- entries.get(VoTableParser.OBJID)
      ra  <- entries.get(VoTableParser.RA)
      dec <- entries.get(VoTableParser.DEC)
    } yield for {
        r           <- Angle.parseDegrees(ra).leftMap(_ => FormattingProblem(VoTableParser.RA, ra))
        d           <- Angle.parseDegrees(dec).leftMap(_ => FormattingProblem(VoTableParser.DEC, dec))
        declination <- Declination.fromAngle(d) \/> FormattingProblem(VoTableParser.DEC, dec)
        coordinates  = Coordinates(RightAscension.fromAngle(r), declination)
      } yield SiderealTarget(id, coordinates, Equinox.J2000, None, Nil, None)

    result.getOrElse(-\/(MissingValues(missing)))
  }
}
