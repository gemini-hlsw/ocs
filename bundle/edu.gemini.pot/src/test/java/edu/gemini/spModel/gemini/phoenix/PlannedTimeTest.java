//
// $
//

package edu.gemini.spModel.gemini.phoenix;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import edu.gemini.spModel.test.SpModelTestBase;

/**
 *
 */
public final class PlannedTimeTest extends SpModelTestBase {

    private ISPObsComponent instObsComponent;
    private InstPhoenix instDataObject;

    private ISPSeqComponent observeSeqComponent;
    private SeqRepeatObserve observeDataObject;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        instObsComponent = addObsComponent(InstPhoenix.SP_TYPE);
        instDataObject   = (InstPhoenix) instObsComponent.getDataObject();

        observeSeqComponent = addSeqComponent(getObs().getSeqComponent(), SeqRepeatObserve.SP_TYPE);
        observeDataObject   = (SeqRepeatObserve) observeSeqComponent.getDataObject();
    }

    private void verify(double expectedTime) throws Exception {
        PlannedTimeSummary time = PlannedTimeSummaryService.getTotalTime(getObs());
        assertEquals(expectedTime, time.getExecTime()/1000.0);
    }

    public void testSingleObserve() throws Exception {
        instDataObject.setExposureTime(42.0);
        instDataObject.setCoadds(3);
        instObsComponent.setDataObject(instDataObject);

        double setup   = instDataObject.getSetupTime(getObs()).getSeconds();
        double expTime = 42.0 * 3 + InstPhoenix.READOUT_OVERHEAD;

        verify(setup + expTime);
    }

    public void testMultiObserve() throws Exception {
        instDataObject.setExposureTime(42.0);
        instDataObject.setCoadds(3);
        instObsComponent.setDataObject(instDataObject);

        observeDataObject.setStepCount(2);
        observeSeqComponent.setDataObject(observeDataObject);

        double setup   = instDataObject.getSetupTime(getObs()).getSeconds();
        double expTime = 42.0 * 3 + InstPhoenix.READOUT_OVERHEAD;

        verify(setup + expTime * 2);
    }
}
