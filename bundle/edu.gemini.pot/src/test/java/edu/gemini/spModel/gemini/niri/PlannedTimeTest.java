//
// $
//

package edu.gemini.spModel.gemini.niri;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import edu.gemini.spModel.gemini.niri.InstNIRI.Mode;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.gemini.altair.AltairParams;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.beans.PropertyDescriptor;

/**
 * Test cases for NIRI planned time calculations.
 */
public final class PlannedTimeTest {

    private IDBDatabaseService odb;
    private ISPObservation obs;

    private ISPObsComponent niriObsComponent;
    private InstNIRI niriDataObject;

    private ISPObsComponent altairObsComponent;
    private InstAltair altairDataObject;

    private ISPSeqComponent niriSeqComponent;
    private SeqConfigNIRI niriSeqDataObject;


    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Before
    public void setUp() throws Exception {
        odb = DBLocalDatabase.createTransient();

        SPProgramID progId = SPProgramID.toProgramID("GS-2009B-Q-1");
        ISPProgram prog = odb.getFactory().createProgram(new SPNodeKey(), progId);
        odb.put(prog);

        obs = odb.getFactory().createObservation(prog, Instrument.none, null);

        List<ISPObsComponent> emptyObsComponents = Collections.emptyList();
        obs.setObsComponents(emptyObsComponents);
        prog.addObservation(obs);

        niriObsComponent = odb.getFactory().createObsComponent(prog, InstNIRI.SP_TYPE, null);
        obs.addObsComponent(niriObsComponent);
        niriDataObject = (InstNIRI) niriObsComponent.getDataObject();

        altairObsComponent = odb.getFactory().createObsComponent(prog, InstAltair.SP_TYPE, null);
        // don't add it yet, will need to test w/ and w/o altair
//        obs.addObsComponent(niriObsComponent);
        altairDataObject = (InstAltair) altairObsComponent.getDataObject();

        niriSeqComponent = odb.getFactory().createSeqComponent(prog, SeqConfigNIRI.SP_TYPE, null);
        obs.getSeqComponent().addSeqComponent(niriSeqComponent);
        niriSeqDataObject = (SeqConfigNIRI) niriSeqComponent.getDataObject();

        ISPSeqComponent observeSeqComponent;
        observeSeqComponent = odb.getFactory().createSeqComponent(prog, SeqRepeatObserve.SP_TYPE, null);
        niriSeqComponent.addSeqComponent(observeSeqComponent);
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
        String expectedStr = String.format("%1.6f", expectedTime);
        String actualStr   = String.format("%1.6f", getExecSeconds(time));
        assertEquals(expectedStr, actualStr);
    }

    private void setMode(Mode mode) throws Exception {
        switch (mode) {
            case imaging:
                niriDataObject.setDisperser(Niri.Disperser.NONE);
                break;
            case spectroscopy:
                niriDataObject.setDisperser(Niri.Disperser.H);
                break;
        }
        niriObsComponent.setDataObject(niriDataObject);
    }

    private void setMode(AltairParams.Mode mode) throws Exception {
        altairDataObject.setMode(mode);
        altairObsComponent.setDataObject(altairDataObject);
        if (!obs.getObsComponents().contains(altairObsComponent)) {
            obs.addObsComponent(altairObsComponent);
        }
    }

    private void cycleSetups(double dhs, double execTime) throws Exception {
        for (Mode mode : Mode.values()) {
            setMode(mode);
            // test mode + all possible altair modes
            for (AltairParams.Mode altairMode : AltairParams.Mode.values()) {
                setMode(altairMode);
                verify(dhs + execTime + InstNIRI.getSetupTime(mode, altairMode).getSeconds());
            }
            // test mode without altair component
            obs.removeObsComponent(altairObsComponent);
            verify(dhs + execTime + InstNIRI.getSetupTime(mode).getSeconds());
        }
    }

    @Test
    public void testEmptySequence() throws Exception {
        final NiriReadoutTime nrt = NiriReadoutTime.lookup(Niri.BuiltinROI.DEFAULT, Niri.ReadMode.DEFAULT).getValue();
        cycleSetups(NiriReadoutTime.getDhsWriteTime(), nrt.getReadout(1));
    }

    private <T> IParameter getParam(PropertyDescriptor desc, T... vals) {
        List<T> valList = Arrays.asList(vals);
        return DefaultParameter.getInstance(desc.getName(), valList);
    }

    @Test
    public void testReadoutTime() throws Exception {
        // Add a single step to the sequence.
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(getParam(InstNIRI.EXPOSURE_TIME_PROP, 60.0));
        niriSeqDataObject.setSysConfig(sc);
        niriSeqComponent.setDataObject(niriSeqDataObject);

        for (Niri.ReadMode mode : Niri.ReadMode.values()) {
            niriDataObject.setReadMode(mode);
            for (Niri.BuiltinROI roi : Niri.BuiltinROI.values()) {
                niriDataObject.setBuiltinROI(roi);
                niriObsComponent.setDataObject(niriDataObject);
                System.out.println(mode.toString() + ", " + roi.toString());

                final NiriReadoutTime nrt = NiriReadoutTime.lookup(roi, mode).getValue();
                cycleSetups(NiriReadoutTime.getDhsWriteTime(), 60 + nrt.getReadout(1));
            }
        }
    }

    @Test
    public void testFilterChange() throws Exception {
        // Add a single step to the sequence.
        ISysConfig sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sc.putParameter(getParam(InstNIRI.EXPOSURE_TIME_PROP, 60.0, 60.0));
        sc.putParameter(getParam(InstNIRI.FILTER_PROP, Niri.Filter.BBF_Y, Niri.Filter.BBF_J));
        niriSeqDataObject.setSysConfig(sc);
        niriSeqComponent.setDataObject(niriSeqDataObject);

        setMode(Mode.imaging);
        obs.removeObsComponent(altairObsComponent);

        Niri.ReadMode mode = Niri.ReadMode.IMAG_1TO25;
        Niri.BuiltinROI roi = Niri.BuiltinROI.CENTRAL_768;
        niriDataObject.setReadMode(mode);
        niriDataObject.setBuiltinROI(roi);
        niriObsComponent.setDataObject(niriDataObject);

        final NiriReadoutTime nrt = NiriReadoutTime.lookup(roi, mode).getValue();
        double setup   = InstNIRI.getSetupTime(Mode.imaging).getSeconds();
        double readout = 2 * (60 + nrt.getReadout(1));
        double dhs     = 2 * NiriReadoutTime.getDhsWriteTime();

        verify(setup + readout + InstNIRI.getFilterChangeOverheadSec() + dhs);
    }
}
