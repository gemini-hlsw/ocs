package edu.gemini.catalog.votable

import java.io.{ByteArrayInputStream, InputStream}

import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target.SiderealTarget

import scala.io.Source
import scala.xml.XML

import scalaz._
import Scalaz._

object VoTableParser extends VoTableParser {
  type CatalogResult = CatalogProblem \/ ParsedVoResource

  val UCD_OBJID = Ucd("meta.id;meta.main")
  val UCD_RA = Ucd("pos.eq.ra;meta.main")
  val UCD_DEC = Ucd("pos.eq.dec;meta.main")

  val OBJID = FieldDescriptor("objid", "objid", UCD_OBJID)
  val RA = FieldDescriptor("raj2000", "raj2000", UCD_RA)
  val DEC = FieldDescriptor("dej2000", "dej2000", UCD_DEC)

  val UCD_MAG = UcdWord("phot.mag")
  val STAT_ERR = UcdWord("stat.error")

  val xsd = "/votable-1.2.xsd"
  
  private def validate(xmlFile: InputStream): Throwable \/ String = \/.fromTryCatch {
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
  def parse(url: String, is: InputStream): CatalogResult =
    validate(is).fold(k => \/.left(ValidationError(url)), r => \/.right(parse(XML.loadString(r))))
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

  def parseDoubleValue(f: FieldDescriptor, s: String): CatalogProblem \/ Double =
    \/.fromTryCatch(s.toDouble).leftMap(_ => FieldValueProblem(f, s))
  
  protected def parseBands(p: (FieldDescriptor, String)): CatalogProblem \/ (MagnitudeBand, Double) = {
    val (field: FieldDescriptor, value: String) = p

    val magRegex = """em.opt.(\w)""".r

    val band = for {
      t <- field.ucd.tokens
      m <- magRegex.findFirstMatchIn(t.token)
      l  = m.group(1).toUpperCase
    } yield MagnitudeBand.all.find(_.name == l)

    for {
      b <- band.headOption.flatten \/> UnmatchedField(field)
      v <- parseDoubleValue(field, value)
    } yield (b, v)
  }

  /**
   * Takes an XML Node and attempts to extract the resources and targets from a VOBTable
   */
  protected def parse(xml: Node): ParsedVoResource = {
    val tables = for {
      table <- xml \\ "TABLE"
      fields = parseFields(table)
      tr = parseTableRows(fields, table)
    } yield ParsedTable(tr.map(tableRow2Target).toList)
    ParsedVoResource(tables.toList)
  }

  /**
   * Convert a table row to a sidereal target or CatalogProblem
   */
  protected def tableRow2Target(row: TableRow): CatalogProblem \/ SiderealTarget = {
    val entries = row.itemsMap

    def missing = REQUIRED.filterNot(entries.contains)

    def magnitudeField(v: (FieldDescriptor, String)) = v._1.ucd.includes(VoTableParser.UCD_MAG) && !v._1.ucd.includes(VoTableParser.STAT_ERR)
    def magnitudeErrorField(v: (FieldDescriptor, String)) = v._1.ucd.includes(VoTableParser.UCD_MAG) && v._1.ucd.includes(VoTableParser.STAT_ERR)

    def combineWithErrors(m: List[Magnitude], e: Map[MagnitudeBand, Double]) = m.map(i => i.copy(error = e.get(i.band)))

    def toSiderealTarget(id: String, ra: String, dec: String, mags: Map[FieldDescriptor, String], magErrs: Map[FieldDescriptor, String]): \/[CatalogProblem, SiderealTarget] =
      for {
        r             <- Angle.parseDegrees(ra).leftMap(_ => FieldValueProblem(VoTableParser.RA, ra))
        d             <- Angle.parseDegrees(dec).leftMap(_ => FieldValueProblem(VoTableParser.DEC, dec))
        declination   <- Declination.fromAngle(d) \/> FieldValueProblem(VoTableParser.DEC, dec)
        magnitudeErrs <- magErrs.map(parseBands).toList.sequenceU
        magnitudes    <- mags.map(parseBands).toList.sequenceU
        coordinates    = Coordinates(RightAscension.fromAngle(r), declination)
      } yield SiderealTarget(id, coordinates, Equinox.J2000, None, combineWithErrors(magnitudes.map {case (b, v) => new Magnitude(v, b)}, magnitudeErrs.toMap).sorted, None)

    val result = for {
        id   <- entries.get(VoTableParser.OBJID)
        ra   <- entries.get(VoTableParser.RA)
        dec  <- entries.get(VoTableParser.DEC)
        mags  = entries.filter(magnitudeField)
        magErrs  = entries.filter(magnitudeErrorField)
      } yield toSiderealTarget(id, ra, dec, mags, magErrs)

    result.getOrElse(\/.left(MissingValues(missing)))
  }
}
