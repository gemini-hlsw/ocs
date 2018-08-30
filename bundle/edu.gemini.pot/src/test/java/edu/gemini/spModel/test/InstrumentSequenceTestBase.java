//
// $
//

package edu.gemini.spModel.test;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqConfigObsBase;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import org.junit.Before;

import java.util.Arrays;
import java.util.List;

/**
 * Base class for test cases that need to work with a simple instrument
 * sequence with a nested observe.
 */
public abstract class InstrumentSequenceTestBase<I extends SPInstObsComp, S extends SeqConfigObsBase> extends SpModelTestBase {

    private ISPObsComponent instObsComponent;
    private I instDataObject;

    private ISPSeqComponent instSeqComponent;
    private S instSeqDataObject;

    private ISPSeqComponent observeSeqComponent;
    private SeqRepeatObserve observeSeqDataObject;

    /**
     * Creates the instrument "static" component and instrument sequence
     * iterator, after calling the superclass setUp() to create the program
     * and database.
     */
    @SuppressWarnings({"unchecked"})
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        instObsComponent = addObsComponent(getObsCompSpType());
        instDataObject = (I) instObsComponent.getDataObject();

        instSeqComponent = addSeqComponent(getObs().getSeqComponent(), getSeqCompSpType());
        instSeqDataObject = (S) instSeqComponent.getDataObject();

        observeSeqComponent = addSeqComponent(instSeqComponent, SeqRepeatObserve.SP_TYPE);
        observeSeqDataObject = (SeqRepeatObserve) observeSeqComponent.getDataObject();
    }

    /**
     * Gets the component type of the instrument static component.  For example
     * {@link edu.gemini.spModel.gemini.flamingos2.Flamingos2#SP_TYPE}.
     */
    protected abstract SPComponentType getObsCompSpType();

    /**
     * Gets the component type of the instrument sequence component.  For
     * example
     * {@link edu.gemini.spModel.gemini.flamingos2.SeqConfigFlamingos2#SP_TYPE}.
     */
    protected abstract SPComponentType getSeqCompSpType();

    protected ISPObsComponent getInstObsComp() {
        return instObsComponent;
    }

    protected I getInstDataObj() {
        return instDataObject;
    }

    protected ISPSeqComponent getInstSeqComp() {
        return instSeqComponent;
    }

    protected S getInstSeqDataObject() {
        return instSeqDataObject;
    }

    protected ISPSeqComponent getObserveSeqComp() {
        return observeSeqComponent;
    }

    protected SeqRepeatObserve getObserveSeqDataObject() {
        return observeSeqDataObject;
    }

    /**
     * Executed the PlannedTimeService for the observation and compares the
     * results to the given expected time.
     *
     * @param expectedTime expected time in seconds
     */
    protected void verify(double expectedTime) throws Exception {
        PlannedTimeSummary time = PlannedTimeSummaryService.getTotalTime(getObs());
        assertEquals(expectedTime, time.getExecTime()/1000.0, 0.001);
    }

    /**
     * Creates a parameter value suitable for storage in an instrument sequence
     * data object.
     *
     * @param propName name of the property
     * @param vals one or more values associated with the property
     */
    public static <T> IParameter getParam(String propName, T... vals) {
        List<T> lst = Arrays.asList(vals);
        return DefaultParameter.getInstance(propName, lst);
    }

    public static IParameter getExpTimeParam(Double... secs) {
        return getParam(InstConstants.EXPOSURE_TIME_PROP, secs);
    }

    public static IParameter getCoaddsParam(Integer... coadds) {
        return getParam(InstConstants.COADDS_PROP, coadds);
    }

    public static ISysConfig createSysConfig() {
        return new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
    }

    /**
     * Updates the instrument iterator with the given configuration.
     */
    @SuppressWarnings({"unchecked"})
    protected void setSysConfig(ISysConfig sc) throws Exception {
        instSeqDataObject.setSysConfig(sc);
        instSeqComponent.setDataObject(instSeqDataObject);

        instDataObject = (I) instObsComponent.getDataObject();
    }

    /**
     * Updates the static instrument observation component with any changes
     * that have been made.
     */
    @SuppressWarnings({"unchecked"})
    protected void storeStaticUpdates() throws Exception {
        instObsComponent.setDataObject(instDataObject);

        instSeqDataObject = (S) instSeqComponent.getDataObject();
    }
}
