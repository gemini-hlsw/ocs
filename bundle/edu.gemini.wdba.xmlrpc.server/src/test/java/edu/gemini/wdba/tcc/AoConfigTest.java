//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.List;

/**
 * Test cases for RotatorConfig.  The logic is about as convoluted as the
 * class itself ...
 */
public class AoConfigTest extends TestBase {

    private ISPObsComponent instObsComp;
    private ISPObsComponent gemsObsComp;
    private ISPObsComponent altairObsComp;

    private ISPObsComponent addInstrument(SPComponentType type) throws Exception {
        instObsComp = odb.getFactory().createObsComponent(prog, type, null);
        obs.addObsComponent(instObsComp);
        return instObsComp;
    }

    private Gems addGems() throws Exception {
        gemsObsComp = odb.getFactory().createObsComponent(prog, Gems.SP_TYPE, null);
        obs.addObsComponent(gemsObsComp);
        return (Gems) gemsObsComp.getDataObject();
    }

    private InstAltair addAltair() throws Exception {
        altairObsComp = odb.getFactory().createObsComponent(prog, InstAltair.SP_TYPE, null);
        obs.addObsComponent(altairObsComp);
        return (InstAltair) altairObsComp.getDataObject();
    }

    private Gsaoi addGsaoi() throws Exception {
        return (Gsaoi) addInstrument(Gsaoi.SP_TYPE).getDataObject();
    }

    private InstNIRI addNiri() throws Exception {
        return (InstNIRI) addInstrument(InstNIRI.SP_TYPE).getDataObject();
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

    private abstract class AoConfigValidator {
        Site site;
        String name = TccNames.NO_AO;

        String getParam(Element element, String paramName) {
            List<Element> lst = (List<Element>) element.selectNodes(".//param[@name='" + paramName + "']");
            if ((lst == null) || (lst.size() == 0)) return null;
            return lst.get(0).attributeValue("value");
        }

        private String getGaosName(Element tcsConfig) {
            return getParam(tcsConfig, TccNames.GAOS);
        }

        Option<Element> getGaosElement(Document doc) {
            if (TccNames.NO_AO.equals(name)) return None.instance();

            String type = site == Site.north ? TccNames.GAOS : "gems";
            Element e = getSubconfig(doc, type);
            if (e == null) return None.instance();
            return new Some<Element>(e);
        }


        void validate() throws Exception {
            Document res = site.getResults(AoConfigTest.this);

            Element tcsConfig = getTcsConfiguration(res);
            String gaosName = getGaosName(tcsConfig);
            assertEquals(name, gaosName);

            Option<Element> gaosElement = getGaosElement(res);
            if (gaosElement.isEmpty()) {
                assertEquals(TccNames.NO_AO, name);
                return;
            }

            validate(res, gaosElement.getValue());
        }

        protected abstract void validate(Document doc, Element gaosElement);
    }

    class AltairAoConfigValidator extends AoConfigValidator {
        InstAltair altair;

        protected void validate(Document doc, Element gaosElement) {
            // fill in Altair specific stuff here
            String fldLens = getParam(gaosElement, AOConfig.FLDLENS);
            assertEquals(altair.getFieldLens().sequenceValue(), fldLens);

            String ndFilter = getParam(gaosElement, AOConfig.NDFILTER);
            assertEquals(altair.getNdFilter().sequenceValue(), ndFilter);

            String wavelength = getParam(gaosElement, AOConfig.WAVELENGTH);
            assertEquals(altair.getWavelength().sequenceValue(), wavelength);
        }
    }

    class GemsAoConfigValidator extends AoConfigValidator {
        Gems gems;

        protected void validate(Document doc, Element gaosElement) {
            String adc = getParam(gaosElement, AOConfig.GEMS_GAOS_ADC);
            assertEquals(gems.getAdc().sequenceValue(), adc);

            String dich = getParam(gaosElement, AOConfig.GEMS_GAOS_DICHROIC);
            assertEquals(gems.getDichroicBeamsplitter().sequenceValue(), dich);

            String ast = getParam(gaosElement, AOConfig.GEMS_GAOS_ASTROMETRIC);
            assertEquals(gems.getAstrometricMode().sequenceValue(), ast);
        }
    }

    // Need tests for NGS/LGS etc.  Whee.
    private void testAltair(InstAltair altair) throws Exception {
        addNiri();
        addAltair();
        altairObsComp.setDataObject(altair);

        AltairAoConfigValidator val = new AltairAoConfigValidator();
        val.site  = Site.north;
        val.name  = "NGS";
        val.altair = altair;
        val.validate();
    }

    public void testAltairDefault()  throws Exception {
        testAltair( new InstAltair() );
    }

    public void testAltairNdFilter() throws Exception {
        InstAltair altair = new InstAltair();
        for(AltairParams.NdFilter ndFilter: AltairParams.NdFilter.values()) {
            altair.setNdFilter(ndFilter);
            testAltair(altair);
        }
    }

    public void testAltairWavelength() throws Exception {
        InstAltair altair = new InstAltair();
        for(AltairParams.Wavelength wavelength: AltairParams.Wavelength.values()) {
            altair.setWavelength(wavelength);
            testAltair(altair);
        }
    }

    private void testGems(Gems gems) throws Exception {
        addGsaoi();
        addGems();
        gemsObsComp.setDataObject(gems);

        GemsAoConfigValidator val = new GemsAoConfigValidator();
        val.site  = Site.south;
        val.name  = TccNames.GEMS_GAOS;
        val.gems  = gems;
        val.validate();
    }

    public void testGemsDefault() throws Exception {
        testGems(new Gems());
    }

    public void testGemsAdc() throws Exception {
        Gems gems = new Gems();
        for (Gems.Adc adc : Gems.Adc.values()) {
            gems.setAdc(adc);
            testGems(gems);
        }
    }

    public void testGemsDichroic() throws Exception {
        Gems gems = new Gems();
        for (Gems.DichroicBeamsplitter bs : Gems.DichroicBeamsplitter.values()) {
            gems.setDichroicBeamsplitter(bs);
            testGems(gems);
        }
    }

    public void testGemsAstrometric() throws Exception {
        Gems gems = new Gems();
        for (Gems.AstrometricMode am : Gems.AstrometricMode.values()) {
            gems.setAstrometricMode(am);
            testGems(gems);
        }
    }

    public void testSouthNoGems() throws Exception {
        addGsaoi();

        AoConfigValidator val = new GemsAoConfigValidator();
        val.site  = Site.south;
        val.name  = TccNames.NO_AO;
        val.validate();
    }
}