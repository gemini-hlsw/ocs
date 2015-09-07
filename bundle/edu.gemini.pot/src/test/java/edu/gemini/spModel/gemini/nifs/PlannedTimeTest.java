//
// $
//

package edu.gemini.spModel.gemini.nifs;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import static edu.gemini.spModel.gemini.nifs.NIFSParams.ReadMode;

import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.test.InstrumentSequenceTestBase;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;

/**
 * Test cases for the NIFS planned time calculations.
 */
public class PlannedTimeTest extends InstrumentSequenceTestBase<InstNIFS, SeqConfigNIFS> {

    protected SPComponentType getObsCompSpType() { return InstNIFS.SP_TYPE; }

    protected SPComponentType getSeqCompSpType() { return SeqConfigNIFS.SP_TYPE; }

    private static IParameter getReadModeParam(ReadMode... modes) {
        return getParam(InstNIFS.READMODE_PROP.getName(), modes);
    }

    public void testSingleStepReadMode() throws Exception {
        // Add a single step to the sequence.
        ISysConfig sc = createSysConfig();
        sc.putParameter(getExpTimeParam(100.0));
        setSysConfig(sc);

        double base = getInstDataObj().getSetupTime(getObs());
        base += 100.0 + InstNIFS.COADD_CONSTANT;
        base += PlannedTime.Category.DHS_OVERHEAD.add(1000).time/1000.;

        for (ReadMode mode : ReadMode.values()) {
            getInstDataObj().setReadMode(mode);
            storeStaticUpdates();
            verify(base + mode.getMinExp());
        }
    }

    public void testMultiStep() throws Exception {
        // Exp Time  Coadds  ReadMode
        // 88.0      2       Faint
        // 22.0      3       Medium
        //  5.5      4       Bright
        ISysConfig sc = createSysConfig();
        sc.putParameter(getExpTimeParam(88.0, 22.0, 5.5));
        sc.putParameter(getCoaddsParam(2, 3, 4));
        sc.putParameter(getReadModeParam(ReadMode.values()));
        setSysConfig(sc);

        double base = getInstDataObj().getSetupTime(getObs());
        double coaddConst = InstNIFS.COADD_CONSTANT;
        double dhs = PlannedTime.Category.DHS_OVERHEAD.add(1000).time/1000.;

        double exp1 = dhs;
        exp1 += 2*(88.0 + ReadMode.FAINT_OBJECT_SPEC.getMinExp()  + coaddConst);

        double exp2 = dhs;
        exp2 += 3*(22.0 + ReadMode.MEDIUM_OBJECT_SPEC.getMinExp() + coaddConst);

        double exp3 = dhs;
        exp3 += 4*( 5.5 + ReadMode.BRIGHT_OBJECT_SPEC.getMinExp() + coaddConst);

        verify(base + exp1 + exp2 + exp3);
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


        double base = getInstDataObj().getSetupTime(getObs());

        double minExp = getInstDataObj().getReadMode().getMinExp();
        double dhs = PlannedTime.Category.DHS_OVERHEAD.add(1000).time/1000.;
        double exp1 = dhs + 3*(10.0 + minExp + InstNIFS.COADD_CONSTANT);
        double exp2 = dhs + 5*(12.0 + minExp + InstNIFS.COADD_CONSTANT);

        verify(base + 2*(exp1 + exp2));
    }

    public void testMultiStepReadModeTime() throws Exception {
        getInstDataObj().setReadMode(ReadMode.BRIGHT_OBJECT_SPEC);
        getInstDataObj().setExposureTime(100.0);
        storeStaticUpdates();

        ISysConfig sc = createSysConfig();
        sc.putParameter(getReadModeParam(ReadMode.BRIGHT_OBJECT_SPEC, ReadMode.FAINT_OBJECT_SPEC));
        setSysConfig(sc);

        // Make two observes at each step.
        SeqRepeatObserve sro = getObserveSeqDataObject();
        sro.setStepCount(2);
        getObserveSeqComp().setDataObject(sro);

        double base = getInstDataObj().getSetupTime(getObs());
        double dhs = PlannedTime.Category.DHS_OVERHEAD.add(1000).time/1000.;
        double common = 100.0 + dhs + InstNIFS.COADD_CONSTANT;
        double bright = common + ReadMode.BRIGHT_OBJECT_SPEC.getMinExp();
        double faint  = common + ReadMode.FAINT_OBJECT_SPEC.getMinExp();

        verify(base + 2*bright + 2*faint);
    }
}
