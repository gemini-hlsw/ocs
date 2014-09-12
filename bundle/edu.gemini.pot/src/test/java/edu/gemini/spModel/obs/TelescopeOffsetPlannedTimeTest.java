//
// $
//

package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffset;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatOffsetBase;
import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.spModel.test.SpModelTestBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the addition of planned time for telescope offsets.
 */
public class TelescopeOffsetPlannedTimeTest extends SpModelTestBase {

    private ISPSeqComponent observeIterator;

    public void setUp() throws Exception {
        super.setUp();
        observeIterator = addSeqComponent(getObs().getSeqComponent(), SeqRepeatObserve.SP_TYPE);
    }

    public ISPSeqComponent addOffsetIterator() throws Exception {
        // Remember the current sequence component children.
        ISPSeqComponent parent = getObs().getSeqComponent();
        List<ISPSeqComponent> children = parent.getSeqComponents();

        // Remove all of its children.
        List<ISPSeqComponent> empty = new ArrayList<ISPSeqComponent>();
        parent.setSeqComponents(empty);

        // Create an offset iterator and make it the only child of the root
        // sequence component.
        ISPSeqComponent offsetComp;
        offsetComp = addSeqComponent(parent, SeqRepeatOffset.SP_TYPE);

        // Put the previous children of the root as the children of the new
        // offset iterator.
        offsetComp.setSeqComponents(children);

        return offsetComp;
    }

    @SuppressWarnings({"unchecked"})
    private void addOffsetPosition(ISPSeqComponent seqComp, double p, double q) throws Exception {
        SeqRepeatOffsetBase<OffsetPosBase> dataObj;
        dataObj = (SeqRepeatOffsetBase<OffsetPosBase>) seqComp.getDataObject();
        dataObj.getPosList().addPosition(p, q);
        seqComp.setDataObject(dataObj);
    }

    private void setObserveCount(int count) throws Exception {
        SeqRepeatObserve dobj = (SeqRepeatObserve) observeIterator.getDataObject();
        dobj.setStepCount(count);
        observeIterator.setDataObject(dobj);
    }

    // offset time in seconds, which unfortunately are used to track time :-(
    private double getOffsetTime(double curP, double curQ, double prevP, double prevQ) {
        double p = curP - prevP;
        double q = curQ - prevQ;

        // Formula given in SCI-0203
        return 7.0 + Math.sqrt(p*p + q*q)/160.0;
    }

    private long toMilliseconds(double seconds) {
        return Math.round(seconds * 1000);
    }

    public void testWithOverridenElapsedTimeInstrument() throws Exception {
        // Add an instrument which has an elapsed time value
        addObsComponent(Flamingos2.SP_TYPE);
        verifySingleOffsetOverheads();
    }

    public void testWithGenericElapsedTimeInstrument() throws Exception {
        // Add an instrument which does not specify an elapsed time value.
        addObsComponent(InstTexes.SP_TYPE);
        verifySingleOffsetOverheads();
    }

    private void verifySingleOffsetOverheads() throws Exception {

        // Record the elapsed time without an offset position.
        PlannedTimeSummary pt0 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Add an offset iterator.
        ISPSeqComponent offset = addOffsetIterator();
        addOffsetPosition(offset, 10, 10);

        // Record the elapsed time with an offset position.
        PlannedTimeSummary pt1 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Make sure the time increased by the time required to do the offset.
        long offsetTime = toMilliseconds(getOffsetTime(10, 10, 0, 0));
        assertEquals(pt0.getExecTime() + offsetTime, pt1.getExecTime());
    }

    public void testMultiplePositions() throws Exception {
        addObsComponent(Flamingos2.SP_TYPE);

        // Record the elapsed time with two steps but without an offset
        // position.
        setObserveCount(2);
        PlannedTimeSummary pt0 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Add a couple of offset positions.
        ISPSeqComponent offset = addOffsetIterator();
        addOffsetPosition(offset,  5,  5);
        addOffsetPosition(offset, 10, 10);

        // Record the elapsed time with two offset positions, but a single
        // observe at each one.
        setObserveCount(1);
        PlannedTimeSummary pt1 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Calculate the time for offset positions.
        long offsetTime = toMilliseconds(getOffsetTime(5, 5, 0, 0) + getOffsetTime(10, 10, 5, 5));

        // Verify the time calculation
        assertEquals(pt0.getExecTime() + offsetTime, pt1.getExecTime());
    }

    public void testUnchangingOffsets() throws Exception {
        addObsComponent(Flamingos2.SP_TYPE);

        // Record the elapsed time with four steps but without an offset
        // position.
        setObserveCount(4);
        PlannedTimeSummary pt0 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Add a couple of offset positions.
        ISPSeqComponent offset = addOffsetIterator();
        addOffsetPosition(offset,  5,  5);
        addOffsetPosition(offset, 10, 10);

        // Record the elapsed time with two offset positions, but only two
        // observes at each one. (total of 4 steps)
        setObserveCount(2);
        PlannedTimeSummary pt1 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Calculate the time for offset positions.  There are only two offsets
        // so only count the time for each position, not twice at each position
        long offsetTime = toMilliseconds(getOffsetTime(5, 5, 0, 0) + getOffsetTime(10, 10, 5, 5));

        // Verify the time calculation
        assertEquals(pt0.getExecTime() + offsetTime, pt1.getExecTime());
    }
}
