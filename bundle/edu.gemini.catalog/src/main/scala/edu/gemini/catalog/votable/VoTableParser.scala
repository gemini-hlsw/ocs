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
  val UCD_PMDEC = Ucd("pos.pm;pos.eq.dec")
  val UCD_PMRA = Ucd("pos.pm;pos.eq.ra")

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

  private val magRegex = """(?i)em.(opt|IR)(\.\w)?""".r
  private val REQUIRED = List(VoTableParser.UCD_OBJID, VoTableParser.UCD_RA, VoTableParser.UCD_DEC)

  protected def parseFieldDescriptor(xml: Node): Option[FieldDescriptor] = xml match {
    case f @ <FIELD/> =>
      (for {
        id   <- f \ "@ID"
        name <- f \ "@name"
        ucd  <- f \ "@ucd"
      } yield FieldDescriptor(FieldId(id.text, Ucd(ucd.text)), name.text)).headOption
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

  def parseDoubleValue(ucd: Ucd, s: String): CatalogProblem \/ Double =
    \/.fromTryCatch(s.toDouble).leftMap(_ => FieldValueProblem(ucd, s))
  
  protected def parseBands(p: (FieldId, String)): CatalogProblem \/ (MagnitudeBand, Double) = {
    val (fieldId: FieldId, value: String) = p

    def parseBandToken(token: String):Option[String] = token match {
      case magRegex(_, null) => "UC".some
      case magRegex(_, b)    => b.replace(".", "").toUpperCase.some
      case _                 => none
    }
    
    val band = for {
      t <- fieldId.ucd.tokens
      b <- parseBandToken(t.token)
    } yield MagnitudeBand.all.find(_.name == b)

    for {
      b <- band.headOption.flatten \/> UnmatchedField(fieldId.ucd)
      v <- parseDoubleValue(fieldId.ucd, value)
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
    } yield ParsedTable(tr.map(tableRow2Target(fields)).toList)
    ParsedVoResource(tables.toList)
  }

  /**
   * Convert a table row to a sidereal target or CatalogProblem
   */
  protected def tableRow2Target(fields: List[FieldDescriptor])(row: TableRow): CatalogProblem \/ SiderealTarget = {
    val isUCAC4 = fields.exists(_.name === "ucac4")
    val ucac4BadMagnitude = 20.0
    val ucac4BadMagnitudeError = 0.98999999999999999.some
    val entries = row.itemsMap
    val entriesByUcd = entries.map(x => x._1.ucd -> x._2)

    def missing = REQUIRED.filterNot(entriesByUcd.contains)

    def containsMagnitude(v: FieldId) = v.ucd.includes(VoTableParser.UCD_MAG) && v.ucd.matches(magRegex)
    def magnitudeField(v: (FieldId, String)) = containsMagnitude(v._1) && !v._1.ucd.includes(VoTableParser.STAT_ERR)
    def magnitudeErrorField(v: (FieldId, String)) = containsMagnitude(v._1) && v._1.ucd.includes(VoTableParser.STAT_ERR)
    def nonValidMagnitude(m: Magnitude) = isUCAC4 && m.value === ucac4BadMagnitude && m.error === ucac4BadMagnitudeError

    def parseProperMotion(pm: (Option[String], Option[String])): CatalogProblem \/ Option[ProperMotion] = {
      val k = for {
        pmra <- pm._1
        pmdec <- pm._2
      } yield for {
          pmrav <- parseDoubleValue(VoTableParser.UCD_PMRA, pmra)
          pmdecv <- parseDoubleValue(VoTableParser.UCD_PMDEC, pmdec)
        } yield ProperMotion(RightAscensionAngularVelocity(AngularVelocity(pmrav)), DeclinationAngularVelocity(AngularVelocity(pmdecv)))

      k.sequenceU
    }

    def combineWithErrorsAndFilter(m: List[Magnitude], e: Map[MagnitudeBand, Double]): List[Magnitude] =
      m.map(i => i.copy(error = e.get(i.band))).filterNot(nonValidMagnitude)

    def toSiderealTarget(id: String, ra: String, dec: String, mags: Map[FieldId, String], magErrs: Map[FieldId, String], pm: (Option[String], Option[String])): \/[CatalogProblem, SiderealTarget] =
      for {
        r             <- Angle.parseDegrees(ra).leftMap(_ => FieldValueProblem(VoTableParser.UCD_RA, ra))
        d             <- Angle.parseDegrees(dec).leftMap(_ => FieldValueProblem(VoTableParser.UCD_DEC, dec))
        declination   <- Declination.fromAngle(d) \/> FieldValueProblem(VoTableParser.UCD_DEC, dec)
        magnitudeErrs <- magErrs.map(parseBands).toList.sequenceU
        magnitudes    <- mags.map(parseBands).toList.sequenceU
        properMotion  <- parseProperMotion(pm)
        coordinates    = Coordinates(RightAscension.fromAngle(r), declination)
      } yield SiderealTarget(id, coordinates, properMotion, combineWithErrorsAndFilter(magnitudes.map {case (b, v) => new Magnitude(v, b)}, magnitudeErrs.toMap).sorted, None)

    val result = for {
        id            <- entriesByUcd.get(VoTableParser.UCD_OBJID)
        ra            <- entriesByUcd.get(VoTableParser.UCD_RA)
        dec           <- entriesByUcd.get(VoTableParser.UCD_DEC)
        (pmRa, pmDec)  = (entriesByUcd.get(VoTableParser.UCD_PMRA), entriesByUcd.get(VoTableParser.UCD_PMDEC))
        mags           = entries.filter(magnitudeField)
        magErrs        = entries.filter(magnitudeErrorField)
      } yield toSiderealTarget(id, ra, dec, mags, magErrs, (pmRa, pmDec))

    result.getOrElse(\/.left(MissingValues(missing)))
  }
}
