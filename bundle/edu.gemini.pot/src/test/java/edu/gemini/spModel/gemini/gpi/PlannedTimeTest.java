package edu.gemini.spModel.gemini.gpi;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatFlatObs;
import edu.gemini.spModel.obs.plannedtime.GcalStepCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test cases for Gpi setup time calculations.
 */
public final class PlannedTimeTest {
    private static final double EXP_TIME = Gpi.DEFAULT_EXPOSURE_TIME;

    private IDBDatabaseService odb;
    private ISPProgram prog;

    private ISPObservation obs;
    private ISPObsComponent gpiObsComponent;
    private Gpi gpiDataObject;
    private ISPSeqComponent gpiSeqComponent;
    private SeqConfigGpi gpiSeqDataObject;


    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Before
    public void setUp() throws Exception {
        odb = DBLocalDatabase.createTransient();
        SPProgramID progId = SPProgramID.toProgramID("GS-2009B-Q-1");
        prog = odb.getFactory().createProgram(new SPNodeKey(), progId);
        odb.put(prog);

        obs = odb.getFactory().createObservation(prog, Instrument.none, null);

        List<ISPObsComponent> emptyObsComponents = Collections.emptyList();
        obs.setObsComponents(emptyObsComponents);
        prog.addObservation(obs);

        gpiObsComponent = odb.getFactory().createObsComponent(prog, Gpi.SP_TYPE, null);
        obs.addObsComponent(gpiObsComponent);
        gpiDataObject = (Gpi) gpiObsComponent.getDataObject();

        gpiSeqComponent = odb.getFactory().createSeqComponent(prog, SeqConfigGpi.SP_TYPE, null);
        obs.getSeqComponent().addSeqComponent(gpiSeqComponent);
        gpiSeqDataObject = (SeqConfigGpi) gpiSeqComponent.getDataObject();

        ISPSeqComponent obsSeqComponent = odb.getFactory().createSeqComponent(prog, SeqRepeatObserve.SP_TYPE, null);
        gpiSeqComponent.addSeqComponent(obsSeqComponent);
    }

    @After
    public void tearDown() throws Exception {
        odb.getDBAdmin().shutdown();
    }

    private double getExecSeconds(PlannedTimeSummary pt) {
        return pt.getExecTime() / 1000.0;
    }

    private void verify(double expectedTime) throws Exception {
        PlannedTimeSummary time = PlannedTimeSummaryService.getTotalTime(obs);
        assertEquals(expectedTime, getExecSeconds(time), 0.00001);
    }

    @Test
    public void testEmptySequence() throws Exception {
        // Imaging setup time + a 1 sec exposure
        verify(Gpi.getImagingSetup().getSeconds() +
                Gpi.READOUT_PER_EXPOSURE_MS/1000.0 +
                Gpi.READOUT_OVERHEAD_SEC +
                gpiDataObject.getWriteTime().timeSeconds());
    }

    /*
    private IParameter getExpTimeParam(Double... secs) {
        String propName = Gpi.EXPOSURE_TIME_PROP.getName();
        List<Double> vals = Arrays.asList(secs);
        return DefaultParameter.getInstance(propName, vals);
    }*/

    private IParameter getHalfWavePlateAngleParam(Double... angles) {
        String propName = Gpi.HALF_WAVE_PLATE_ANGLE_VALUE_PROP.getName();
        List<Double> vals = Arrays.asList(angles);
        return DefaultParameter.getInstance(propName, vals);
    }

    private IParameter getFilterParam(Gpi.Filter... filters) {
        String propName = Gpi.FILTER_PROP.getName();
        List<Gpi.Filter> vals = Arrays.asList(filters);
        return DefaultParameter.getInstance(propName, vals);
    }

    private IParameter getDisperserParam(Gpi.Disperser... disp) {
        String propName = Gpi.DISPERSER_PROP.getName();
        List<Gpi.Disperser> vals = Arrays.asList(disp);
        return DefaultParameter.getInstance(propName, vals);
    }

    // XXX TODO
//    public void testReadoutTime() throws Exception {
//        // Add a single step to the sequence.
//        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
//        sc.putParameter(getExpTimeParam(EXP_TIME));
//        gpiSeqDataObject.setSysConfig(sc);
//        gpiSeqComponent.setDataObject(gpiSeqDataObject);
//
//        double base = Gpi.getImagingSetupSec() + 60;
//
//        for (Gpi.ReadMode mode : Gpi.ReadMode.values()) {
//            gpiDataObject.setReadMode(mode);
//            gpiObsComponent.setDataObject(gpiDataObject);
//            verify(base + mode.readoutTimeSec());
//        }
//    }

    private void testSingleParamChange(IParameter param, double overhead) throws Exception {
        gpiDataObject.setExposureTime(EXP_TIME);
        gpiObsComponent.setDataObject(gpiDataObject);

        // Add two steps in which the value changes
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(param);
        gpiSeqDataObject.setSysConfig(sc);
        gpiSeqComponent.setDataObject(gpiSeqDataObject);

        // Account for setup, exposure time, and readout time for the 2 steps
        double time = Gpi.getImagingSetup().getSeconds();
        time += (EXP_TIME + Gpi.READOUT_PER_EXPOSURE_MS/1000.0) * 2;
        time += Gpi.READOUT_OVERHEAD_SEC * 2;
        time += gpiDataObject.getWriteTime().timeSeconds() * 2;

        // Account for the change
        verify(time + overhead);
    }

    @Test
    public void testFilterChangeOverhead() throws Exception {
        gpiDataObject.setFilter(Gpi.Filter.J);
        IParameter param = getFilterParam(Gpi.Filter.J, Gpi.Filter.K1);
        testSingleParamChange(param, Gpi.SINGLE_CHANGE_OVERHEAD_SECS);
    }

    @Test
    public void testDisperserChangeOverhead() throws Exception {
        // Setup the obs component so that we get spectroscopy setup time.
        gpiDataObject.setDisperser(Gpi.Disperser.PRISM);
        IParameter param = getDisperserParam(Gpi.Disperser.PRISM, Gpi.Disperser.WOLLASTON);
        testSingleParamChange(param, Gpi.SINGLE_CHANGE_OVERHEAD_SECS);
    }

    @Test
    public void testMultiChangeOverhead() throws Exception {
        // Setup the instrument component.
        gpiDataObject.setFilter(Gpi.Filter.J);
        gpiDataObject.setDisperser(Gpi.Disperser.PRISM);
        gpiDataObject.setExposureTime(EXP_TIME);
        gpiObsComponent.setDataObject(gpiDataObject);

        // Setup the Sequence with 2 steps in which filter, fpu, and disperser
        // change.
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(getFilterParam(Gpi.Filter.J, Gpi.Filter.K1));
        sc.putParameter(getDisperserParam(Gpi.Disperser.PRISM, Gpi.Disperser.WOLLASTON));
        gpiSeqDataObject.setSysConfig(sc);
        gpiSeqComponent.setDataObject(gpiSeqDataObject);

        // Account for setup, exposure time, and readout time for the 2 steps.
        double time = Gpi.getImagingSetup().getSeconds();

        time += (Gpi.READOUT_PER_EXPOSURE_MS/1000.0 + EXP_TIME) * 2;
        time += Gpi.READOUT_OVERHEAD_SEC * 2;
        time += gpiDataObject.getWriteTime().timeSeconds() * 2;

        double over = Gpi.MULTI_CHANGE_OVERHEAD_SECS;
        verify(time + over);
    }

    @Test
    public void testHalfWavePlateChangeOverhead() throws Exception {
        // Setup the instrument component.
        gpiDataObject.setExposureTime(EXP_TIME);
        gpiDataObject.setHalfWavePlateAngle(0);
        gpiObsComponent.setDataObject(gpiDataObject);

        // Setup the Sequence with 3 steps in which the halfwave plate angle changes
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(getHalfWavePlateAngleParam(45.0, 60.0, 90.0));
        gpiSeqDataObject.setSysConfig(sc);
        gpiSeqComponent.setDataObject(gpiSeqDataObject);

        // Account for setup, exposure time, and readout time for the 3 steps.
        double time = Gpi.getImagingSetup().getSeconds();

        time += (Gpi.READOUT_PER_EXPOSURE_MS/1000.0 + EXP_TIME) * 3;
        time += Gpi.READOUT_OVERHEAD_SEC * 3;
        time += gpiDataObject.getWriteTime().timeSeconds() * 3;

        double overhead = 2 * Gpi.HALFWAVE_PLATE_CHANGE_OVERHEAD_SECS;
        verify(time + overhead);
    }

    @Test
    public void testHalfWavePlateAndComponentChangeOverhead() throws Exception {
        // Setup the instrument component.
        gpiDataObject.setExposureTime(EXP_TIME);
        gpiDataObject.setHalfWavePlateAngle(0);
        gpiObsComponent.setDataObject(gpiDataObject);

        // Setup the Sequence with 2 steps in which the halfwave plate angle changes and a filter changes
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(getFilterParam(Gpi.Filter.J, Gpi.Filter.K1));
        sc.putParameter(getHalfWavePlateAngleParam(45.0, 60.0));
        gpiSeqDataObject.setSysConfig(sc);
        gpiSeqComponent.setDataObject(gpiSeqDataObject);

        // Account for setup, exposure time, and readout time for both steps
        double time = Gpi.getImagingSetup().getSeconds();

        time += (Gpi.READOUT_PER_EXPOSURE_MS/1000.0 + EXP_TIME) * 2;
        time += Gpi.READOUT_OVERHEAD_SEC * 2;
        time += gpiDataObject.getWriteTime().timeSeconds() * 2;

        // The filter change dominates the half wave plate angle change
        double overhead = Gpi.SINGLE_CHANGE_OVERHEAD_SECS;
        verify(time + overhead);
    }

    // Test a sequence in which there is a change followed by another step with
    // no change.
    @Test
    public void testMixed() throws Exception {
        // Setup the instrument component.
        gpiDataObject.setFilter(Gpi.Filter.J);
        gpiDataObject.setExposureTime(EXP_TIME);
        gpiObsComponent.setDataObject(gpiDataObject);

        // Setup the Sequence with 2 steps in which filter, fpu, and disperser
        // change.
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(getFilterParam(Gpi.Filter.J, Gpi.Filter.K2, Gpi.Filter.K2));
        gpiSeqDataObject.setSysConfig(sc);
        gpiSeqComponent.setDataObject(gpiSeqDataObject);

        // Account for setup, exposure time, and readout time for the 3 steps.
        double time = Gpi.getImagingSetup().getSeconds();

        time += (Gpi.READOUT_PER_EXPOSURE_MS/1000.0 + EXP_TIME) * 3;
        time += Gpi.READOUT_OVERHEAD_SEC * 3;
        time += gpiDataObject.getWriteTime().timeSeconds() * 3;

        double over = Gpi.SINGLE_CHANGE_OVERHEAD_SECS;
        verify(time + over);
    }

    @Test
    public void testCoadds() throws Exception {
        gpiDataObject.setExposureTime(EXP_TIME);
        gpiObsComponent.setDataObject(gpiDataObject);

        // Add a step to the sequence.
        final IParameter param = getFilterParam(Gpi.Filter.J);
        final ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(param);
        gpiSeqDataObject.setSysConfig(sc);
        gpiSeqComponent.setDataObject(gpiSeqDataObject);

        // Remove the observe and replace it with a manual flat
        final ISPSeqComponent flatComp = odb.getFactory().createSeqComponent(prog, SeqRepeatFlatObs.SP_TYPE, null);
        final SeqRepeatFlatObs flatDataObject = (SeqRepeatFlatObs) flatComp.getDataObject();
        flatDataObject.setExposureTime(2.0);
        flatDataObject.setCoaddsCount(5);
        flatComp.setDataObject(flatDataObject);

        final List<ISPSeqComponent> children = new ArrayList<>();
        children.add(flatComp);
        gpiSeqComponent.setSeqComponents(children);

        // Account for setup, exposure time, and readout time for the 2 steps
        double time = Gpi.getImagingSetup().getSeconds();
        time += (2.0 + Gpi.READOUT_PER_EXPOSURE_MS/1000.0) * 5;
        time += Gpi.READOUT_OVERHEAD_SEC;
        time += gpiDataObject.getWriteTime().timeSeconds();
        time += GcalStepCalculator.SCIENCE_FOLD_MOVE_TIME/1000.0;

        verify(time);

    }
}
