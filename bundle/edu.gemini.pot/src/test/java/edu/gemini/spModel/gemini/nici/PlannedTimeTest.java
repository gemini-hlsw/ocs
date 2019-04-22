//
// $
//

package edu.gemini.spModel.gemini.nici;

import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.shared.util.immutable.Tuple2;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import static edu.gemini.spModel.gemini.nici.NICIParams.*;

import edu.gemini.spModel.obs.plannedtime.OffsetOverheadCalculator;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummaryService;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeSummary;
import edu.gemini.spModel.test.InstrumentSequenceTestBase;
import edu.gemini.spModel.target.offset.OffsetPosList;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test cases for the NICI planned time calculations.
 */
public class PlannedTimeTest extends InstrumentSequenceTestBase<InstNICI, SeqConfigNICI> {


    protected SPComponentType getObsCompSpType() { return InstNICI.SP_TYPE; }

    protected SPComponentType getSeqCompSpType() { return SeqConfigNICI.SP_TYPE; }

    private static IParameter getFocalPlaneMaskParameter(FocalPlaneMask... masks) {
        return getParameter(InstNICI.FOCAL_PLANE_MASK_PROP, masks);
    }

    private static IParameter getChannel1Parameter(Channel1FW... filters) {
        return getParameter(InstNICI.CHANNEL1_FW_PROP, filters);
    }

    private static IParameter getChannel2Parameter(Channel2FW... filters) {
        return getParameter(InstNICI.CHANNEL2_FW_PROP, filters);
    }

    private static <T> IParameter getParameter(PropertyDescriptor pd, T... vals) {
        return getParam(pd.getName(), vals);
    }

    public void testSingleStepExposureTime() throws Exception {
        ISysConfig sc = createSysConfig();
        sc.putParameter(getCoaddsParam(3));
        sc.putParameter(getExpTimeParam(10.0));
        setSysConfig(sc);

        double expected = getInstDataObj().getSetupTime(getObs()).getSeconds() + InstNICI.EXPOSURE_OVERHEAD_CONSTANT;
        expected += 3 * (10.0 + InstNICI.COADD_CONSTANT);
        verify(expected);
    }

    public void testMultiStepExposureTime() throws Exception {
        ISysConfig sc = createSysConfig();
        sc.putParameter(getCoaddsParam(3, 5));
        sc.putParameter(getExpTimeParam(10.0, 12.0));
        setSysConfig(sc);

        // Make two observes at each step.
        SeqRepeatObserve sro = getObserveSeqDataObject();
        sro.setStepCount(2);
        getObserveSeqComp().setDataObject(sro);

        double base = getInstDataObj().getSetupTime(getObs()).getSeconds();

        double exp1 = InstNICI.EXPOSURE_OVERHEAD_CONSTANT + 3 * (10.0 + InstNICI.COADD_CONSTANT);
        double exp2 = InstNICI.EXPOSURE_OVERHEAD_CONSTANT + 5 * (12.0 + InstNICI.COADD_CONSTANT);

        verify(base + 2*(exp1 + exp2));
    }

    public void testDichroicWheelChange() throws Exception {
        IParameter param = getParameter(InstNICI.DICHROIC_WHEEL_PROP, DichroicWheel.H5050_BEAMSPLITTER, DichroicWheel.MIRROR);
        testSingleConfigChange(param);
    }

    public void testFiberCalSourceChange() throws Exception {
        IParameter param = getParameter(InstNICI.FOCS_PROP, Focs.IN_OFF, Focs.IN_ON);
        testSingleConfigChange(param);
    }

    public void testFocalPlaneMaskChange() throws Exception {
        testSingleConfigChange(getFocalPlaneMaskParameter(FocalPlaneMask.MASK_1, FocalPlaneMask.MASK_2));
    }

    public void testPupilImagerChange() throws Exception {
        IParameter param = getParameter(InstNICI.PUPIL_IMAGER_PROP, PupilImager.OPEN, PupilImager.PUPIL_IMAGING);
        testSingleConfigChange(param);
    }

    public void testPupilMaskChange() throws Exception {
        IParameter param = getParameter(InstNICI.PUPIL_MASK_PROP, PupilMask.APODIZED, PupilMask.EIGHTY_PERCENT);
        testSingleConfigChange(param);
    }

    public void testChannel1Change() throws Exception {
        testSingleConfigChange(getChannel1Parameter(Channel1FW.CH4H1L, Channel1FW.CH4H1S));
    }

    public void testChannel2Change() throws Exception {
        testSingleConfigChange(getChannel2Parameter(Channel2FW.CH4H1L, Channel2FW.CH4H1S));
    }

    private void testSingleConfigChange(IParameter param) throws Exception {
        ISysConfig sc = createSysConfig();
        sc.putParameter(param);
        testTwoStepConfigChange(sc);
    }

    // Tests two or more items in the config changing simultaneously.
    public void testMultiConfigChange() throws Exception {
        ISysConfig sc = createSysConfig();
        sc.putParameter(getFocalPlaneMaskParameter(FocalPlaneMask.MASK_1, FocalPlaneMask.MASK_2));
        sc.putParameter(getChannel1Parameter(Channel1FW.CH4H1L, Channel1FW.CH4H1S));
        sc.putParameter(getChannel2Parameter(Channel2FW.CH4H1L, Channel2FW.CH4H1S));
        testTwoStepConfigChange(sc);
    }

    private void testTwoStepConfigChange(ISysConfig sc) throws Exception {
        setSysConfig(sc);

        getInstDataObj().setExposureTime(10.0);
        getInstDataObj().setCoadds(1);
        storeStaticUpdates();

        double base = getInstDataObj().getSetupTime(getObs()).getSeconds();

        // Time for two exposures of 10 seconds with 1 coadd
        double expTime = 2 * (InstNICI.EXPOSURE_OVERHEAD_CONSTANT + 10.0 + InstNICI.COADD_CONSTANT);

        // Setup + exposure + 1 configuration change
        verify(base + expTime + InstNICI.CONFIG_CHANGE_COST);
    }

    private Tuple2<Long, Long> testOffsetCorrection(Offset... offsets) throws Exception {
        ISysConfig sc = createSysConfig();

        // We want the number of steps to be the number of offset positions.
        // We will measure the time required without offset positions for this
        // number of steps.  Later, we will make one instrument iterator step,
        // but with offsets.length offset positions so the step count will be
        // the same and the only difference will be whether offsets were
        // used or not.
        Integer[] coadds = new Integer[offsets.length];
        Arrays.fill(coadds, 1);
        Double[] exptime = new Double[offsets.length];
        Arrays.fill(exptime, 10.0);

        sc.putParameter(getCoaddsParam(coadds));
        sc.putParameter(getExpTimeParam(exptime));
        setSysConfig(sc);

        // Figure out the overhead when no offset positions are given.
        PlannedTimeSummary noOffsetTime = PlannedTimeSummaryService.getTotalTime(getObs());

        // Reduce the number of steps in the inst iterator to 1.
        sc.putParameter(getCoaddsParam(1));
        sc.putParameter(getExpTimeParam(10.0));
        setSysConfig(sc);

        // Now insert offset position(s).
        ISPSeqComponent comp = getInstSeqComp();
        List<ISPSeqComponent> children = comp.getSeqComponents();

        List<ISPSeqComponent> empty = Collections.emptyList();
        comp.setSeqComponents(empty);

        ISPSeqComponent offsetComp = addSeqComponent(comp, SeqRepeatNiciOffset.SP_TYPE);
        SeqRepeatNiciOffset offsetDataObj = (SeqRepeatNiciOffset) offsetComp.getDataObject();
        OffsetPosList<NiciOffsetPos> posList = offsetDataObj.getPosList();
        for (Offset off : offsets) {
            posList.addPosition(off.p().getMagnitude(), off.q().getMagnitude());
        }
        offsetComp.setDataObject(offsetDataObj);
        offsetComp.setSeqComponents(children);

        // Get the time required with the offset position(s) in the mix.
        PlannedTimeSummary offsetTime = PlannedTimeSummaryService.getTotalTime(getObs());

        return new Pair<Long, Long>(noOffsetTime.getExecTime(), offsetTime.getExecTime());
    }

    private void testNoOffsetCorrection(double mag) throws Exception {
        // Get the time required to execute with a big offset.
        Angle a = new Angle(mag, Angle.Unit.ARCSECS);
        Offset offset = new Offset(a, a);
        Tuple2<Long, Long> res = testOffsetCorrection(offset);

        // Should differ by the standard offset overhead.  In other words, no
        // correction should have been applied.
        long actualDiff = res._2() - res._1();

        // Compute the difference in time that we expect to see.
        double secs = OffsetOverheadCalculator.instance.calc(Offset.ZERO_OFFSET, offset);
        long expectedDiff = Math.round(secs * 1000);

        assertEquals(expectedDiff, actualDiff);
    }

    public void testNoOffsetCorrectionWellOverLimit() throws Exception {
        testNoOffsetCorrection(10); // well over small offset limit
    }

    public void testNoOffsetCorrectionJustOverLimit() throws Exception {
        testNoOffsetCorrection(0.22); // just over the small offset limit
    }

    private void testSmallOffsetCorrection(double mag) throws Exception {
        // Get the time required to execute with a small offset.
        Angle a = new Angle(mag, Angle.Unit.ARCSECS);
        Offset offset = new Offset(a, a);
        Tuple2<Long, Long> res = testOffsetCorrection(offset);

        // Should differ by a second, which is the overhead for small offsets.
        long actualDiff = res._2() - res._1();

        assertEquals(1000, actualDiff);
    }

    public void testSmallOffsetCorrectionWellUnderLimit() throws Exception {
        testSmallOffsetCorrection(0.2);      // under the limit
    }

    public void testSmallOffsetCorrectionJustUnderLimit() throws Exception {
        testSmallOffsetCorrection(0.212132); // just under the limit
    }

    public void testUnchangingSmallOffsetCorrection() throws Exception {
        // Get the time required to execute with a small offset.
        Angle a = new Angle(0.2, Angle.Unit.ARCSECS);
        Offset offset = new Offset(a, a);
        Tuple2<Long, Long> res = testOffsetCorrection(offset, offset);

        // Should differ by a second, which is the overhead for small offsets.
        long actualDiff = res._2() - res._1();

        assertEquals(1000, actualDiff);
    }

    public void testChangingSmallOffsetCorrection() throws Exception {
        // Get the time required to execute with a small offset.
        Angle a1 = new Angle(0.2, Angle.Unit.ARCSECS);
        Offset offset1 = new Offset(a1, a1);

        // at the 0.3 limit for a small offset distance
        Angle a2 = new Angle(0.212132, Angle.Unit.ARCSECS);
        Offset offset2 = new Offset(a2, a2);

        Tuple2<Long, Long> res = testOffsetCorrection(offset1, offset2);

        // Should differ by a second for each step, which is the overhead for
        // small offsets.
        long actualDiff = res._2() - res._1();

        assertEquals(2000, actualDiff);
    }

    public void testChangingSmallOffsetCorrectionToNoCorrection() throws Exception {
        // Get the time required to execute with a small offset.
        Angle a1 = new Angle(0.2, Angle.Unit.ARCSECS);
        Offset offset1 = new Offset(a1, a1);

        // Move well away from that offset position.
        Angle a2 = new Angle(-0.22, Angle.Unit.ARCSECS);
        Offset offset2 = new Offset(a2, a2);

        Tuple2<Long, Long> res = testOffsetCorrection(offset1, offset2);

        // Should differ by one second for the first step plus the standard
        // offset overhead.
        long actualDiff = res._2() - res._1();
        long expectedDiff = 1000;
        double secs = OffsetOverheadCalculator.instance.calc(offset2, offset1);
        expectedDiff += Math.round(secs * 1000);

        assertEquals(expectedDiff, actualDiff);
    }
}
