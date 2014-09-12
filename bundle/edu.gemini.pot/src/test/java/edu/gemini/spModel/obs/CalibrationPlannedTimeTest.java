//
// $
//

package edu.gemini.spModel.obs;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatFlatObs;
import edu.gemini.spModel.obs.plannedtime.GcalStepCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import edu.gemini.spModel.test.SpModelTestBase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * Tests the addition of planned time for telescope offsets.
 */
public class CalibrationPlannedTimeTest extends SpModelTestBase {

    public void setUp() throws Exception {
        super.setUp();
    }

    private ISPSeqComponent addCalibration() throws Exception {
        return addSeqComponent(getObs().getSeqComponent(), SeqRepeatFlatObs.SP_TYPE);
    }

    private ISPSeqComponent addObsCount(int obsCount) throws Exception {
        ISPSeqComponent comp = addSeqComponent(getObs().getSeqComponent(), SeqRepeatObserve.SP_TYPE);
        SeqRepeatObserve rep = new SeqRepeatObserve();
        rep.setStepCount(obsCount);
        comp.setDataObject(rep);
        return comp;
    }

    private ISPSeqComponent replaceWithCalibration(ISPSeqComponent comp) throws Exception {
        ISPSeqComponent parent = getObs().getSeqComponent();
        List<ISPSeqComponent> children = parent.getSeqComponents();

        int index = children.indexOf(comp);
        assertTrue(index >= 0);

        ISPSeqComponent calComp = getFactory().createSeqComponent(getProgram(), SeqRepeatFlatObs.SP_TYPE, null);

        children.set(index, calComp);

        parent.setChildren(new ArrayList<ISPNode>(children));

        return calComp;
    }

    private void setupInstrument() throws Exception {
        ISPObsComponent obsComp = addObsComponent(Flamingos2.SP_TYPE);
        Flamingos2 f2 = (Flamingos2) obsComp.getDataObject();
        f2.setExposureTime(InstConstants.DEF_EXPOSURE_TIME);
        obsComp.setDataObject(f2);
    }

    @Test public void testFirstStepScienceFoldMove() throws Exception {
        setupInstrument();

        // Add a normal observe.
        ISPSeqComponent obsIterComp = addObsCount(1);

        // Record the time used.
        PlannedTimeSummary pt0 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Replace the observe with an flat
        replaceWithCalibration(obsIterComp);

        // Record the time used now.
        PlannedTimeSummary pt1 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Check that the time increased by the science fold move.
        long diff = pt1.getExecTime() - pt0.getExecTime();
        assertEquals(GcalStepCalculator.SCIENCE_FOLD_MOVE_TIME, diff);
    }

    @Test public void testSecondStepScienceFoldMove() throws Exception {
        setupInstrument();

        // Add two normal observes
        addObsCount(1);
        ISPSeqComponent obsIterComp2 = addObsCount(1);

        // Record the time used.
        PlannedTimeSummary pt0 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Replace the second obs with an flat
        replaceWithCalibration(obsIterComp2);

        // Record the time used now.
        PlannedTimeSummary pt1 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Check that the time increased by the science fold move.
        long diff = pt1.getExecTime() - pt0.getExecTime();
        assertEquals(GcalStepCalculator.SCIENCE_FOLD_MOVE_TIME, diff);
    }

    @Test public void testMoveScienceFoldOut() throws Exception {
        setupInstrument();

        // Add three normal observes
        addObsCount(1);
        ISPSeqComponent obsIterComp2 = addObsCount(1);
        addObsCount(1);

        // Record the time used.
        PlannedTimeSummary pt0 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Replace the second obs with an flat
        replaceWithCalibration(obsIterComp2);

        // Record the time used now.
        PlannedTimeSummary pt1 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Check that the time increased by the science fold move, moving in
        // and then moving out in the third step
        long diff = pt1.getExecTime() - pt0.getExecTime();
        assertEquals(GcalStepCalculator.SCIENCE_FOLD_MOVE_TIME * 2, diff);
    }

    @Test public void testFilterChange() throws Exception {
        testConfigDiff(new ApplyOp<SeqRepeatFlatObs>() {
            @Override public void apply(SeqRepeatFlatObs seqRepeatFlatObs) {
                seqRepeatFlatObs.setFilter(CalUnitParams.Filter.ND_40);
            }
        });
    }

    @Test public void testDiffuserChange() throws Exception {
        testConfigDiff(new ApplyOp<SeqRepeatFlatObs>() {
            @Override public void apply(SeqRepeatFlatObs seqRepeatFlatObs) {
                seqRepeatFlatObs.setDiffuser(CalUnitParams.Diffuser.VISIBLE);
            }
        });
    }

    @Test public void testShutterChange() throws Exception {
        testConfigDiff(new ApplyOp<SeqRepeatFlatObs>() {
            @Override public void apply(SeqRepeatFlatObs seqRepeatFlatObs) {
                seqRepeatFlatObs.setShutter(CalUnitParams.Shutter.CLOSED);
            }
        });
    }

    @Test public void testLampChange() throws Exception {
        testConfigDiff(new ApplyOp<SeqRepeatFlatObs>() {
            @Override public void apply(SeqRepeatFlatObs seqRepeatFlatObs) {
                seqRepeatFlatObs.setLamp(CalUnitParams.Lamp.XE_ARC);
            }
        }, 0); // not expecting a config change cost here
    }

    private void testConfigDiff(ApplyOp<SeqRepeatFlatObs> op) throws Exception {
        testConfigDiff(op, GcalStepCalculator.CONFIG_CHANGE_TIME);
    }

    private void testConfigDiff(ApplyOp<SeqRepeatFlatObs> op, long expectedDiff) throws Exception {
        setupInstrument();

        // Add two calibration observes
        addCalibration();
        ISPSeqComponent calComp2 = addCalibration();

        // Record the time used.
        PlannedTimeSummary pt0 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Change the filter in the second step.
        SeqRepeatFlatObs caldo = (SeqRepeatFlatObs) calComp2.getDataObject();
        op.apply(caldo);
        calComp2.setDataObject(caldo);

        // Record the time used.
        PlannedTimeSummary pt1 = PlannedTimeSummaryService.getTotalTime(getObs());

        // Check that the time increased by the config change.
        long diff = pt1.getExecTime() - pt0.getExecTime();
        assertEquals(expectedDiff, diff);
    }
}
