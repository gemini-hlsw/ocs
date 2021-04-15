package edu.gemini.catalog.votable

import edu.gemini.catalog.api.CatalogName
import edu.gemini.spModel.core._
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._

class VoTableParserSpec extends Specification with VoTableParser {

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
        <FIELD ID="ppmxl" datatype="int" name="ppmxl" ucd="meta.id;meta.main"/>
      </TABLE>

    val tableRow =
      <TR>
        <TD>0.0960165</TD>
        <TD>0.0503736</TD>
        <TD>268435728</TD>
        <TD>-2140405448</TD>
      </TR>

    val dataNode =
      <DATA>
        <TABLEDATA>
          <TR>
            <TD>0.0960165</TD>
            <TD>0.0503736</TD>
            <TD>268435728</TD>
            <TD>-2140405448</TD>
          </TR>
          <TR>
            <TD>0.51784</TD>
            <TD>0.252201</TD>
            <TD>536871168</TD>
            <TD>-2140404569</TD>
          </TR>
        </TABLEDATA>
      </DATA>

    val skyObjects =
      <TABLE>
        <FIELD ID="flags1" datatype="int" name="flags1" ucd="meta.code"/>
        <FIELD ID="umag" datatype="double" name="umag" ucd="phot.mag;em.opt.u"/>
        <FIELD ID="flags2" datatype="int" name="flags2" ucd="meta.code"/>
        <FIELD ID="imag" datatype="double" name="imag" ucd="phot.mag;em.opt.i"/>
        <FIELD ID="decj2000" datatype="double" name="dej2000" ucd="pos.eq.dec;meta.main"/>
        <FIELD ID="raj2000" datatype="double" name="raj2000" ucd="pos.eq.ra;meta.main"/>
        <FIELD ID="rmag" datatype="double" name="rmag" ucd="phot.mag;em.opt.r"/>
        <FIELD ID="objid" datatype="int" name="objid" ucd="meta.id;meta.main"/>
        <FIELD ID="gmag" datatype="double" name="gmag" ucd="phot.mag;em.opt.g"/>
        <FIELD ID="zmag" datatype="double" name="zmag" ucd="phot.mag;em.opt.z"/>
        <FIELD ID="type" datatype="int" name="type" ucd="meta.code"/>
        <FIELD ID="ppmxl" datatype="int" name="ppmxl" ucd="meta.id;meta.main"/>
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
              <TD>-2140405448</TD>
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
              <TD>-2140404569</TD>
            </TR>
          </TABLEDATA>
      </DATA>
    </TABLE>

    val skyObjectsWithRedshift =
      <TABLE>
        <FIELD ID="flags1" datatype="int" name="flags1" ucd="meta.code"/>
        <FIELD ID="umag" datatype="double" name="umag" ucd="phot.mag;em.opt.u"/>
        <FIELD ID="flags2" datatype="int" name="flags2" ucd="meta.code"/>
        <FIELD ID="imag" datatype="double" name="imag" ucd="phot.mag;em.opt.i"/>
        <FIELD ID="decj2000" datatype="double" name="dej2000" ucd="pos.eq.dec;meta.main"/>
        <FIELD ID="raj2000" datatype="double" name="raj2000" ucd="pos.eq.ra;meta.main"/>
        <FIELD ID="rmag" datatype="double" name="rmag" ucd="phot.mag;em.opt.r"/>
        <FIELD ID="objid" datatype="int" name="objid" ucd="meta.id;meta.main"/>
        <FIELD ID="gmag" datatype="double" name="gmag" ucd="phot.mag;em.opt.g"/>
        <FIELD ID="zmag" datatype="double" name="zmag" ucd="phot.mag;em.opt.z"/>
        <FIELD ID="type" datatype="int" name="type" ucd="meta.code"/>
        <FIELD ID="ppmxl" datatype="int" name="ppmxl" ucd="meta.id;meta.main"/>
        <FIELD ID="Z_VALUE" datatype="double" name="Z_VALUE" ucd="src.redshift"/>
        <FIELD ID="RV_VALUE" datatype="double" name="RV_VALUE" ucd="spect.dopplerVeloc.opt"/>
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
              <TD>-2140405448</TD>
              <TD>0.000068</TD>
              <TD></TD>
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
              <TD>-2140404569</TD>
              <TD></TD>
              <TD>20.30</TD>
            </TR>
          </TABLEDATA>
      </DATA>
    </TABLE>

    val skyObjectsWithRadialVelocityError =
      <TABLE>
        <FIELD ID="flags1" datatype="int" name="flags1" ucd="meta.code"/>
        <FIELD ID="umag" datatype="double" name="umag" ucd="phot.mag;em.opt.u"/>
        <FIELD ID="flags2" datatype="int" name="flags2" ucd="meta.code"/>
        <FIELD ID="imag" datatype="double" name="imag" ucd="phot.mag;em.opt.i"/>
        <FIELD ID="decj2000" datatype="double" name="dej2000" ucd="pos.eq.dec;meta.main"/>
        <FIELD ID="raj2000" datatype="double" name="raj2000" ucd="pos.eq.ra;meta.main"/>
        <FIELD ID="rmag" datatype="double" name="rmag" ucd="phot.mag;em.opt.r"/>
        <FIELD ID="objid" datatype="int" name="objid" ucd="meta.id;meta.main"/>
        <FIELD ID="gmag" datatype="double" name="gmag" ucd="phot.mag;em.opt.g"/>
        <FIELD ID="zmag" datatype="double" name="zmag" ucd="phot.mag;em.opt.z"/>
        <FIELD ID="type" datatype="int" name="type" ucd="meta.code"/>
        <FIELD ID="ppmxl" datatype="int" name="ppmxl" ucd="meta.id;meta.main"/>
        <FIELD ID="RV_VALUE" datatype="double" name="RV_VALUE" ucd="spect.dopplerVeloc.opt"/>
        <DATA>
          <TABLEDATA>
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
              <TD>-2140404569</TD>
              <TD>299792.459</TD>
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
        <FIELD ID="decj2000" datatype="double" name="dej2000" ucd="pos.eq.dec;meta.main"/>
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
        <FIELD ID="ppmxl" datatype="int" name="ppmxl" ucd="meta.id;meta.main"/>
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
              <TD>-2140405448</TD>
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
              <TD>-2140404569</TD>
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

    val gaia =
      <TABLE>
        <FIELD arraysize="*" datatype="char" name="designation" ucd="meta.id;meta.main">
          <DESCRIPTION>Unique source designation (unique across all Data Releases)</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="ra" ref="GAIADR2" ucd="pos.eq.ra;meta.main" unit="deg" utype="Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1">
          <DESCRIPTION>Right ascension</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="ra_error" ucd="stat.error;pos.eq.ra" unit="mas">
          <DESCRIPTION>Standard error of right ascension</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="dec" ref="GAIADR2" ucd="pos.eq.dec;meta.main" unit="deg" utype="Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2">
          <DESCRIPTION>Declination</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="dec_error" ucd="stat.error;pos.eq.dec" unit="mas">
          <DESCRIPTION>Standard error of declination</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="parallax" ucd="pos.parallax" unit="mas">
          <DESCRIPTION>Parallax</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="pmra" ucd="pos.pm;pos.eq.ra" unit="mas.yr**-1">
          <DESCRIPTION>Proper motion in right ascension direction</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="pmra_error" ucd="stat.error;pos.pm;pos.eq.ra" unit="mas.yr**-1">
          <DESCRIPTION>Standard error of proper motion in right ascension direction</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="pmdec" ucd="pos.pm;pos.eq.dec" unit="mas.yr**-1">
          <DESCRIPTION>Proper motion in declination direction</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="pmdec_error" ucd="stat.error;pos.pm;pos.eq.dec" unit="mas.yr**-1">
          <DESCRIPTION>Standard error of proper motion in declination direction</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="ref_epoch" ucd="meta.ref;time.epoch" unit="yr">
          <DESCRIPTION>Reference epoch</DESCRIPTION>
        </FIELD>
        <FIELD datatype="float" name="phot_g_mean_mag" ucd="phot.mag;stat.mean;em.opt" unit="mag">
          <DESCRIPTION>G-band mean magnitude</DESCRIPTION>
        </FIELD>
        <FIELD datatype="float" name="bp_rp" ucd="phot.color" unit="mag">
          <DESCRIPTION>BP - RP colour</DESCRIPTION>
        </FIELD>
        <FIELD datatype="double" name="radial_velocity" ucd="spect.dopplerVeloc.opt" unit="km.s**-1">
          <DESCRIPTION>Radial velocity</DESCRIPTION>
        </FIELD>
        <DATA>
          <TABLEDATA>
            <TR>
              <TD>Gaia DR2 5500810292414804352</TD>
              <TD>95.97543693997628</TD>
              <TD>0.8972436225190542</TD>
              <TD>-52.74602088557901</TD>
              <TD>1.1187287208599193</TD>
              <TD>-0.059333971256738484</TD>
              <TD>5.444032860309618</TD>
              <TD>2.0096218591421637</TD>
              <TD>2.412759805075276</TD>
              <TD>2.292112882376078</TD>
              <TD>2015.5</TD>
              <TD>19.782911</TD>
              <TD></TD> <!-- No BP - RP means no magnitude information -->
              <TD></TD>
            </TR>
            <TR>
              <TD>Gaia DR2 5500810842175280768</TD>
              <TD>96.07794677734371</TD>
              <TD>1.7974083970121115</TD>
              <TD>-52.752866472994484</TD>
              <TD>1.3361631129404261</TD>
              <TD></TD>
              <TD></TD>
              <TD></TD>
              <TD></TD>
              <TD></TD>
              <TD>2015.5</TD>
              <TD></TD> <!-- No G-band means no magnitude information -->
              <TD></TD>
              <TD></TD>
            </TR>
            <TR>
              <TD>Gaia DR2 5500810223699979264</TD>
              <TD>95.96329279548434</TD>
              <TD>0.01360005536042634</TD>
              <TD>-52.77304994651542</TD>
              <TD>0.01653042640473304</TD>
              <TD>1.0777658952216769</TD>
              <TD>-0.8181139364821904</TD>
              <TD>0.028741305378710533</TD>
              <TD>12.976157539714205</TD>
              <TD>0.031294621220519486</TD>
              <TD>2015.5</TD>
              <TD>13.91764</TD>
              <TD>2.68324375</TD>
              <TD></TD>
            </TR>
            <TR>
              <TD>Gaia DR2 5500810326779190016</TD>
              <TD>95.98749097569124</TD>
              <TD>0.0862887211183082</TD>
              <TD>-52.741666247338124</TD>
              <TD>0.09341802945058283</TD>
              <TD>3.6810721649521616</TD>
              <TD>6.456830239423608</TD>
              <TD>0.19897351485381112</TD>
              <TD>22.438383124975978</TD>
              <TD>0.18174463860202664</TD>
              <TD>2015.5</TD>
              <TD>14.292543</TD>
              <TD>1.0745363</TD>
              <TD>20.30</TD>  <!-- Radial velocity -->
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

    val voTableWithRedshift =
      <VOTABLE>
        <RESOURCE type="results">
          {skyObjectsWithRedshift}
        </RESOURCE>
      </VOTABLE>

    val voTableWithRadialVelocityError =
      <VOTABLE>
        <RESOURCE type="results">
          {skyObjectsWithRadialVelocityError}
        </RESOURCE>
      </VOTABLE>

    val voTableGaia =
      <VOTABLE>
        <RESOURCE type="results">
          {gaia}
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
    "swap in name for ID if missing in a field definition" in {
      val fieldXml = <FIELD datatype="double" name="ref_epoch" ucd="meta.ref;time.epoch" unit="yr"/>
      parseFieldDescriptor(fieldXml) should beSome(FieldDescriptor(FieldId("ref_epoch", Ucd("meta.ref;time.epoch")), "ref_epoch"))
    }
    "be able to parse a list of fields" in {
      val result =
        FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err") ::
        FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err") ::
        FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1") ::
        FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl") :: Nil

      parseFields(fieldsNode) should beEqualTo(result)
    }
    "be able to parse a data  row with a list of fields" in {
      val fields = parseFields(fieldsNode)

      val result = TableRow(
        TableRowItem(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err"), "0.0960165") ::
        TableRowItem(FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err"), "0.0503736") ::
        TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"), "268435728") ::
        TableRowItem(FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl"), "-2140405448") :: Nil
      )
      parseTableRow(fields, tableRow) should beEqualTo(result)
    }
    "be able to parse a list of rows with a list of fields" in {
      val fields = parseFields(fieldsNode)

      val result = List(
        TableRow(
          TableRowItem(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err"), "0.0960165") ::
          TableRowItem(FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err"), "0.0503736") ::
          TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"), "268435728") ::
          TableRowItem(FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl"), "-2140405448") :: Nil
        ),
        TableRow(
          TableRowItem(FieldDescriptor(FieldId("gmag_err", Ucd("stat.error;phot.mag;em.opt.g")), "gmag_err"), "0.51784") ::
          TableRowItem(FieldDescriptor(FieldId("rmag_err", Ucd("stat.error;phot.mag;em.opt.r")), "rmag_err"), "0.252201") ::
          TableRowItem(FieldDescriptor(FieldId("flags1", Ucd("meta.code")), "flags1"), "536871168") ::
          TableRowItem(FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl"), "-2140404569") :: Nil
        ))
      parseTableRows(fields, dataNode) should beEqualTo(result)
    }
    "be able to convert a TableRow into a SiderealTarget" in {
      val fields = parseFields(fieldsNode)

      val validRow = TableRow(
                TableRowItem(FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl"), "123456") ::
                TableRowItem(FieldDescriptor(FieldId("decj2000", Ucd("pos.eq.dec;meta.main")),"dej2000"), "0.209323681906") ::
                TableRowItem(FieldDescriptor(FieldId("raj2000", Ucd("pos.eq.ra;meta.main")), "raj2000"), "359.745951955") :: Nil
              )
      tableRow2Target(CatalogAdapter.PPMXL, fields)(validRow) should beEqualTo(\/-(SiderealTarget.empty.copy(name = "123456", coordinates = Coordinates(RightAscension.fromAngle(Angle.parseDegrees("359.745951955").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDegrees("0.209323681906").getOrElse(Angle.zero)).getOrElse(Declination.zero)))))

      val rowWithMissingId = TableRow(
                TableRowItem(FieldDescriptor(FieldId("decj2000", Ucd("pos.eq.dec;meta.main")), "dej2000"), "0.209323681906") ::
                TableRowItem(FieldDescriptor(FieldId("raj2000", Ucd("pos.eq.ra;meta.main")), "raj2000"), "359.745951955") :: Nil
              )
      tableRow2Target(CatalogAdapter.PPMXL, fields)(rowWithMissingId) should beEqualTo(-\/(MissingValue(FieldId("ppmxl", VoTableParser.UCD_OBJID))))

      val rowWithBadRa = TableRow(
                TableRowItem(FieldDescriptor(FieldId("ppmxl", Ucd("meta.id;meta.main")), "ppmxl"), "123456") ::
                TableRowItem(FieldDescriptor(FieldId("decj2000", Ucd("pos.eq.dec;meta.main")), "dej2000"), "0.209323681906") ::
                TableRowItem(FieldDescriptor(FieldId("raj2000", Ucd("pos.eq.ra;meta.main")), "raj2000"), "ABC") :: Nil
            )
      tableRow2Target(CatalogAdapter.PPMXL, fields)(rowWithBadRa) should beEqualTo(-\/(FieldValueProblem(VoTableParser.UCD_RA, "ABC")))
    }
    "be able to parse magnitude bands in PPMXL" in {
      val iMagField = Ucd("phot.mag;em.opt.i")
      // Optical band
      CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", iMagField), "20.3051")) should beEqualTo(\/-((FieldId("id", iMagField), MagnitudeBand.I, 20.3051)))

      val jIRMagField = Ucd("phot.mag;em.IR.J")
      // IR band
      CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", jIRMagField), "13.2349")) should beEqualTo(\/-((FieldId("id", jIRMagField), MagnitudeBand.J, 13.2349)))

      val jIRErrMagField = Ucd("stat.error;phot.mag;em.IR.J")
      // IR Error
      CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", jIRErrMagField), "0.02")) should beEqualTo(\/-((FieldId("id", jIRErrMagField), MagnitudeBand.J, 0.02)))

      // No magnitude field
      val badField = Ucd("meta.name")
      CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", badField), "id")) should beEqualTo(-\/(UnmatchedField(badField)))

      // Bad value
      CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", iMagField), "stringValue")) should beEqualTo(-\/(FieldValueProblem(iMagField, "stringValue")))

      // Unknown magnitude
      val noBandField = Ucd("phot.mag;em.opt.p")
      CatalogAdapter.PPMXL.parseMagnitude((FieldId("id", noBandField), "stringValue")) should beEqualTo(-\/(UnmatchedField(noBandField)))
    }
    "be able to map sloan magnitudes in UCAC4, OCSADV-245" in {
      val gMagField = Ucd("phot.mag;em.opt.R")
      // gmag maps to g'
      CatalogAdapter.UCAC4.parseMagnitude((FieldId("gmag", gMagField), "20.3051")) should beEqualTo(\/-((FieldId("gmag", gMagField), MagnitudeBand._g, 20.3051)))

      val rMagField = Ucd("phot.mag;em.opt.R")
      // rmag maps to r'
      CatalogAdapter.UCAC4.parseMagnitude((FieldId("rmag", rMagField), "20.3051")) should beEqualTo(\/-((FieldId("rmag", rMagField), MagnitudeBand._r, 20.3051)))

      val iMagField = Ucd("phot.mag;em.opt.I")
      // imag maps to r'
      CatalogAdapter.UCAC4.parseMagnitude((FieldId("imag", iMagField), "20.3051")) should beEqualTo(\/-((FieldId("imag", iMagField), MagnitudeBand._i, 20.3051)))
    }
    "be able to map sloan magnitudes in Simbad" in {
      val zMagField = Ucd("phot.mag;em.opt.I")
      // FLUX_z maps to z'
      CatalogAdapter.Simbad.parseMagnitude((FieldId("FLUX_z", zMagField), "20.3051")) should beEqualTo(\/-((FieldId("FLUX_z", zMagField), MagnitudeBand._z, 20.3051)))

      val rMagField = Ucd("phot.mag;em.opt.R")
      // FLUX_r maps to r'
      CatalogAdapter.Simbad.parseMagnitude((FieldId("FLUX_r", rMagField), "20.3051")) should beEqualTo(\/-((FieldId("FLUX_r", rMagField), MagnitudeBand._r, 20.3051)))

      val uMagField = Ucd("phot.mag;em.opt.u")
      // FLUX_u maps to u'
      CatalogAdapter.Simbad.parseMagnitude((FieldId("FLUX_u", uMagField), "20.3051")) should beEqualTo(\/-((FieldId("FLUX_u", uMagField), MagnitudeBand._u, 20.3051)))

      val gMagField = Ucd("phot.mag;em.opt.b")
      // FLUX_g maps to g'
      CatalogAdapter.Simbad.parseMagnitude((FieldId("FLUX_g", gMagField), "20.3051")) should beEqualTo(\/-((FieldId("FLUX_g", gMagField), MagnitudeBand._g, 20.3051)))

      val iMagField = Ucd("phot.mag;em.opt.i")
      // FLUX_u maps to u'
      CatalogAdapter.Simbad.parseMagnitude((FieldId("FLUX_i", iMagField), "20.3051")) should beEqualTo(\/-((FieldId("FLUX_i", iMagField), MagnitudeBand._i, 20.3051)))
    }
    "be able to map non-sloan magnitudes in Simbad" in {
      val rMagField = Ucd("phot.mag;em.opt.R")
      // FLUX_R maps to R
      CatalogAdapter.Simbad.parseMagnitude((FieldId("FLUX_R", rMagField), "20.3051")) should beEqualTo(\/-((FieldId("FLUX_R", rMagField), MagnitudeBand.R, 20.3051)))

      val uMagField = Ucd("phot.mag;em.opt.U")
      // FLUX_U maps to U
      CatalogAdapter.Simbad.parseMagnitude((FieldId("FLUX_U", uMagField), "20.3051")) should beEqualTo(\/-((FieldId("FLUX_U", uMagField), MagnitudeBand.U, 20.3051)))

      val iMagField = Ucd("phot.mag;em.opt.I")
      // FLUX_I maps to I
      CatalogAdapter.Simbad.parseMagnitude((FieldId("FLUX_I", iMagField), "20.3051")) should beEqualTo(\/-((FieldId("FLUX_I", iMagField), MagnitudeBand.I, 20.3051)))
    }
    "be able to map magnitude errors in Simbad" in {
      // Magnitude errors in simbad don't include the band in the UCD, we must get it from the ID :(
      val magErrorUcd = Ucd("stat.error;phot.mag")
      // FLUX_r maps to r'
      CatalogAdapter.Simbad.parseMagnitude((FieldId("FLUX_ERROR_r", magErrorUcd), "20.3051")) should beEqualTo(\/-((FieldId("FLUX_ERROR_r", magErrorUcd), MagnitudeBand._r, 20.3051)))

      // FLUX_R maps to R
      CatalogAdapter.Simbad.parseMagnitude((FieldId("FLUX_ERROR_R", magErrorUcd), "20.3051")) should beEqualTo(\/-((FieldId("FLUX_ERROR_R", magErrorUcd), MagnitudeBand.R, 20.3051)))
    }
    "be able to parse an xml into a list of SiderealTargets list of rows with a list of fields" in {
      val magsTarget1 = List(new Magnitude(23.0888, MagnitudeBand.U), new Magnitude(22.082, MagnitudeBand._g), new Magnitude(20.88, MagnitudeBand.R), new Magnitude(20.3051, MagnitudeBand.I), new Magnitude(19.8812, MagnitudeBand._z))
      val magsTarget2 = List(new Magnitude(23.0853, MagnitudeBand.U), new Magnitude(23.0889, MagnitudeBand._g), new Magnitude(21.7686, MagnitudeBand.R), new Magnitude(20.7891, MagnitudeBand.I), new Magnitude(20.0088, MagnitudeBand._z))

      val result = ParsedTable(List(
        \/-(SiderealTarget.empty.copy(name = "-2140405448", coordinates = Coordinates(RightAscension.fromDegrees(359.745951955), Declination.fromAngle(Angle.parseDegrees("0.209323681906").getOrElse(Angle.zero)).getOrElse(Declination.zero)), magnitudes = magsTarget1)),
        \/-(SiderealTarget.empty.copy(name = "-2140404569", coordinates = Coordinates(RightAscension.fromDegrees(359.749274134), Declination.fromAngle(Angle.parseDegrees("0.210251239819").getOrElse(Angle.zero)).getOrElse(Declination.zero)), magnitudes = magsTarget2))
      ))
      // There is only one table
      parse(CatalogAdapter.PPMXL, voTable).tables.head should beEqualTo(result)
      parse(CatalogAdapter.PPMXL, voTable).tables.head.containsError should beFalse
    }
    "be able to parse an xml into a list of SiderealTargets including redshift" in {
      val result = parse(CatalogAdapter.PPMXL, voTableWithRedshift).tables.head

      // There should be no errors
      result.containsError should beFalse
      val redshifts = result.rows.map(_.toOption.get.redshift.get.z)

      // 2 redshift values both roughly equal to 0.000068.  One specified as
      // redshift and the other converted from radial velocity
      redshifts.size shouldEqual 2
      redshifts.forall(r => (r - 0.000068).abs < 0.000001) shouldEqual true
    }
    "balk if the radial velocity is faster than the speed of light" in {
      val result = parse(CatalogAdapter.PPMXL, voTableWithRadialVelocityError).tables.head
      result.rows.head.swap.toOption.get.displayValue.startsWith("Invalid radial velocity:") shouldEqual true
    }
    "be able to parse an xml into a list of SiderealTargets including magnitude errors" in {
      val magsTarget1 = List(new Magnitude(23.0888, MagnitudeBand.U, 0.518214), new Magnitude(22.082, MagnitudeBand._g, 0.0960165), new Magnitude(20.88, MagnitudeBand.R, 0.0503736), new Magnitude(20.3051, MagnitudeBand.I, 0.0456069), new Magnitude(19.8812, MagnitudeBand._z, 0.138202), new Magnitude(13.74, MagnitudeBand.J, 0.03))
      val magsTarget2 = List(new Magnitude(23.0853, MagnitudeBand.U, 1.20311), new Magnitude(23.0889, MagnitudeBand._g, 0.51784), new Magnitude(21.7686, MagnitudeBand.R, 0.252201), new Magnitude(20.7891, MagnitudeBand.I, 0.161275), new Magnitude(20.0088, MagnitudeBand._z, 0.35873), new Magnitude(12.023, MagnitudeBand.J, 0.02))

      val result = ParsedTable(List(
        \/-(SiderealTarget.empty.copy(name = "-2140405448", coordinates = Coordinates(RightAscension.fromDegrees(359.745951955), Declination.fromAngle(Angle.parseDegrees("0.209323681906").getOrElse(Angle.zero)).getOrElse(Declination.zero)), magnitudes = magsTarget1)),
        \/-(SiderealTarget.empty.copy(name = "-2140404569", coordinates = Coordinates(RightAscension.fromDegrees(359.749274134), Declination.fromAngle(Angle.parseDegrees("0.210251239819").getOrElse(Angle.zero)).getOrElse(Declination.zero)), magnitudes = magsTarget2))
      ))
      parse(CatalogAdapter.PPMXL, voTableWithErrors).tables.head should beEqualTo(result)
    }
    "be able to parse an xml into a list of SiderealTargets including proper motion" in {
      val magsTarget1 = List(new Magnitude(14.76, MagnitudeBand._r, MagnitudeSystem.AB))
      val magsTarget2 = List(new Magnitude(12.983, MagnitudeBand._r, MagnitudeSystem.AB))
      val pm1 = ProperMotion(RightAscensionAngularVelocity(AngularVelocity(-10.199999999999999)), DeclinationAngularVelocity(AngularVelocity(-4.9000000000000004))).some
      val pm2 = ProperMotion(RightAscensionAngularVelocity(AngularVelocity(-7)), DeclinationAngularVelocity(AngularVelocity(-13.9))).some

      val result = ParsedTable(List(
        \/-(SiderealTarget("550-001323", Coordinates(RightAscension.fromDegrees(9.897141944444456), Declination.fromAngle(Angle.parseDegrees("19.98878944444442").getOrElse(Angle.zero)).getOrElse(Declination.zero)), pm1, None, None, magsTarget1, None, None)),
        \/-(SiderealTarget("550-001324", Coordinates(RightAscension.fromDegrees(9.91958055555557), Declination.fromAngle(Angle.parseDegrees("19.997709722222226").getOrElse(Angle.zero)).getOrElse(Declination.zero)), pm2, None, None, magsTarget2, None, None))
      ))
      parse(CatalogAdapter.UCAC4, voTableWithProperMotion).tables.head should beEqualTo(result)
    }

    "be able to read Gaia results" in {

      // Extract just the targets.
      val result = parse(CatalogAdapter.GaiaEsa, voTableGaia).tables.head.rows.sequenceU.getOrElse(Nil)

      // 6 targets (i.e., and no errors)
      result.size shouldEqual 4

      val List(a, b, c, d) = result

      // First two targets have no magnitudes because the necessary conversion
      // information is not present.
      a.magnitudes shouldEqual Nil
      b.magnitudes shouldEqual Nil

      import MagnitudeBand._
      c.magnitudes.map(_.band).toSet shouldEqual Set(_g, _r, V, R, I, _i, K, H, J)

      // Final target has everything.
      d.magnitudes.map(_.band).toSet shouldEqual CatalogName.GaiaEsa.supportedBands.toSet

      // Even radial velocity.
      ((d.redshift.get.z - 0.000068).abs < 0.000001) shouldEqual true

      // Check conversion
      val g     = 14.2925430
      val bp_rp =  1.0745363
      val v     = g + 0.0176 + bp_rp * (0.00686 + 0.1732 * bp_rp)
      (d.magnitudes.exists { m =>
        (m.band === V) && ((m.value - v).abs < 0.000001)
      }) shouldEqual true

      // All targets with a proper motion have epoch 2015.5
      val e2015_5 = Epoch(2015.5)
      result.forall(_.properMotion.forall(_.epoch == e2015_5))
    }

    "be able to validate and parse an xml from sds9" in {
      val badXml = "votable-non-validating.xml"
      VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$badXml")) should beEqualTo(-\/(ValidationError(CatalogName.UCAC4)))
    }
    "be able to detect unknown catalogs" in {
      val xmlFile = "votable-unknown.xml"
      val result  = VoTableParser.parse(CatalogName.GSC234, getClass.getResourceAsStream(s"/$xmlFile"))
      result must beEqualTo(-\/(UnknownCatalog))
    }
    "be able to validate and parse an xml from ucac4" in {
      val xmlFile = "votable-ucac4.xml"
      VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
      VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables should be size 1
    }
    "be able to validate and parse an xml from ppmxl" in {
      val xmlFile = "votable-ppmxl.xml"
      VoTableParser.parse(CatalogName.PPMXL, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
      VoTableParser.parse(CatalogName.PPMXL, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables should be size 1
    }
    "be able to select r1mag over r2mag and b2mag when b1mag is absent in ppmxl" in {
      val xmlFile = "votable-ppmxl.xml"
      val result = VoTableParser.parse(CatalogName.PPMXL, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.map(TargetsTable.apply).map(_.rows).flatMap(_.find(_.name == "-1471224894")).headOption

      val magR = result >>= {_.magnitudeIn(MagnitudeBand.R)}
      magR.map(_.value) should beSome(18.149999999999999)
      val magB = result >>= {_.magnitudeIn(MagnitudeBand.B)}
      magB.map(_.value) should beSome(17.109999999999999)
    }
    "be able to ignore bogus magnitudes on ppmxl" in {
      val xmlFile = "votable-ppmxl.xml"
      // Check a well-known target containing invalid magnitude values an bands H, I, K and J
      val result = VoTableParser.parse(CatalogName.PPMXL, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.map(TargetsTable.apply).map(_.rows).flatMap(_.find(_.name == "-1471224894")).headOption
      val magH = result >>= {_.magnitudeIn(MagnitudeBand.H)}
      val magI = result >>= {_.magnitudeIn(MagnitudeBand.I)}
      val magK = result >>= {_.magnitudeIn(MagnitudeBand.K)}
      val magJ = result >>= {_.magnitudeIn(MagnitudeBand.J)}
      magH should beNone
      magI should beNone
      magK should beNone
      magJ should beNone
    }
    "be able to filter out bad magnitudes" in {
      val xmlFile = "fmag.xml"
      VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
      // The sample has only one row
      val result = VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get

      val mags = result.map(_.magnitudeIn(MagnitudeBand.R))
      // Does not contain R as it is filtered out being magnitude 20 and error 99
      mags should beEqualTo(\/.right(None))
    }
    "convert fmag to UC" in {
      val xmlFile = "fmag.xml"
      VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).map(_.tables.forall(!_.containsError)) must beEqualTo(\/.right(true))
      // The sample has only one row
      val result = VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get

      val mags = result.map(_.magnitudeIn(MagnitudeBand.UC))
      // Fmag gets converted to UC
      mags should beEqualTo(\/.right(Some(Magnitude(5.9, MagnitudeBand.UC, None, MagnitudeSystem.Vega))))
    }
    "extract Sloan's band" in {
      val xmlFile = "sloan.xml"
      // The sample has only one row
      val result = VoTableParser.parse(CatalogName.UCAC4, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get

      val gmag = result.map(_.magnitudeIn(MagnitudeBand._g))
      // gmag gets converted to g'
      gmag should beEqualTo(\/.right(Some(Magnitude(15.0, MagnitudeBand._g, 0.39.some, MagnitudeSystem.AB))))
      val rmag = result.map(_.magnitudeIn(MagnitudeBand._r))
      // rmag gets converted to r'
      rmag should beEqualTo(\/.right(Some(Magnitude(13.2, MagnitudeBand._r, 0.5.some, MagnitudeSystem.AB))))
      val imag = result.map(_.magnitudeIn(MagnitudeBand._i))
      // rmag gets converted to r'
      imag should beEqualTo(\/.right(Some(Magnitude(5, MagnitudeBand._i, 0.34.some, MagnitudeSystem.AB))))
    }
    "parse simbad named queries" in {
      // From http://simbad.u-strasbg.fr/simbad/sim-id?Ident=Vega&output.format=VOTable
      val xmlFile = "simbad-vega.xml"
      // The sample has only one row
      val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get

      // id and coordinates
      result.map(_.name) should beEqualTo(\/.right("* alf Lyr"))
      result.map(_.coordinates.ra) should beEqualTo(\/.right(RightAscension.fromAngle(Angle.fromDegrees(279.23473479))))
      result.map(_.coordinates.dec) should beEqualTo(\/.right(Declination.fromAngle(Angle.fromDegrees(38.78368896)).getOrElse(Declination.zero)))
      // proper motions
      result.map(_.properMotion.map(_.deltaRA)) should beEqualTo(\/.right(Some(RightAscensionAngularVelocity(AngularVelocity(200.94)))))
      result.map(_.properMotion.map(_.deltaDec)) should beEqualTo(\/.right(Some(DeclinationAngularVelocity(AngularVelocity(286.23)))))
      // redshift
      result.map(_.redshift) should beEqualTo(\/.right(Redshift(-0.000069).some))
      // parallax
      result.map(_.parallax) should beEqualTo(\/.right(Parallax.fromMas(130.23)))
      // magnitudes
      result.map(_.magnitudeIn(MagnitudeBand.U)) should beEqualTo(\/.right(Some(new Magnitude(0.03, MagnitudeBand.U))))
      result.map(_.magnitudeIn(MagnitudeBand.B)) should beEqualTo(\/.right(Some(new Magnitude(0.03, MagnitudeBand.B))))
      result.map(_.magnitudeIn(MagnitudeBand.V)) should beEqualTo(\/.right(Some(new Magnitude(0.03, MagnitudeBand.V))))
      result.map(_.magnitudeIn(MagnitudeBand.R)) should beEqualTo(\/.right(Some(new Magnitude(0.07, MagnitudeBand.R))))
      result.map(_.magnitudeIn(MagnitudeBand.I)) should beEqualTo(\/.right(Some(new Magnitude(0.10, MagnitudeBand.I))))
      result.map(_.magnitudeIn(MagnitudeBand.J)) should beEqualTo(\/.right(Some(new Magnitude(-0.18, MagnitudeBand.J))))
      result.map(_.magnitudeIn(MagnitudeBand.H)) should beEqualTo(\/.right(Some(new Magnitude(-0.03, MagnitudeBand.H))))
      result.map(_.magnitudeIn(MagnitudeBand.K)) should beEqualTo(\/.right(Some(new Magnitude(0.13, MagnitudeBand.K))))
    }
    "parse simbad named queries with sloan magnitudes" in {
      // From http://simbad.u-strasbg.fr/simbad/sim-id?Ident=2MFGC6625&output.format=VOTable
      val xmlFile = "simbad-2MFGC6625.xml"
      // The sample has only one row
      val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get

      // id and coordinates
      result.map(_.name) should beEqualTo(\/.right("2MFGC 6625"))
      result.map(_.coordinates.ra) should beEqualTo(\/.right(RightAscension.fromAngle(Angle.fromHMS(8, 23, 54.966).getOrElse(Angle.zero))))
      result.map(_.coordinates.dec) should beEqualTo(\/.right(Declination.fromAngle(Angle.fromDMS(28, 6, 21.6792).getOrElse(Angle.zero)).getOrElse(Declination.zero)))
      // proper motions
      result.map(_.properMotion) should beEqualTo(\/.right(None))
      // redshift
      result.map(_.redshift) should beEqualTo(\/.right(Redshift(0.04724).some))
      // parallax
      result.map(_.parallax) should beEqualTo(\/.right(None))
      // magnitudes
      result.map(_.magnitudeIn(MagnitudeBand._u)) should beEqualTo(\/.right(Some(new Magnitude(17.353, MagnitudeBand._u, 0.009))))
      result.map(_.magnitudeIn(MagnitudeBand._g)) should beEqualTo(\/.right(Some(new Magnitude(16.826, MagnitudeBand._g, 0.004))))
      result.map(_.magnitudeIn(MagnitudeBand._r)) should beEqualTo(\/.right(Some(new Magnitude(17.286, MagnitudeBand._r, 0.005))))
      result.map(_.magnitudeIn(MagnitudeBand._i)) should beEqualTo(\/.right(Some(new Magnitude(16.902, MagnitudeBand._i, 0.005))))
      result.map(_.magnitudeIn(MagnitudeBand._z)) should beEqualTo(\/.right(Some(new Magnitude(17.015, MagnitudeBand._z, 0.011))))
    }
    "parse simbad named queries with mixed magnitudes" in {
      // From http://simbad.u-strasbg.fr/simbad/sim-id?Ident=2SLAQ%20J000008.13%2B001634.6&output.format=VOTable
      val xmlFile = "simbad-J000008.13.xml"
      // The sample has only one row
      val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get

      // id and coordinates
      result.map(_.name) should beEqualTo(\/.right("2SLAQ J000008.13+001634.6"))
      result.map(_.coordinates.ra) should beEqualTo(\/.right(RightAscension.fromAngle(Angle.fromHMS(0, 0, 8.136).getOrElse(Angle.zero))))
      result.map(_.coordinates.dec) should beEqualTo(\/.right(Declination.fromAngle(Angle.fromDMS(0, 16, 34.6908).getOrElse(Angle.zero)).getOrElse(Declination.zero)))
      // proper motions
      result.map(_.properMotion) should beEqualTo(\/.right(None))
      // redshift
      result.map(_.redshift) should beEqualTo(\/.right(Redshift(1.8365).some))
      // parallax
      result.map(_.parallax) should beEqualTo(\/.right(None))
      // magnitudes
      result.map(_.magnitudeIn(MagnitudeBand.B)) should beEqualTo(\/.right(Some(new Magnitude(20.35, MagnitudeBand.B))))
      result.map(_.magnitudeIn(MagnitudeBand.V)) should beEqualTo(\/.right(Some(new Magnitude(20.03, MagnitudeBand.V))))
      // Bands J, H and K for this target have no standard magnitude system
      result.map(_.magnitudeIn(MagnitudeBand.J)) should beEqualTo(\/.right(Some(new Magnitude(19.399, MagnitudeBand.J, 0.073, MagnitudeSystem.AB))))
      result.map(_.magnitudeIn(MagnitudeBand.H)) should beEqualTo(\/.right(Some(new Magnitude(19.416, MagnitudeBand.H, 0.137, MagnitudeSystem.AB))))
      result.map(_.magnitudeIn(MagnitudeBand.K)) should beEqualTo(\/.right(Some(new Magnitude(19.176, MagnitudeBand.K, 0.115, MagnitudeSystem.AB))))
      result.map(_.magnitudeIn(MagnitudeBand._u)) should beEqualTo(\/.right(Some(new Magnitude(20.233, MagnitudeBand._u, 0.054))))
      result.map(_.magnitudeIn(MagnitudeBand._g)) should beEqualTo(\/.right(Some(new Magnitude(20.201, MagnitudeBand._g, 0.021))))
      result.map(_.magnitudeIn(MagnitudeBand._r)) should beEqualTo(\/.right(Some(new Magnitude(19.929, MagnitudeBand._r, 0.021))))
      result.map(_.magnitudeIn(MagnitudeBand._i)) should beEqualTo(\/.right(Some(new Magnitude(19.472, MagnitudeBand._i, 0.023))))
      result.map(_.magnitudeIn(MagnitudeBand._z)) should beEqualTo(\/.right(Some(new Magnitude(19.191, MagnitudeBand._z, 0.068))))
    }
    "don't allow negative parallax values" in {
      // From http://simbad.u-strasbg.fr/simbad/sim-id?output.format=VOTable&Ident=HIP43018
      val xmlFile = "simbad_hip43018.xml"
      // We are interested only on the first row
      val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil)).tables.headOption.flatMap(_.rows.headOption).get

      // parallax is reported as -0.57 by Simbad, the parser makes it a 0
      result.map(_.parallax) should beEqualTo(\/.right(Parallax.zero.some))
    }
    "parse simbad with a not-found name" in {
      val xmlFile = "simbad-not-found.xml"
      // Simbad returns non-valid xml when an element is not found, we need to skip validation :S
      val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile"))
      result must beEqualTo(\/.right(ParsedVoResource(List())))
    }
    "parse simbad with an npe" in {
      val xmlFile = "simbad-npe.xml"
      // Simbad returns non-valid xml when there is an internal error like an NPE
      val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile"))
      result must beEqualTo(\/.left(ValidationError(CatalogName.SIMBAD)))
    }
    "ppmxl proper motion should be in mas/y. REL-2841" in {
      val xmlFile = "votable-ppmxl-proper-motion.xml"
      // PPMXL returns proper motion on degrees per year, it should be converted to mas/year
      val result = VoTableParser.parse(CatalogName.PPMXL, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil))

      val targets = for {
        t <- result.tables.map(TargetsTable.apply)
        r <- t.rows
        if r.name == "-1201792896"
      } yield r

      val pmRA = targets.headOption >>= {_.properMotion} >>= {_.deltaRA.some}
      val pmDec = targets.headOption >>= {_.properMotion} >>= {_.deltaDec.some}
      pmRA must beSome(RightAscensionAngularVelocity(AngularVelocity(-1.400004)))
      pmDec must beSome(DeclinationAngularVelocity(AngularVelocity(-7.56)))
    }
    "support simbad repeated magnitude entries, REL-2853" in {
      val xmlFile = "simbad-ngc-2438.xml"
      // Simbad returns an xml with multiple measurements of the same band, use only the first one
      val result = VoTableParser.parse(CatalogName.SIMBAD, getClass.getResourceAsStream(s"/$xmlFile")).getOrElse(ParsedVoResource(Nil))

      val target = (for {
          t <- result.tables.map(TargetsTable.apply)
          r <- t.rows
        } yield r).headOption
      target.map(_.name) should beSome("NGC  2438")
      target.map(_.magnitudeIn(MagnitudeBand.J)) should beSome(Some(new Magnitude(17.02, MagnitudeBand.J, 0.15, MagnitudeSystem.Vega)))
    }
  }
}
