package edu.gemini.catalog.votable

import edu.gemini.spModel.core._
import edu.gemini.spModel.core.Target.SiderealTarget
import org.specs2.mutable.SpecificationWithJUnit

import scalaz._
import Scalaz._

class VoTableParserSpec extends SpecificationWithJUnit with VoTableParser {

  // TODO: remove this, use method on Angle companion
  def fromMilliarcseconds(d: Double): Angle =
    Angle.fromDegrees(d / (60 * 60 * 1000))

  "Ucd" should {
    "detect if is a superset" in {
      Ucd("stat.error;phot.mag;em.opt.i").includes(UcdWord("phot.mag")) should beTrue
      Ucd("stat.error;phot.mag;em.opt.i").matches("phot.mag".r) should beTrue
      Ucd("stat.error;phot.mag;em.opt.i").matches("em.opt.(\\w)".r) should beTrue
    }
  }

  "The VoTable Parser" should {
    val fieldsNode =
      <TABLE>
        <FIELD ID="gmag_err" datatype="double" name="gmag_err" ucd="stat.error;phot.mag;em.opt.g"/>
        <FIELD ID="rmag_err" datatype="double" name="rmag_err" ucd="stat.error;phot.mag;em.opt.r"/>
        <FIELD ID="flags1" datatype="int" name="flags1" ucd="meta.code"/>
      </TABLE>

    val tableRow =
      <TR>
        <TD>0.0960165</TD>
        <TD>0.0503736</TD>
        <TD>268435728</TD>
      </TR>

    val dataNode =
      <DATA>
        <TABLEDATA>
          <TR>
            <TD>0.0960165</TD>
            <TD>0.0503736</TD>
            <TD>268435728</TD>
          </TR>
          <TR>
            <TD>0.51784</TD>
            <TD>0.252201</TD>
            <TD>536871168</TD>
          </TR>
        </TABLEDATA>
      </DATA>

    val skyObjects =
      <TABLE>
        <FIELD ID="flags1" datatype="int" name="flags1" ucd="meta.code"/>
        <FIELD ID="umag" datatype="double" name="umag" ucd="phot.mag;em.opt.u"/>
        <FIELD ID="flags2" datatype="int" name="flags2" ucd="meta.code"/>
        <FIELD ID="imag" datatype="double" name="imag" ucd="phot.mag;em.opt.i"/>
        <FIELD ID="dej2000" datatype="double" name="dej2000" ucd="pos.eq.dec;meta.main"/>
        <FIELD ID="raj2000" datatype="double" name="raj2000" ucd="pos.eq.ra;meta.main"/>
        <FIELD ID="rmag" datatype="double" name="rmag" ucd="phot.mag;em.opt.r"/>
        <FIELD ID="objid" datatype="int" name="objid" ucd="meta.id;meta.main"/>
        <FIELD ID="gmag" datatype="double" name="gmag" ucd="phot.mag;em.opt.g"/>
        <FIELD ID="zmag" datatype="double" name="zmag" ucd="phot.mag;em.opt.z"/>
        <FIELD ID="type" datatype="int" name="type" ucd="meta.code"/>
        <DATA>
          <TABLEDATA>
            <TR>
              <TD>268435728</TD>
              <TD>23.0888</TD>
              <TD>8208</TD>
              <TD>20.3051</TD>
              <TD>0.209323681906</TD>
              <TD>359.745951955</TD>
              <TD>20.88</TD>
              <TD>-2140405448</TD>
              <TD>22.082</TD>
              <TD>19.8812</TD>
              <TD>3</TD>
            </TR>
            <TR>
              <TD>536871168</TD>
              <TD>23.0853</TD>
              <TD>65552</TD>
              <TD>20.7891</TD>
              <TD>0.210251239819</TD>
              <TD>359.749274134</TD>
              <TD>21.7686</TD>
              <TD>-2140404569</TD>
              <TD>23.0889</TD>
              <TD>20.0088</TD>
              <TD>3</TD>
            </TR>
          </TABLEDATA>
      </DATA>
    </TABLE>

    val skyObjectsWithErrors =
      <TABLE>
        <FIELD ID="gmag_err" datatype="double" name="gmag_err" ucd="stat.error;phot.mag;em.opt.g"/>
        <FIELD ID="rmag_err" datatype="double" name="rmag_err" ucd="stat.error;phot.mag;em.opt.r"/>
        <FIELD ID="flags1" datatype="int" name="flags1" ucd="meta.code"/>
        <FIELD ID="umag" datatype="double" name="umag" ucd="phot.mag;em.opt.u"/>
        <FIELD ID="flags2" datatype="int" name="flags2" ucd="meta.code"/>
        <FIELD ID="imag" datatype="double" name="imag" ucd="phot.mag;em.opt.i"/>
        <FIELD ID="zmag_err" datatype="double" name="zmag_err" ucd="stat.error;phot.mag;em.opt.z"/>
        <FIELD ID="dej2000" datatype="double" name="dej2000" ucd="pos.eq.dec;meta.main"/>
        <FIELD ID="umag_err" datatype="double" name="umag_err" ucd="stat.error;phot.mag;em.opt.u"/>
        <FIELD ID="imag_err" datatype="double" name="imag_err" ucd="stat.error;phot.mag;em.opt.i"/>
        <FIELD ID="raj2000" datatype="double" name="raj2000" ucd="pos.eq.ra;meta.main"/>
        <FIELD ID="rmag" datatype="double" name="rmag" ucd="phot.mag;em.opt.r"/>
        <FIELD ID="objid" datatype="int" name="objid" ucd="meta.id;meta.main"/>
        <FIELD ID="gmag" datatype="double" name="gmag" ucd="phot.mag;em.opt.g"/>
        <FIELD ID="zmag" datatype="double" name="zmag" ucd="phot.mag;em.opt.z"/>
        <FIELD ID="type" datatype="int" name="type" ucd="meta.code"/>
        <FIELD ID="jmag" datatype="double" name="jmag" ucd="phot.mag;em.IR.J"/>
        <FIELD ID="e_jmag" datatype="double" name="e_jmag" ucd="stat.error;phot.mag;em.IR.J"/>
        <DATA>
          <TABLEDATA>
            <TR>
              <TD>0.0960165</TD>
              <TD>0.0503736</TD>
              <TD>268435728</TD>
              <TD>23.0888</TD>
              <TD>8208</TD>
              <TD>20.3051</TD>
              <TD>0.138202</TD>
              <TD>0.209323681906</TD>
              <TD>0.518214</TD>
              <TD>0.0456069</TD>
              <TD>359.745951955</TD>
              <TD>20.88</TD>
              <TD>-2140405448</TD>
              <TD>22.082</TD>
              <TD>19.8812</TD>
              <TD>3</TD>
              <TD>13.74</TD>
              <TD>0.029999999999999999</TD>
            </TR>
            <TR>
              <TD>0.51784</TD>
              <TD>0.252201</TD>
              <TD>536871168</TD>
              <TD>23.0853</TD>
              <TD>65552</TD>
              <TD>20.7891</TD>
              <TD>0.35873</TD>
              <TD>0.210251239819</TD>
              <TD>1.20311</TD>
              <TD>0.161275</TD>
              <TD>359.749274134</TD>
              <TD>21.7686</TD>
              <TD>-2140404569</TD>
              <TD>23.0889</TD>
              <TD>20.0088</TD>
              <TD>3</TD>
              <TD>12.023</TD>
              <TD>0.02</TD>
            </TR>
          </TABLEDATA>
      </DATA>
    </TABLE>

    val skyObjectsWithProperMotion =
      <TABLE>
        <FIELD ID="pmde" datatype="double" name="pmde" ucd="pos.pm;pos.eq.dec"/>
        <FIELD ID="pmra" datatype="double" name="pmra" ucd="pos.pm;pos.eq.ra"/>
        <FIELD ID="dej2000" datatype="double" name="dej2000" ucd="pos.eq.dec;meta.main"/>
        <FIELD ID="epde" datatype="double" name="epde" ucd="time.epoch"/>
        <FIELD ID="raj2000" datatype="double" name="raj2000" ucd="pos.eq.ra;meta.main"/>
        <FIELD ID="rmag" datatype="double" name="rmag" ucd="phot.mag;em.opt.R"/>
        <FIELD ID="e_vmag" datatype="double" name="e_vmag" ucd="stat.error;phot.mag;em.opt.V"/>
        <FIELD ID="e_pmra" datatype="double" name="e_pmra" ucd="stat.error;pos.pm;pos.eq.ra"/>
        <FIELD ID="ucac4" arraysize="*" datatype="char" name="ucac4" ucd="meta.id;meta.main"/>
        <FIELD ID="epra" datatype="double" name="epra" ucd="time.epoch"/>
        <FIELD ID="e_pmde" datatype="double" name="e_pmde" ucd="stat.error;pos.pm;pos.eq.dec"/>
        <DATA>
         <TABLEDATA>
          <TR>
           <TD>-4.9000000000000004</TD>
           <TD>-10.199999999999999</TD>
           <TD>19.9887894444444</TD>
           <TD>2000.3499999999999</TD>
           <TD>9.8971419444444404</TD>
           <TD>14.76</TD>
           <TD>0.02</TD>
           <TD>3.8999999999999999</TD>
           <TD>550-001323</TD>
           <TD>1999.9100000000001</TD>
            <TD>2.4345</TD>
          </TR>
          <TR>
           <TD>-13.9</TD>
           <TD>-7</TD>
           <TD>19.997709722222201</TD>
           <TD>2000.0699999999999</TD>
           <TD>9.9195805555555605</TD>
           <TD>12.983000000000001</TD>
           <TD>0.029999999999999999</TD>
           <TD>1.8</TD>
           <TD>550-001324</TD>
           <TD>1999.4300000000001</TD>
           <TD>2.3999999999999999</TD>
          </TR>
         </TABLEDATA>
        </DATA>
       </TABLE>

    val voTable =
      <VOTABLE>
        <RESOURCE type="results">
          {skyObjects}
        </RESOURCE>
      </VOTABLE>

    val voTableWithErrors =
      <VOTABLE>
        <RESOURCE type="results">
          {skyObjectsWithErrors}
        </RESOURCE>
      </VOTABLE>

    val voTableWithProperMotion =
      <VOTABLE>
        <RESOURCE type="results">
          {skyObjectsWithProperMotion}
        </RESOURCE>
      </VOTABLE>

    "be able to parse empty ucds" in {
      Ucd.parseUcd("") should beEqualTo(Ucd(List()))
    }
    "be able to parse single token ucds" in {
      Ucd.parseUcd("meta.code") should beEqualTo(Ucd(List(UcdWord("meta.code"))))
    }
    "be able to parse multi token ucds and preserve order" in {
      Ucd.parseUcd("stat.error;phot.mag;em.opt.g") should beEqualTo(Ucd(List(UcdWord("stat.error"), UcdWord("phot.mag"), UcdWord("em.opt.g"))))
    }
    "be able to parse be case-insensitive, converting to lower case" in {
      Ucd.parseUcd("STAT.Error;EM.opt.G") should beEqualTo(Ucd("stat.error;em.opt.g"))
    }
    "be able to parse a field definition" in {
      val fieldXml = <FIELD ID="gmag_err" datatype="double" name="gmag_err" ucd="stat.error;phot.mag;em.opt.g"/>
      parseFieldDescriptor(fieldXml) should beSome(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err"))
      // Empty field
      parseFieldDescriptor(<FIELD/>) should beNone
      // non field xml
      parseFieldDescriptor(<TAG/>) should beNone
      // missing attributes
      parseFieldDescriptor(<FIELD ID="abc"/>) should beNone
    }
    "be able to parse a list of fields" in {
      val result =
        FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err") ::
        FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err") ::
        FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1") :: Nil

      parseFields(fieldsNode) should beEqualTo(result)
    }
    "be able to parse a data  row with a list of fields" in {
      val fields = parseFields(fieldsNode)

      val result = TableRow(
        TableRowItem(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err"), "0.0960165") ::
        TableRowItem(FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err"), "0.0503736") ::
        TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"), "268435728") :: Nil
      )
      parseTableRow(fields, tableRow) should beEqualTo(result)
    }
    "be able to parse a list of rows with a list of fields" in {
      val fields = parseFields(fieldsNode)

      val result = List(
        TableRow(
          TableRowItem(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err"), "0.0960165") ::
          TableRowItem(FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err"), "0.0503736") ::
          TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"), "268435728") :: Nil
        ),
        TableRow(
          TableRowItem(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err"), "0.51784") ::
          TableRowItem(FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err"), "0.252201") ::
          TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"), "536871168") :: Nil
        ))
      parseTableRows(fields, dataNode) should beEqualTo(result)
    }
    "be able to parse a list of rows with a list of fields" in {
      val fields = parseFields(fieldsNode)

      val result = List(
        TableRow(
          TableRowItem(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err"), "0.0960165") ::
          TableRowItem(FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err"), "0.0503736") ::
          TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"), "268435728") :: Nil
        ),
        TableRow(
          TableRowItem(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err"), "0.51784") ::
          TableRowItem(FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err"), "0.252201") ::
          TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"), "536871168") :: Nil
        ))
      parseTableRows(fields, dataNode) should beEqualTo(result)
    }
    "be able to convert a TableRow into a SiderealTarget" in {
      val validRow = TableRow(
                TableRowItem(FieldDescriptor(FieldId("objid", Ucd("meta.id;meta.main")), "objid"), "123456") ::
                TableRowItem(FieldDescriptor(FieldId("dej2000", Ucd("pos.eq.dec;meta.main")),"dej2000"), "0.209323681906") ::
                TableRowItem(FieldDescriptor(FieldId("raj2000", Ucd("pos.eq.ra;meta.main")), "raj2000"), "359.745951955") :: Nil
              )
      tableRow2Target(Nil)(validRow) should beEqualTo(\/-(SiderealTarget("123456", Coordinates(RightAscension.fromAngle(Angle.parseDegrees("359.745951955").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDegrees("0.209323681906").getOrElse(Angle.zero)).getOrElse(Declination.zero)), None, Nil, None)))

      val rowWithMissingId = TableRow(
                TableRowItem(FieldDescriptor(FieldId("dej2000", Ucd("pos.eq.dec;meta.main")), "dej2000"), "0.209323681906") ::
                TableRowItem(FieldDescriptor(FieldId("raj2000", Ucd("pos.eq.ra;meta.main")), "raj2000"), "359.745951955") :: Nil
              )
      tableRow2Target(Nil)(rowWithMissingId) should beEqualTo(-\/(MissingValues(List(VoTableParser.UCD_OBJID))))

      val rowWithBadRa = TableRow(
                TableRowItem(FieldDescriptor(FieldId("objid", Ucd("meta.id;meta.main")), "objid"), "123456") ::
                TableRowItem(FieldDescriptor(FieldId("dej2000", Ucd("pos.eq.dec;meta.main")), "dej2000"), "0.209323681906") ::
                TableRowItem(FieldDescriptor(FieldId("raj2000", Ucd("pos.eq.ra;meta.main")), "raj2000"), "ABC") :: Nil
            )
      tableRow2Target(Nil)(rowWithBadRa) should beEqualTo(-\/(FieldValueProblem(VoTableParser.UCD_RA, "ABC")))
    }
    "be able to parse magnitudes' band" in {
      val iMagField = Ucd("phot.mag;em.opt.i")
      // Optical band
      parseBands(DefaultFilter)((FieldId("id", iMagField), "20.3051")) should beEqualTo(\/-((MagnitudeBand.I, 20.3051)))

      val jIRMagField = Ucd("phot.mag;em.IR.J")
      // IR band
      parseBands(DefaultFilter)((FieldId("id", jIRMagField), "13.2349")) should beEqualTo(\/-((MagnitudeBand.J, 13.2349)))

      val jIRErrMagField = Ucd("stat.error;phot.mag;em.IR.J")
      // IR Error
      parseBands(DefaultFilter)((FieldId("id", jIRErrMagField), "0.02")) should beEqualTo(\/-((MagnitudeBand.J, 0.02)))

      // No magnitude field
      val badField = Ucd("meta.name")
      parseBands(DefaultFilter)((FieldId("id", badField), "id")) should beEqualTo(-\/(UnmatchedField(badField)))

      // Bad value
      parseBands(DefaultFilter)((FieldId("id", iMagField), "stringValue")) should beEqualTo(-\/(FieldValueProblem(iMagField, "stringValue")))

      // Unknown magnitude
      val noBandField = Ucd("phot.mag;em.opt.p")
      parseBands(DefaultFilter)((FieldId("id", noBandField), "stringValue")) should beEqualTo(-\/(UnmatchedField(noBandField)))
    }
    "be able to map sloan magnitudes in UCAC4, OCSADV-245" in {
      val gMagField = Ucd("phot.mag;em.opt.R")
      // gmag maps to g'
      parseBands(UCAC4Filter)((FieldId("gmag", gMagField), "20.3051")) should beEqualTo(\/-((MagnitudeBand._g, 20.3051)))

      val rMagField = Ucd("phot.mag;em.opt.R")
      // rmag maps to r'
      parseBands(UCAC4Filter)((FieldId("rmag", rMagField), "20.3051")) should beEqualTo(\/-((MagnitudeBand._r, 20.3051)))

      val iMagField = Ucd("phot.mag;em.opt.I")
      // imag maps to r'
      parseBands(UCAC4Filter)((FieldId("imag", iMagField), "20.3051")) should beEqualTo(\/-((MagnitudeBand._i, 20.3051)))
    }
    "be able to parse an xml into a list of SiderealTargets list of rows with a list of fields" in {
      val magsTarget1 = List(new Magnitude(22.082, MagnitudeBand.G), new Magnitude(20.3051, MagnitudeBand.I), new Magnitude(20.88, MagnitudeBand.R), new Magnitude(23.0888, MagnitudeBand.U), new Magnitude(19.8812, MagnitudeBand.Z))
      val magsTarget2 = List(new Magnitude(23.0889, MagnitudeBand.G), new Magnitude(20.7891, MagnitudeBand.I), new Magnitude(21.7686, MagnitudeBand.R), new Magnitude(23.0853, MagnitudeBand.U), new Magnitude(20.0088, MagnitudeBand.Z))

      val result = ParsedTable(List(
        \/-(SiderealTarget("-2140405448", Coordinates(RightAscension.fromDegrees(359.745951955), Declination.fromAngle(Angle.parseDegrees("0.209323681906").getOrElse(Angle.zero)).getOrElse(Declination.zero)), None, magsTarget1, None)),
        \/-(SiderealTarget("-2140404569", Coordinates(RightAscension.fromDegrees(359.749274134), Declination.fromAngle(Angle.parseDegrees("0.210251239819").getOrElse(Angle.zero)).getOrElse(Declination.zero)), None, magsTarget2, None))
      ))
      // There is only one table
      parse(voTable).tables(0) should beEqualTo(result)
      parse(voTable).tables(0).containsError should beFalse
    }
    "be able to parse an xml into a list of SiderealTargets including magnitude errors" in {
      val magsTarget1 = List(new Magnitude(22.082, MagnitudeBand.G, 0.0960165), new Magnitude(20.3051, MagnitudeBand.I, 0.0456069), new Magnitude(13.74, MagnitudeBand.J, 0.03), new Magnitude(20.88, MagnitudeBand.R, 0.0503736), new Magnitude(23.0888, MagnitudeBand.U, 0.518214), new Magnitude(19.8812, MagnitudeBand.Z, 0.138202))
      val magsTarget2 = List(new Magnitude(23.0889, MagnitudeBand.G, 0.51784), new Magnitude(20.7891, MagnitudeBand.I, 0.161275), new Magnitude(12.023, MagnitudeBand.J, 0.02), new Magnitude(21.7686, MagnitudeBand.R, 0.252201), new Magnitude(23.0853, MagnitudeBand.U, 1.20311), new Magnitude(20.0088, MagnitudeBand.Z, 0.35873))

      val result = ParsedTable(List(
        \/-(SiderealTarget("-2140405448", Coordinates(RightAscension.fromDegrees(359.745951955), Declination.fromAngle(Angle.parseDegrees("0.209323681906").getOrElse(Angle.zero)).getOrElse(Declination.zero)), None, magsTarget1, None)),
        \/-(SiderealTarget("-2140404569", Coordinates(RightAscension.fromDegrees(359.749274134), Declination.fromAngle(Angle.parseDegrees("0.210251239819").getOrElse(Angle.zero)).getOrElse(Declination.zero)), None, magsTarget2, None))
      ))
      parse(voTableWithErrors).tables(0) should beEqualTo(result)
    }
    "be able to parse an xml into a list of SiderealTargets including proper motion" in {
      val magsTarget1 = List(new Magnitude(14.76, MagnitudeBand._r))
      val magsTarget2 = List(new Magnitude(12.983, MagnitudeBand._r))
      val pm1 = ProperMotion(RightAscensionAngularVelocity(AngularVelocity(-10.199999999999999)), DeclinationAngularVelocity(AngularVelocity(-4.9000000000000004))).some
      val pm2 = ProperMotion(RightAscensionAngularVelocity(AngularVelocity(-7)), DeclinationAngularVelocity(AngularVelocity(-13.9))).some

      val result = ParsedTable(List(
        \/-(SiderealTarget("550-001323", Coordinates(RightAscension.fromDegrees(9.897141944444456), Declination.fromAngle(Angle.parseDegrees("19.98878944444442").getOrElse(Angle.zero)).getOrElse(Declination.zero)), pm1, magsTarget1, None)),
        \/-(SiderealTarget("550-001324", Coordinates(RightAscension.fromDegrees(9.91958055555557), Declination.fromAngle(Angle.parseDegrees("19.997709722222226").getOrElse(Angle.zero)).getOrElse(Declination.zero)), pm2, magsTarget2, None))
      ))
      parse(voTableWithProperMotion).tables(0) should beEqualTo(result)
    }
    "be able to validate and parse an xml from sds9" in {
      val badXml = "votable-non-validating.xml"
      VoTableParser.parse(badXml, getClass.getResourceAsStream(s"/$badXml")) should beEqualTo(-\/(ValidationError(badXml)))

      val goodXml = "votable.xml"
      VoTableParser.parse(goodXml, getClass.getResourceAsStream(s"/$goodXml")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
      VoTableParser.parse(goodXml, getClass.getResourceAsStream(s"/$goodXml")).getOrElse(ParsedVoResource(Nil)).tables should be size 1
    }
    "be able to validate and parse an xml from ucac4" in {
      val xmlFile = "votable-ucac4.xml"
      VoTableParser.parse(xmlFile, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
      VoTableParser.parse(xmlFile, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables should be size 1
    }
    "be able to filter out bad magnitudes" in {
      val xmlFile = "fmag.xml"
      VoTableParser.parse(xmlFile, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
      // The sample has only one row
      val result = VoTableParser.parse(xmlFile, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.map(_.rows.headOption).flatten.get

      val mags = result.map(_.magnitudeIn(MagnitudeBand.R))
      // Does not contain R as it is filtered out being magnitude 20 and error 99
      mags should beEqualTo(\/.right(None))
    }
    "convert fmag to UC" in {
      val xmlFile = "fmag.xml"
      VoTableParser.parse(xmlFile, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
      // The sample has only one row
      val result = VoTableParser.parse(xmlFile, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.map(_.rows.headOption).flatten.get

      val mags = result.map(_.magnitudeIn(MagnitudeBand.UC))
      // Fmag gets converted to UC
      mags should beEqualTo(\/.right(Some(Magnitude(5.9, MagnitudeBand.UC, None, MagnitudeSystem.VEGA))))
    }
    "extract Sloan's band" in {
      val xmlFile = "sloan.xml"
      // The sample has only one row
      val result = VoTableParser.parse(xmlFile, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.map(_.rows.headOption).flatten.get

      val gmag = result.map(_.magnitudeIn(MagnitudeBand._g))
      // gmag gets converted to g'
      gmag should beEqualTo(\/.right(Some(Magnitude(15.0, MagnitudeBand._g, 0.39.some, MagnitudeSystem.VEGA))))
      val rmag = result.map(_.magnitudeIn(MagnitudeBand._r))
      // rmag gets converted to r'
      rmag should beEqualTo(\/.right(Some(Magnitude(13.2, MagnitudeBand._r, 0.5.some, MagnitudeSystem.VEGA))))
      val imag = result.map(_.magnitudeIn(MagnitudeBand._i))
      // rmag gets converted to r'
      imag should beEqualTo(\/.right(Some(Magnitude(5, MagnitudeBand._i, 0.34.some, MagnitudeSystem.VEGA))))
    }
  }
}
