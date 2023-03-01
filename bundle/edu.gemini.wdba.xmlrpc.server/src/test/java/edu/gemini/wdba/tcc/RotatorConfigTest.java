//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nici.NICIParams;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.visitor.VisitorConfig;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import org.dom4j.Document;
import org.dom4j.Element;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

/**
 * Test cases for RotatorConfig.  The logic is about as convoluted as the
 * class itself ...
 */
public class RotatorConfigTest extends TestBase {

    private ISPObsComponent instObsComp;
    private ISPObsComponent altairObsComp;

    private ISPObsComponent addInstrument(SPComponentType type) throws Exception {
        instObsComp = odb.getFactory().createObsComponent(prog, type, null);
        obs.addObsComponent(instObsComp);
        return instObsComp;
    }

    private InstAltair addAltair() throws Exception {
        altairObsComp = odb.getFactory().createObsComponent(prog, InstAltair.SP_TYPE, null);
        obs.addObsComponent(altairObsComp);
        return (InstAltair) altairObsComp.getDataObject();
    }

    private InstNIRI addNiri() throws Exception {
        return (InstNIRI) addInstrument(InstNIRI.SP_TYPE).getDataObject();
    }

    private InstNICI addNici() throws Exception {
        return (InstNICI) addInstrument(InstNICI.SP_TYPE).getDataObject();
    }

    private Gpi addGpi() throws Exception {
        return (Gpi) addInstrument(Gpi.SP_TYPE).getDataObject();
    }

    private VisitorInstrument addMaroonX() throws Exception {
        final ISPObsComponent   oc = addInstrument(SPComponentType.INSTRUMENT_VISITOR);
        final VisitorInstrument vi = (VisitorInstrument) oc.getDataObject();
        vi.setVisitorConfig(VisitorConfig.MaroonX$.MODULE$);
        oc.setDataObject(vi);
        return vi;
    }

    private InstGmosSouth addGmos() throws Exception {
        return (InstGmosSouth) addInstrument(InstGmosSouth.SP_TYPE).getDataObject();
    }

    private String getRotatorConfigParamValue(Document doc, Type type) throws Exception {
        Element fieldConfigElement = getTccFieldConfig(doc);

        @SuppressWarnings({"unchecked"}) List<Element> params = (List<Element>) fieldConfigElement.elements("param");
        for (Element param : params) {
            if (type.name().equals(param.attributeValue("name"))) {
                return param.attributeValue("value");
            }
        }
        fail("Could not find rotator config param value");
        return null;
    }

    private enum Site {
        north() {
            Document getResults(TestBase base) throws Exception {
                return base.getNorthResults();
            }
        },
        south() {
            Document getResults(TestBase base) throws Exception {
                return base.getSouthResults();
            }
        },
        ;
        abstract Document getResults(TestBase base) throws Exception;
    }

    private enum Type {
        posAngle,
        rotator,
        ;

        Element getElement(Document doc, TestBase base) throws Exception {
            return base.getTccFieldContainedParamSet(doc, name()).get(0);
        }
    }

    private enum RotConfigExtractor {
        name() {
            String extract(Element element) {
                return element.attributeValue("name");
            }
        },
        ipa() {
            String extract(Element element) {
                return child(element, name()).attributeValue("value");
            }
        },
        cosys() {
            String extract(Element element) {
                return child(element, name()).attributeValue("value");
            }
        },
        ;

        abstract String extract(Element element);

        private static Element child(Element element, String name) {
            //noinspection unchecked
            for (Element child : (List<Element>) element.elements()) {
                if (name.equals(child.attributeValue("name"))) return child;
            }
            fail("Could not find child element '" + name + "'");
            return null;
        }
    }

    private class RotConfigValidator {
        Site site;
        Type type;
        String name;
        String ipa;
        String cosys;

        void validate() throws Exception {
            Document res = site.getResults(RotatorConfigTest.this);

            // RotatorConfig is added as a parameter whose name is
            // based on the type "rotator" or "posAngle" and a value which
            // is, for example, "AltairFixed", "NICIFixed", or a position
            // angle.
            assertEquals(name, getRotatorConfigParamValue(res, type));

            // In addition to the parameter, we add a param set that contains
            // detail information including the actual position angle.
            Element paramSet = type.getElement(res, RotatorConfigTest.this);
            assertEquals(name, RotConfigExtractor.name.extract(paramSet));
            assertEquals(ipa, RotConfigExtractor.ipa.extract(paramSet));
            assertEquals(cosys, RotConfigExtractor.cosys.extract(paramSet));
        }

    }

    @Test public void testPosAngle() throws Exception {
        InstGmosSouth gmos = addGmos();
        gmos.setPosAngle(10.5);
        instObsComp.setDataObject(gmos);

        RotConfigValidator val = new RotConfigValidator();
        val.site  = Site.south;
        val.type  = Type.posAngle;
        val.name  = "10.5";
        val.ipa   = "10.5";
        val.cosys = TccNames.FK5J2000;
        val.validate();
    }

    @Test public void testAltairPosAngle() throws Exception {
        InstNIRI niri = addNiri();
        niri.setPosAngle(10.5);
        instObsComp.setDataObject(niri);

        InstAltair altair = addAltair();
        assertEquals(AltairParams.CassRotator.FOLLOWING, altair.getCassRotator());

        RotConfigValidator val = new RotConfigValidator();
        val.site  = Site.north;
        val.type  = Type.posAngle;
        val.name  = "10.5";
        val.ipa   = "10.5";
        val.cosys = TccNames.FK5J2000;
        val.validate();
    }

    @Test public void testAltairFixed() throws Exception {
        InstNIRI niri = addNiri();
        niri.setPosAngle(10.5);
        instObsComp.setDataObject(niri);

        InstAltair altair = addAltair();
        altair.setCassRotator(AltairParams.CassRotator.FIXED);
        altairObsComp.setDataObject(altair);

        // A param set isn't added in this case for some unknown reason.
        Document res = getNorthResults();
        getRotatorConfigParamValue(res, Type.rotator);
        assertEquals(0, getTccFieldContainedParamSet(res, "rotator").size());
    }

    @Test public void testNiciFixed() throws Exception {
        InstNICI nici = addNici();
        nici.setPosAngle(10.5);
        nici.setCassRotator(NICIParams.CassRotator.FIXED);
        instObsComp.setDataObject(nici);

        RotConfigValidator val = new RotConfigValidator();
        val.site  = Site.south;
        val.type  = Type.rotator;
        val.name  = "NICIFixed";
        val.ipa   = "10.5";
        val.cosys = TccNames.FIXED;
        val.validate();
    }

    @Test public void testNiciPosAngle() throws Exception {
        InstNICI nici = addNici();
        nici.setCassRotator(NICIParams.CassRotator.FOLLOW);
        nici.setPosAngle(10.5);
        instObsComp.setDataObject(nici);

        RotConfigValidator val = new RotConfigValidator();
        val.site  = Site.south;
        val.type  = Type.posAngle;
        val.name  = "10.5";
        val.ipa   = "10.5";
        val.cosys = TccNames.FK5J2000;
        val.validate();
    }

    @Test public void testGpiFixed() throws Exception {
        Gpi gpi = addGpi();
        instObsComp.setDataObject(gpi);

        RotConfigValidator val = new RotConfigValidator();
        val.site  = Site.south;
        val.type  = Type.rotator;
        val.name  = "GPIFixed";
        val.ipa   = "0";
        val.cosys = TccNames.FIXED;
        val.validate();
    }

    @Test public void testGpiPosAngle0() throws Exception {
        Gpi gpi = addGpi();
        gpi.setPosAngle(0);
        instObsComp.setDataObject(gpi);

        RotConfigValidator val = new RotConfigValidator();
        val.site  = Site.south;
        val.type  = Type.rotator;
        val.name  = "GPIFixed";
        val.ipa   = "0";
        val.cosys = TccNames.FIXED;
        val.validate();
    }

    @Test public void testGpiPosAngle180() throws Exception {
        Gpi gpi = addGpi();
        gpi.setPosAngle(180);
        instObsComp.setDataObject(gpi);

        RotConfigValidator val = new RotConfigValidator();
        val.site  = Site.south;
        val.type  = Type.rotator;
        val.name  = "GPIFixed";
        val.ipa   = "180";
        val.cosys = TccNames.FIXED;
        val.validate();
    }

    @Test public void testMaroonXFixed() throws Exception {
        VisitorInstrument vi = addMaroonX();
        vi.setPosAngleDegrees(15.0); // this should be ignored due to Maroon X position angle mode
        instObsComp.setDataObject(vi);

        RotConfigValidator val = new RotConfigValidator();
        val.site  = Site.north;
        val.type  = Type.rotator;
        val.name  = "fixed";
        val.ipa   = "0"; // should always be 0 for Maroon X
        val.cosys = TccNames.FIXED;
        val.validate();
    }
}
