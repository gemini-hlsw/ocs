package edu.gemini.catalog.votable

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URL

import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target.SiderealTarget

import scala.io.Source
import scala.xml.XML

import scalaz._
import Scalaz._

object VoTableParser extends VoTableParser {
  type CatalogResult = CatalogProblem \/ ParsedVoResource

  // by band
  private val MagnitudeOrdering: scala.math.Ordering[Magnitude] =
    scala.math.Ordering.by(_.band)

  val UCD_OBJID = Ucd("meta.id;meta.main")
  val UCD_RA = Ucd("pos.eq.ra;meta.main")
  val UCD_DEC = Ucd("pos.eq.dec;meta.main")
  val UCD_PMDEC = Ucd("pos.pm;pos.eq.dec")
  val UCD_PMRA = Ucd("pos.pm;pos.eq.ra")

  val UCD_MAG = UcdWord("phot.mag")
  val STAT_ERR = UcdWord("stat.error")

  val xsd = "/votable-1.2.xsd"
  
  private def validate(xmlText: String): Throwable \/ String = \/.fromTryCatch {
    import javax.xml.transform.stream.StreamSource
    import javax.xml.validation.SchemaFactory

    val schemaLang = "http://www.w3.org/2001/XMLSchema"
    val factory = SchemaFactory.newInstance(schemaLang)
    val schema = factory.newSchema(new StreamSource(getClass.getResourceAsStream(xsd)))
    val validator = schema.newValidator()

    validator.validate(new StreamSource(new ByteArrayInputStream(xmlText.getBytes(java.nio.charset.Charset.forName("UTF-8")))))
    xmlText
  }

  /**
   * parse takes an input stream and attempts to read the xml content and convert it to a VoTable resource
   */
  def parse(url: URL, is: InputStream, checkValidity: Boolean = true): CatalogResult = {
    // Load in memory (Could be a problem for large responses)
    val xmlText = Source.fromInputStream(is, "UTF-8").getLines().mkString

    (checkValidity, validate(xmlText)) match {
      case (true, -\/(e))  => \/.left(ValidationError(url))
      case (false, -\/(e)) => \/.right(parse(XML.loadString(xmlText)))
      case (_, \/-(r))     => \/.right(parse(XML.loadString(r)))
    }
  }
}

// A CatalogAdapter improves parsing handling catalog-specific options like parsing magnitudes and selecting key fields
sealed trait CatalogAdapter {
  // Required fields
  val idField: FieldId
  val raField: FieldId
  val decField: FieldId
  val pmRaField = FieldId("pmra", VoTableParser.UCD_PMRA)
  val pmDecField = FieldId("pmde", VoTableParser.UCD_PMDEC)

  // Indicates if the field is a magnitude
  def isMagnitudeField(v: (FieldId, String)): Boolean = containsMagnitude(v._1) && !v._1.ucd.includes(VoTableParser.STAT_ERR) && v._2.nonEmpty
  // Indicates if the field is a magnitude error
  def isMagnitudeErrorField(v: (FieldId, String)): Boolean = containsMagnitude(v._1) && v._1.ucd.includes(VoTableParser.STAT_ERR) && v._2.nonEmpty
  // filter magnitudes as a whole, removing invalid values and duplicates
  def filterAndDeduplicateMagnitudes(magnitudeFields: List[(FieldId, Magnitude)]): List[Magnitude] = magnitudeFields.collect { case (_, mag) if validMagnitude(mag) => mag }
  // Indicates if a parsed magnitude is valid
  def validMagnitude(m: Magnitude): Boolean = !(m.value.isNaN || m.error.exists(_.isNaN))
  // Attempts to extract a band and value for a magnitude from a pair of field and value
  def parseMagnitude(p: (FieldId, String)): CatalogProblem \/ (FieldId, MagnitudeBand, Double) = {
    val (fieldId: FieldId, value: String) = p

    val band = fieldToBand(fieldId)

    for {
      b <- band \/> UnmatchedField(fieldId.ucd)
      v <- CatalogAdapter.parseDoubleValue(fieldId.ucd, value)
    } yield (fieldId, b, v)
  }

  // From a Field extract the band from either the field id or the UCD
  protected def fieldToBand(field: FieldId): Option[MagnitudeBand]

  // Indicates if a field contianing a magnitude should be ignored, by default all fields are considered
  protected def ignoreMagnitudeField(v: FieldId): Boolean = false
  // Indicates if the field has a magnitude field
  protected def containsMagnitude(v: FieldId): Boolean = v.ucd.includes(VoTableParser.UCD_MAG) && v.ucd.matches(CatalogAdapter.magRegex) && !ignoreMagnitudeField(v)
}

object CatalogAdapter {
  val magRegex = """(?i)em.(opt|IR)(\.\w)?""".r

  def parseDoubleValue(ucd: Ucd, s: String): CatalogProblem \/ Double =
    \/.fromTryCatch(s.toDouble).leftMap(_ => FieldValueProblem(ucd, s))

}

// Common methods for UCAC4 and PPMXL
trait StandardAdapter {
  // Find what band the field descriptor should represent, in general prefer "upper case" bands over "lower case" Sloan bands.
  // This will prefer U, R and I over u', r' and i' but will map "g" and "z" to the Sloan bands g' and z'.
  def parseBand(fieldId: FieldId, band: String): Option[MagnitudeBand] =
    MagnitudeBand.all.
      find(_.name == band.toUpperCase).
      orElse(MagnitudeBand.all.find(_.name == band))

  // From a Field extract the band from either the field id or the UCD
  protected def fieldToBand(field: FieldId): Option[MagnitudeBand] = {
    // Parses a UCD token to extract the band for catalogs that include the band in the UCD (UCAC4/PPMXL)
    def parseBandToken(token: String):Option[String] = token match {
      case CatalogAdapter.magRegex(_, null) => "UC".some
      case CatalogAdapter.magRegex(_, b)    => b.replace(".", "").some
      case _                                => none
    }

    (for {
      t <- field.ucd.tokens
      b <- parseBandToken(t.token)
    } yield parseBand(field, b)).headOption.flatten
  }
}

case object UCAC4Adapter extends CatalogAdapter with StandardAdapter {
  val idField = FieldId("ucac4", VoTableParser.UCD_OBJID)
  val raField = FieldId("raj2000", VoTableParser.UCD_RA)
  val decField = FieldId("dej2000", VoTableParser.UCD_DEC)

  private val ucac4BadMagnitude = 20.0
  private val ucac4BadMagnitudeError = 0.9.some

  // UCAC4 ignores A-mags
  override def ignoreMagnitudeField(v: FieldId) = v.id === "amag" || v.id === "e_amag"
  // Magnitudes with value 20 or error over or equal to 0.9 are invalid in UCAC4
  override def validMagnitude(m: Magnitude) = super.validMagnitude(m) && m.value =/= ucac4BadMagnitude && m.error.map(math.abs) <= ucac4BadMagnitudeError
  // UCAC4 has a few special cases to map magnitudes, g, r and i refer to the Sloan bands g', r' and i'
  override def parseBand(id: FieldId, band: String): Option[MagnitudeBand] = (id.id, id.ucd) match {
    case ("gmag" | "e_gmag", ucd) if ucd.includes(UcdWord("em.opt.r")) => Some(MagnitudeBand._g)
    case ("rmag" | "e_rmag", ucd) if ucd.includes(UcdWord("em.opt.r")) => Some(MagnitudeBand._r)
    case ("imag" | "e_imag", ucd) if ucd.includes(UcdWord("em.opt.i")) => Some(MagnitudeBand._i)
    case _                                                             => super.parseBand(id, band)
  }
}

case object PPMXLAdapter extends CatalogAdapter with StandardAdapter {
  val idField = FieldId("ppmxl", VoTableParser.UCD_OBJID)
  val raField = FieldId("raj2000", VoTableParser.UCD_RA)
  val decField = FieldId("decj2000", VoTableParser.UCD_DEC)

  // PPMXL may contain two representations for bands R and B, represented with ids r1mag/r2mag or b1mag/b2mac
  // The ids r1mag/r2mag are preferred but if they are absent we should use the alternative values
  val primaryMagnitudesIds = List("r1mag", "b1mag")
  val alternateMagnitudesIds = List("r2mag", "b2mag")
  val idsMapping = primaryMagnitudesIds.zip(alternateMagnitudesIds)

  override def filterAndDeduplicateMagnitudes(magnitudeFields: List[(FieldId, Magnitude)]): List[Magnitude] = {
    // Read all magnitudes, including duplicates
    val magMap1 = (Map.empty[String, Magnitude]/:magnitudeFields) {
      case (m, (FieldId(i, _), mag)) if validMagnitude(mag) => m + (i -> mag)
      case (m, _)                                           => m
    }
    // Now magMap1 might have double entries for R and B.  Get rid of the alternative if so.
    val magMap2 = (magMap1/:idsMapping) { case (m, (id1, id2)) =>
      if (magMap1.contains(id1)) m - id2 else m
    }
    magMap2.values.toList
  }
}

case object SimbadAdapter extends CatalogAdapter {
  private val errorFluxID = "FLUX_ERROR_(.)".r
  private val fluxID = "FLUX_(.)".r
  val idField = FieldId("MAIN_ID", VoTableParser.UCD_OBJID)
  val raField = FieldId("RA_d", VoTableParser.UCD_RA)
  val decField = FieldId("DEC_d", VoTableParser.UCD_DEC)
  override val pmRaField = FieldId("PMRA", VoTableParser.UCD_PMRA)
  override val pmDecField = FieldId("PMDEC", VoTableParser.UCD_PMDEC)

  override def ignoreMagnitudeField(v: FieldId) = !v.id.toLowerCase.startsWith("flux")

  // Simbad has a few special cases to map sloan magnitudes
  def findBand(id: FieldId): Option[MagnitudeBand] = (id.id, id.ucd) match {
    case ("FLUX_z" | "e_gmag", ucd) if ucd.includes(UcdWord("em.opt.i")) => Some(MagnitudeBand._z) // Special case
    case ("FLUX_g" | "e_rmag", ucd) if ucd.includes(UcdWord("em.opt.b")) => Some(MagnitudeBand._g) // Special case
    case (errorFluxID(b), _)                                             => findBand(b)
    case (fluxID(b), _)                                                  => findBand(b)
    case _                                                               => None
  }

  // Simbad doesn't put the band in the ucd for magnitude errors
  override def isMagnitudeErrorField(v: (FieldId, String)): Boolean =
    v._1.ucd.includes(VoTableParser.UCD_MAG) && v._1.ucd.includes(VoTableParser.STAT_ERR) && errorFluxID.findFirstIn(v._1.id).isDefined && !ignoreMagnitudeField(v._1) && v._2.nonEmpty

  protected def findBand(band: String): Option[MagnitudeBand] =
    MagnitudeBand.all.
      find(_.name == band)

  override def fieldToBand(field: FieldId): Option[MagnitudeBand] = {
    ((field.ucd.includes(VoTableParser.UCD_MAG) && !ignoreMagnitudeField(field)) option {
      findBand(field)
    }).flatten
  }
}

trait VoTableParser {

  import scala.xml.Node

  protected def parseFieldDescriptor(xml: Node): Option[FieldDescriptor] = xml match {
    case f @ <FIELD>{_*}</FIELD> =>
      (for {
        id   <- f \ "@ID"
        name <- f \ "@name"
        ucd  <- f \ "@ucd"
      } yield FieldDescriptor(FieldId(id.text, Ucd(ucd.text)), name.text)).headOption
    case _                       => None
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
   * Takes an XML Node and attempts to extract the resources and targets from a VOBTable
   */
  protected def parse(xml: Node): ParsedVoResource = {
    val tables = for {
      table <- xml \\ "TABLE"
      tableId = table \ "@ID"
      fields  = parseFields(table)
      tr = parseTableRows(fields, table)
    } yield ParsedTable(tr.map(tableRow2Target(tableId.headOption.map(_.text), fields)).toList)
    ParsedVoResource(tables.toList)
  }

  /**
   * Convert a table row to a sidereal target or CatalogProblem
   */
  protected def tableRow2Target(tableId: Option[String], fields: List[FieldDescriptor])(row: TableRow): CatalogProblem \/ SiderealTarget = {
    val entries = row.itemsMap

    val catalogAdapter: CatalogProblem \/ CatalogAdapter = {
      val isUCAC4  = fields.exists(_.name === "ucac4")
      val isPPMXL  = fields.exists(_.name === "ppmxl")
      val isSimbad = tableId.exists(_.equalsIgnoreCase("simbad"))
      if (isUCAC4) UCAC4Adapter.right else if (isPPMXL) PPMXLAdapter.right else if (isSimbad) SimbadAdapter.right else UnknownCatalog.left
    }

    def parseProperMotion(pm: (Option[String], Option[String])): CatalogProblem \/ Option[ProperMotion] = {
      val k = for {
        pmra  <- pm._1.filter(_.nonEmpty)
        pmdec <- pm._2.filter(_.nonEmpty)
      } yield for {
          pmrav  <- CatalogAdapter.parseDoubleValue(VoTableParser.UCD_PMRA, pmra)
          pmdecv <- CatalogAdapter.parseDoubleValue(VoTableParser.UCD_PMDEC, pmdec)
        } yield ProperMotion(RightAscensionAngularVelocity(AngularVelocity(pmrav)), DeclinationAngularVelocity(AngularVelocity(pmdecv)))

      k.sequenceU
    }

    def combineWithErrorsAndFilter(m: List[(FieldId, MagnitudeBand, Double)], e: List[(FieldId, MagnitudeBand, Double)], adapter: CatalogAdapter): List[Magnitude] = {
      val mags = m.map {
          case (f, b, d) => f -> new Magnitude(d, b, b.defaultSystem)
        }
      val magErrors = e.map {
          case (_, b, d) => b -> d
        }.toMap
      // Filter magnitudes as a whole
      val magnitudes = adapter.filterAndDeduplicateMagnitudes(mags)
      // Link magnitudes with their errors
      magnitudes.map(i => i.copy(error = magErrors.get(i.band))).filter(adapter.validMagnitude)
    }

    def toSiderealTarget(id: String, ra: String, dec: String, pm: (Option[String], Option[String])): \/[CatalogProblem, SiderealTarget] = {
      for {
        adapter        <- catalogAdapter
        r              <- Angle.parseDegrees(ra).leftMap(_ => FieldValueProblem(VoTableParser.UCD_RA, ra))
        d              <- Angle.parseDegrees(dec).leftMap(_ => FieldValueProblem(VoTableParser.UCD_DEC, dec))
        declination    <- Declination.fromAngle(d) \/> FieldValueProblem(VoTableParser.UCD_DEC, dec)
        properMotion   <- parseProperMotion(pm)
        mags            = entries.filter(adapter.isMagnitudeField)
        magErrs         = entries.filter(adapter.isMagnitudeErrorField)
        magnitudes     <- mags.map(adapter.parseMagnitude).toList.sequenceU
        magnitudeErrs  <- magErrs.map(adapter.parseMagnitude).toList.sequenceU
      } yield {
        val coordinates = Coordinates(RightAscension.fromAngle(r), declination)
        val combMags    = combineWithErrorsAndFilter(magnitudes, magnitudeErrs, adapter).sorted(VoTableParser.MagnitudeOrdering)
        SiderealTarget(id, coordinates, properMotion, combMags)
      }
    }

    val result = for {
      adapter <- catalogAdapter
      id      <- entries.get(adapter.idField) \/> MissingValue(adapter.idField)
      ra      <- entries.get(adapter.raField) \/> MissingValue(adapter.raField)
      dec     <- entries.get(adapter.decField) \/> MissingValue(adapter.decField)
      (pmRa, pmDec)   = (entries.get(adapter.pmRaField), entries.get(adapter.pmDecField))
    } yield toSiderealTarget(id, ra, dec, (pmRa, pmDec))

    result.join
  }
}
