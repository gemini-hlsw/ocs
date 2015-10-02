//
// $
//

package edu.gemini.spModel.gemini.flamingos2;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test cases for Flamingos2 setup time calculations.
 *
 * <p>HLPG_PROJECT_BASE property must be set.
 */
public final class PlannedTimeTest {

    private IDBDatabaseService odb;
    private ISPProgram prog;
    private ISPObservation obs;
    private ISPObsComponent f2ObsComponent;
    private Flamingos2 f2DataObject;
    private ISPSeqComponent f2SeqComponent;
    private SeqConfigFlamingos2 f2SeqDataObject;

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Before
    public void setUp() throws Exception {
        odb = DBLocalDatabase.createTransient();

        SPProgramID progId = SPProgramID.toProgramID("GS-2009B-Q-1");
        prog = odb.getFactory().createProgram(new SPNodeKey(), progId);
        odb.put(prog);

        obs = odb.getFactory().createObservation(prog, null);

        List<ISPObsComponent> emptyObsComponents = Collections.emptyList();
        obs.setObsComponents(emptyObsComponents);
        prog.addObservation(obs);

        f2ObsComponent = odb.getFactory().createObsComponent(prog, Flamingos2.SP_TYPE, null);
        obs.addObsComponent(f2ObsComponent);
        f2DataObject = (Flamingos2) f2ObsComponent.getDataObject();

        f2SeqComponent = odb.getFactory().createSeqComponent(prog, SeqConfigFlamingos2.SP_TYPE, null);
        obs.getSeqComponent().addSeqComponent(f2SeqComponent);
        f2SeqDataObject = (SeqConfigFlamingos2) f2SeqComponent.getDataObject();

        ISPSeqComponent observeSeqComponent;
        observeSeqComponent = odb.getFactory().createSeqComponent(prog, SeqRepeatObserve.SP_TYPE, null);
        f2SeqComponent.addSeqComponent(observeSeqComponent);
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
        assertEquals(expectedTime, getExecSeconds(time), 0.0001);
    }

    @Test
    public void testEmptySequence() throws Exception {
        double readout = f2DataObject.getReadMode().readoutTimeSec() + PlannedTime.Category.DHS_OVERHEAD.time/1000.;

        // Imaging setup time.
        verify(Flamingos2.getImagingSetupSec(obs) + readout);

        // Spectroscopy setup time.
        f2DataObject.setFpu(Flamingos2.FPUnit.LONGSLIT_1);
        f2ObsComponent.setDataObject(f2DataObject);

        verify(Flamingos2.getSpectroscopySetupSec() + readout);
    }

    @Test
    public void testImagingSetup() throws Exception {
        // Defaulting to PWFS2
        double readout = f2DataObject.getReadMode().readoutTimeSec() + PlannedTime.Category.DHS_OVERHEAD.time/1000.;

        // Imaging setup time.
        verify(Flamingos2.getImagingSetupSec(obs) + readout);

        // With an explicit PWFS2 guider.
        final ISPObsComponent targetComp = odb.getFactory().createObsComponent(prog, SPComponentType.TELESCOPE_TARGETENV, null);
        final TargetObsComp targetDo = (TargetObsComp) targetComp.getDataObject();
        final TargetEnvironment tenv = targetDo.getTargetEnvironment();
        obs.addObsComponent(targetComp);

        final SPTarget pwfs2Target = new SPTarget();
        final GuideProbeTargets pwfs2 = GuideProbeTargets.create(PwfsGuideProbe.pwfs2, new SPTarget()).selectPrimary(pwfs2Target);
        targetDo.setTargetEnvironment(tenv.putPrimaryGuideProbeTargets(pwfs2));
        targetComp.setDataObject(targetDo);

        assertEquals(360.0, Flamingos2.getImagingSetupSec(obs), 0.000001);
        verify(Flamingos2.getImagingSetupSec(obs) + readout);

        // With OIWFS
        final SPTarget oiwfsTarget = new SPTarget();
        final GuideProbeTargets oiwfs = GuideProbeTargets.create(Flamingos2OiwfsGuideProbe.instance, oiwfsTarget).selectPrimary(oiwfsTarget);
        targetDo.setTargetEnvironment(tenv.putPrimaryGuideProbeTargets(oiwfs));
        targetComp.setDataObject(targetDo);

        assertEquals(900.0, Flamingos2.getImagingSetupSec(obs), 0.000001);
        verify(Flamingos2.getImagingSetupSec(obs) + readout);
    }

    private IParameter getExpTimeParam(Double... secs) {
        String propName = Flamingos2.EXPOSURE_TIME_PROP.getName();
        List<Double> vals = Arrays.asList(secs);
        return DefaultParameter.getInstance(propName, vals);
    }

    private IParameter getFpuParam(Flamingos2.FPUnit... fpus) {
        String propName = Flamingos2.FPU_PROP.getName();
        List<Flamingos2.FPUnit> vals = Arrays.asList(fpus);
        return DefaultParameter.getInstance(propName, vals);
    }

    private IParameter getFilterParam(Flamingos2.Filter... filters) {
        String propName = Flamingos2.FILTER_PROP.getName();
        List<Flamingos2.Filter> vals = Arrays.asList(filters);
        return DefaultParameter.getInstance(propName, vals);
    }

    private IParameter getDisperserParam(Flamingos2.Disperser... disp) {
        String propName = Flamingos2.DISPERSER_PROP.getName();
        List<Flamingos2.Disperser> vals = Arrays.asList(disp);
        return DefaultParameter.getInstance(propName, vals);
    }

    @Test
    public void testReadoutTime() throws Exception {
        // Add a single step to the sequence.
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(getExpTimeParam(60.0));
        f2SeqDataObject.setSysConfig(sc);
        f2SeqComponent.setDataObject(f2SeqDataObject);

        double base = Flamingos2.getImagingSetupSec(obs) + 60 + PlannedTime.Category.DHS_OVERHEAD.time/1000.;

        for (Flamingos2.ReadMode mode : Flamingos2.ReadMode.values()) {
            Flamingos2 obj = (Flamingos2) f2ObsComponent.getDataObject();
            obj.setReadMode(mode);
            f2ObsComponent.setDataObject(obj);
            verify(base + mode.readoutTimeSec());
        }
    }

    private void testSingleParamChange(IParameter param, double overhead) throws Exception {
        f2DataObject.setExposureTime(60.0);
        f2ObsComponent.setDataObject(f2DataObject);

        // Add two steps in which the value changes
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(param);
        f2SeqDataObject.setSysConfig(sc);
        f2SeqComponent.setDataObject(f2SeqDataObject);

        // Account for setup, exposure time, and readout time for the 2 steps
        double time = Flamingos2.getSpectroscopySetupSec() + 60.0 * 2;
        time += f2DataObject.getReadMode().readoutTimeSec() * 2;
        time += PlannedTime.Category.DHS_OVERHEAD.time/1000. * 2;

        // Account for the change
        verify(time + overhead);
    }

    @Test
    public void testFpuChangeOverhead() throws Exception {
        // Setup the obs component so that we get spectroscopy setup time.
        f2DataObject.setFpu(Flamingos2.FPUnit.LONGSLIT_1);
        IParameter param = getFpuParam(Flamingos2.FPUnit.LONGSLIT_1, Flamingos2.FPUnit.LONGSLIT_2);
        testSingleParamChange(param, Flamingos2.getFpuChangeOverheadSec());
    }

    @Test
    public void testFilterChangeOverhead() throws Exception {
        // Setup the obs component so that we get spectroscopy setup time.
        f2DataObject.setFpu(Flamingos2.FPUnit.LONGSLIT_1);
        f2DataObject.setFilter(Flamingos2.Filter.J);
        IParameter param = getFilterParam(Flamingos2.Filter.J, Flamingos2.Filter.J_LOW);
        testSingleParamChange(param, Flamingos2.getFilterChangeOverheadSec());
    }

    @Test
    public void testDisperserChangeOverhead() throws Exception {
        // Setup the obs component so that we get spectroscopy setup time.
        f2DataObject.setDisperser(Flamingos2.Disperser.R1200HK);
        IParameter param = getDisperserParam(Flamingos2.Disperser.R1200HK, Flamingos2.Disperser.R1200JH);
        testSingleParamChange(param, Flamingos2.getDisperserChangeOverheadSec());
    }

    @Test
    public void testMaxChangeOverhead() throws Exception {
        // Setup the instrument component.
        f2DataObject.setFpu(Flamingos2.FPUnit.LONGSLIT_1);
        f2DataObject.setFilter(Flamingos2.Filter.J);
        f2DataObject.setDisperser(Flamingos2.Disperser.R1200HK);
        f2DataObject.setExposureTime(60.0);
        f2ObsComponent.setDataObject(f2DataObject);

        // Setup the Sequence with 2 steps in which filter, fpu, and disperser
        // change.
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(getFpuParam(Flamingos2.FPUnit.LONGSLIT_1, Flamingos2.FPUnit.LONGSLIT_2));
        sc.putParameter(getFilterParam(Flamingos2.Filter.J, Flamingos2.Filter.J_LOW));
        sc.putParameter(getDisperserParam(Flamingos2.Disperser.R1200HK, Flamingos2.Disperser.R1200JH));
        f2SeqDataObject.setSysConfig(sc);
        f2SeqComponent.setDataObject(f2SeqDataObject);

        // Account for setup, exposure time, and readout time for the 2 steps.
        double time = Flamingos2.getSpectroscopySetupSec() + 60.0 * 2;
        time += f2DataObject.getReadMode().readoutTimeSec() * 2;
        time += PlannedTime.Category.DHS_OVERHEAD.time/1000. * 2;

        // Figure out the overhead for the change.
        double over = Flamingos2.getFpuChangeOverheadSec();
        over = Math.max(over, Flamingos2.getFilterChangeOverheadSec());
        over = Math.max(over, Flamingos2.getDisperserChangeOverheadSec());
        verify(time + over);
    }

    // Test a sequence in which there is a change followed by another step with
    // no change.
    @Test
    public void testMixed() throws Exception {
        // Setup the instrument component.
        f2DataObject.setFpu(Flamingos2.FPUnit.LONGSLIT_1);
        f2DataObject.setFilter(Flamingos2.Filter.J);
        f2DataObject.setExposureTime(60.0);
        f2ObsComponent.setDataObject(f2DataObject);

        // Setup the Sequence with 2 steps in which filter, fpu, and disperser
        // change.
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(getFilterParam(Flamingos2.Filter.J, Flamingos2.Filter.J_LOW, Flamingos2.Filter.J_LOW));
        f2SeqDataObject.setSysConfig(sc);
        f2SeqComponent.setDataObject(f2SeqDataObject);

        // Account for setup, exposure time, and readout time for the 3 steps.
        double time = Flamingos2.getSpectroscopySetupSec() + 60.0 * 3;
        time += f2DataObject.getReadMode().readoutTimeSec() * 3;
        time += PlannedTime.Category.DHS_OVERHEAD.time/1000. * 3;

        // Figure out the overhead for the single filter change.
        double over = Flamingos2.getFilterChangeOverheadSec();
        verify(time + over);
    }
}
