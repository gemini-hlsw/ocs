package edu.gemini.p2checker.rules;

import edu.gemini.p2checker.api.ObservationElements;
import edu.gemini.p2checker.api.Problem;
import edu.gemini.p2checker.rules.gmos.GmosRule;
import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBIDClashException;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Class GmosRuleTest. Not being executed currently. Can't figure out how to get GmosRule to actually run through all the rules.
 * Probably missing an observe.
 *
 * @author Nicolas A. Barriga
 *         Date: 4/14/11
 *
 * Fixed Gmos tests.
 *
 * @author Javier Luhrs
 *         Date: 4/2/14
 */

public class GmosRuleTest extends AbstractRuleTest {
    protected ISPObsComponent gmosComponent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        gmosComponent = addGmosNorth();
        // Set a filter to avoid warning
        setFilterNorth(GmosNorthType.FilterNorth.r_G0303);
    }

    @Test
    public void testNoErrors() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        addTargetObsCompAO();
        final ISPObsComponent altairComp = addAltair(AltairParams.Mode.NGS);
        final InstAltair altair = (InstAltair) altairComp.getDataObject();
        altair.setMode(AltairParams.Mode.NGS_FL);
        altairComp.setDataObject(altair);

        addSiteQuality();

        ObservationElements elems = new ObservationElements(obs);
        assertTrue(elems.hasAltair());

        GmosRule rules = new GmosRule();
        List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(0, problems.size());

    }

    @Test
    public void testNoAltair20() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        addTargetObsCompAO();
        setExposureTime(10.0);
        addSimpleScienceObserve();

        addSiteQuality(SPSiteQuality.ImageQuality.PERCENT_20);

        ObservationElements elems = new ObservationElements(obs);

        GmosRule rules = new GmosRule();
        List<Problem> problems = rules.check(elems).getProblems();
        assertEquals(0, problems.size());

    }

    @Test
    public void testNoAltairNo20() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        addTargetObsCompAO();
        setBin(GmosCommonType.Binning.ONE, GmosCommonType.Binning.ONE);
        setExposureTime(10.0);
        addSimpleScienceObserve();

        addSiteQuality();

        ObservationElements elems = new ObservationElements(obs);

        GmosRule rules = new GmosRule();
        List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(1, problems.size());
        Problem p0 = problems.get(0);
        assertEquals(Problem.Type.WARNING, p0.getType());
        assertTrue(p0.toString(), p0.toString().startsWith("Warning:1x1 binning is usually only necessary in IQ=20"));

    }

    @Test
    public void testAltairNo1x1() throws SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException {
        setBin(GmosCommonType.Binning.FOUR, GmosCommonType.Binning.FOUR);
        addTargetObsCompAOP1();
        addAltair(AltairParams.Mode.NGS_FL);
        addSimpleScienceObserve();
        setExposureTime(1.0);

        ObservationElements elems = new ObservationElements(obs);
        assertTrue(elems.hasAltair());

        GmosRule rules = new GmosRule();
        List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(1, problems.size());
        Problem p0 = problems.get(0);
        assertEquals(Problem.Type.WARNING, p0.getType());
        assertTrue(p0.toString(), p0.toString().startsWith("Warning:Altair observations should use 1x1 binning."));

    }

    @Test
    public void testDayCalAnyConditions() throws Exception {

        addSimpleDarkObserve(1, 1.0, 1);
        addSiteQuality(
            SPSiteQuality.ImageQuality.ANY,
            SPSiteQuality.CloudCover.ANY,
            SPSiteQuality.SkyBackground.ANY,
            SPSiteQuality.WaterVapor.ANY
        );

        final ObservationElements elems = new ObservationElements(obs);
        final GmosRule rules = new GmosRule();
        final List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(0, problems.size());
    }

    @Test
    public void testDayCalBetterConditions() throws Exception {

        addSimpleDarkObserve(1, 1.0, 1);
        addSiteQuality(
            SPSiteQuality.ImageQuality.PERCENT_20,
            SPSiteQuality.CloudCover.ANY,
            SPSiteQuality.SkyBackground.ANY,
            SPSiteQuality.WaterVapor.ANY
        );

        final ObservationElements elems = new ObservationElements(obs);
        final GmosRule rules = new GmosRule();
        final List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(1, problems.size());

        Problem p0 = problems.get(0);
        assertEquals(Problem.Type.WARNING, p0.getType());
        assertTrue(p0.toString(), p0.toString().startsWith("Warning:Daytime calibrations should not have conditions constraints."));
    }

    @Test
    public void testPartnerCalAnyConditions() throws Exception {

        setDisperserNorth(GmosNorthType.DisperserNorth.B1200_G5301);
        addSimpleArcObserve(1, 1.0, 1);
        addSiteQuality(
            SPSiteQuality.ImageQuality.ANY,
            SPSiteQuality.CloudCover.ANY,
            SPSiteQuality.SkyBackground.ANY,
            SPSiteQuality.WaterVapor.ANY
        );

        final ObservationElements elems = new ObservationElements(obs);
        final GmosRule rules = new GmosRule();
        final List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(0, problems.size());
    }

    @Test
    public void testPartnerCalBetterConditions() throws Exception {

        setDisperserNorth(GmosNorthType.DisperserNorth.B1200_G5301);
        addSimpleArcObserve(1, 1.0, 1);
        addSiteQuality(
            SPSiteQuality.ImageQuality.PERCENT_20,
            SPSiteQuality.CloudCover.ANY,
            SPSiteQuality.SkyBackground.ANY,
            SPSiteQuality.WaterVapor.ANY
        );

        final ObservationElements elems = new ObservationElements(obs);
        final GmosRule rules = new GmosRule();
        final List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(1, problems.size());

        Problem p0 = problems.get(0);
        assertEquals(Problem.Type.WARNING, p0.getType());
        assertTrue(p0.toString(), p0.toString().startsWith("Warning:GMOS baseline spectrophotometric standards should not have conditions constraints."));
    }

    private void checkZeroExp(ISPObservation observation) {
        ObservationElements elems = new ObservationElements(observation);
        GmosRule rules = new GmosRule();
        List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(1, problems.size());
        Problem p0 = problems.get(0);
        assertEquals(Problem.Type.ERROR, p0.getType());
        assertTrue(p0.toString(), p0.toString().startsWith("Error:Exposure time must be greater than 0"));
    }

    @Test
    public void testZeroExp() throws SPException, DBIDClashException, SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException, SPBadIDException {
        // Check with science observe
        addSimpleScienceObserve();

        setExposureTime(0.0);

        checkZeroExp(obs);

        // Check with dark
        obs.getSeqComponent().setSeqComponents(new ArrayList<ISPSeqComponent>());
        addSimpleDarkObserve(1, 0.0, 1);

        addSiteQuality(
            SPSiteQuality.ImageQuality.ANY,
            SPSiteQuality.CloudCover.ANY,
            SPSiteQuality.SkyBackground.ANY,
            SPSiteQuality.WaterVapor.ANY
        );
        checkZeroExp(obs);

        // Check with flat
        obs.getSeqComponent().setSeqComponents(new ArrayList<ISPSeqComponent>());
        addSimpleFlatObserve(1, 0.0, 1);

        checkZeroExp(obs);
    }

    @Test
    public void testNoIntegerExp() throws SPException, DBIDClashException, SPUnknownIDException, SPTreeStateException, SPNodeNotLocalException, SPBadIDException {
        // Check with science observe
        addSimpleScienceObserve();

        setExposureTime(5.5);

        ObservationElements elems = new ObservationElements(obs);
        GmosRule rules = new GmosRule();
        List<Problem> problems = rules.check(elems).getProblems();

        assertEquals(1, problems.size());
        Problem p0 = problems.get(0);
        assertEquals(Problem.Type.ERROR, p0.getType());
        assertTrue(p0.toString(), p0.toString().startsWith("Error:The GMOS DC does not support fractional exposure times"));

    }

    private void setFilterNorth(GmosNorthType.FilterNorth filter) {
        InstGmosNorth gmosDataObject = (InstGmosNorth) gmosComponent.getDataObject();
        gmosDataObject.setFilterNorth(filter);
        gmosComponent.setDataObject(gmosDataObject);
    }

    private void setDisperserNorth(GmosNorthType.DisperserNorth disperser) {
        InstGmosNorth gmosDataObject = (InstGmosNorth) gmosComponent.getDataObject();
        gmosDataObject.setDisperserNorth(disperser);
        gmosComponent.setDataObject(gmosDataObject);
    }

    private void setExposureTime(double exposureTime) {
        InstGmosNorth gmosDataObject = (InstGmosNorth) gmosComponent.getDataObject();
        gmosDataObject.setExposureTime(exposureTime);
        gmosComponent.setDataObject(gmosDataObject);
    }

    private void setBin(GmosCommonType.Binning xBin, GmosCommonType.Binning yBin) {
        InstGmosNorth gmosDataObject = (InstGmosNorth) gmosComponent.getDataObject();
        gmosDataObject.setCcdXBinning(xBin);
        gmosDataObject.setCcdYBinning(yBin);
        gmosComponent.setDataObject(gmosDataObject);
    }

    private boolean hasProblem(Problem.Type t, String id, List<Problem> problems) {
        for (Problem p : problems) {
            if (p.getId().equals(id) && p.getType() == t) return true;
        }
        return false;
    }

    @Test
    public void testIfuNoSpatialBinning() throws Exception {
        final GmosRule gmos = new GmosRule();
        final String id     = "GmosRule_IFU_NO_SPATIAL_BINNING_RULE";

        addSimpleScienceObserve();
        final InstGmosNorth gmosDataObject = (InstGmosNorth) gmosComponent.getDataObject();

        for (GmosNorthType.FPUnitNorth fpu : GmosNorthType.FPUnitNorth.values()) {
            for (GmosNorthType.DisperserNorth d : GmosNorthType.DisperserNorth.values()) {
                for (GmosCommonType.Binning b : GmosCommonType.Binning.values()) {
                    gmosDataObject.setFPUnitNorth(fpu);
                    gmosDataObject.setDisperserNorth(d);
                    gmosDataObject.setCcdYBinning(b);
                    gmosComponent.setDataObject(gmosDataObject);

                    final String msg = String.format("id=%s: fpu=%s, disperser=%s, ybin=%s", id, fpu.displayValue(), d.displayValue(), b.displayValue());
                    final boolean shouldWarn = fpu.isIFU() && !d.isMirror() && b != GmosCommonType.Binning.ONE;
                    final ObservationElements elems = new ObservationElements(obs);
                    assertEquals(msg, shouldWarn, hasProblem(Problem.Type.WARNING, id, gmos.check(elems).getProblems()));
                }
            }
        }
    }
}
