//
// $
//

package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.ReadMode;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.obs.plannedtime.PlannedTime;
import edu.gemini.spModel.obs.plannedtime.PlannedTimeCalculator;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.seqcomp.SeqRepeatObserve;
import edu.gemini.spModel.test.InstrumentSequenceTestBase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * work in progress test cases for GNIRS elapsed times
 */
public class ElapsedTimeTest extends InstrumentSequenceTestBase<InstGNIRS, SeqConfigGNIRS> {
    @Override
    protected SPComponentType getObsCompSpType() { return InstGNIRS.SP_TYPE; }
    @Override
    protected SPComponentType getSeqCompSpType() { return SeqConfigGNIRS.SP_TYPE; }

    private static final Map<ReadMode, double[]> overheads = new HashMap<ReadMode, double[]>();

    // DHS overhead in secs
    private static final double dhsSecs = 2.8; // REL-1678


    static{
        overheads.put(ReadMode.VERY_BRIGHT, new double[]{0.8, 0.14});
        overheads.put(ReadMode.BRIGHT, new double[]{0.5, 0.7});
        overheads.put(ReadMode.FAINT, new double[]{2.8, 11.0});
        overheads.put(ReadMode.VERY_FAINT, new double[]{5.0, 21.9});
    }

    private void verifyElapsedTimes(double... expected) throws Exception {
        PlannedTime pta = PlannedTimeCalculator.instance.calc(getObs());
        assertEquals(expected.length, pta.steps.size());

        for (int i=0; i<expected.length; ++i) {
            assertEquals("Observe #"+i+" failed", Math.round(expected[i] * 1000), pta.steps.get(i).totalTime());
        }
    }


    @Test
    public void testBasic() throws Exception {
        InstGNIRS gnirs = getInstDataObj();
        gnirs.setCoadds(2);
        gnirs.setExposureTime(10.0);
        gnirs.setReadMode(ReadMode.FAINT);
        storeStaticUpdates();

        ISysConfig sc = createSysConfig();
        sc.putParameter(getParam(InstConstants.OBSERVING_WAVELENGTH_PROP,GNIRSConstants.DEF_CENTRAL_WAVELENGTH));
        setSysConfig(sc);
        verifyElapsedTimes(2*10 + 2.8+(2*11.0) + dhsSecs);
    }
    @Test
    public void testBasicSequence() throws Exception {
        InstGNIRS gnirs = getInstDataObj();
        gnirs.setCoadds(2);
        gnirs.setExposureTime(10.0);
        gnirs.setReadMode(ReadMode.FAINT);
        storeStaticUpdates();


        double[] expTimes={100.0,50.0,10.0,1.0};
        int[] coadds={1,3};



        ISysConfig sc = createSysConfig();
        for(double expTime:expTimes){
            for(ReadMode rm:ReadMode.values()){
                sc.putParameter(getExpTimeParam(expTime, expTime));
                sc.putParameter(getCoaddsParam(coadds[0], coadds[1]));
                sc.putParameter(getParam(GNIRSConstants.READ_MODE_PROP,rm));
                setSysConfig(sc);
                double[] oh=overheads.get(rm);
                verifyElapsedTimes(coadds[0]*expTime + oh[0]+(coadds[0]*oh[1]) + dhsSecs,
                        coadds[1]*expTime + oh[0]+(coadds[1]*oh[1]) + dhsSecs);
            }
        }
    }

    /**
     * 	GNIRS (ReadMode={faint, very faint})
	 *	    GNIRS (coadds={1,2})
	 *		    Observe
     */
    @Test
    public void testNested() throws Exception {
        List<ISPNode> children = new ArrayList<ISPNode>();
        children.add(getObserveSeqComp());  // child observe 1X

        ISPSeqComponent gnirsSeq = getInstSeqComp();  // GNIRS sequence
        gnirsSeq.setChildren(new ArrayList<ISPNode>());// remove GNIRS sequence children
        ISPSeqComponent nestedSeq = addSeqComponent(gnirsSeq, getSeqCompSpType());//add a nested sequence
        nestedSeq.setChildren(children);//add the observe to the nested sequence

        //ISPSeqComponent seqComp = getObs().getSeqComponent();  // Root sequence
        //seqComp.setChildren(children);

        //setup static component
        double expTime=10.0;
        InstGNIRS gnirs = getInstDataObj();
        gnirs.setCoadds(1);
        gnirs.setExposureTime(expTime);
        gnirs.setReadMode(ReadMode.FAINT);
        storeStaticUpdates();


        //setup GNIRS sequence
        ISysConfig sc = createSysConfig();
        sc.putParameter(getParam(GNIRSConstants.READ_MODE_PROP,ReadMode.FAINT,ReadMode.VERY_FAINT));
        SeqConfigGNIRS gnirsDataObj = (SeqConfigGNIRS)gnirsSeq.getDataObject();
        gnirsDataObj.setSysConfig(sc);
        gnirsSeq.setDataObject(gnirsDataObj);

        //setup nested sequence
        int[] coadds={1,2};
        sc = createSysConfig();
        sc.putParameter(getCoaddsParam(coadds[0],coadds[1]));
        SeqConfigGNIRS nestedDataObj = (SeqConfigGNIRS)nestedSeq.getDataObject();
        nestedDataObj.setSysConfig(sc);
        nestedSeq.setDataObject(nestedDataObj);


        double[] ohFaint=overheads.get(ReadMode.FAINT);
        double[] ohVeryFaint=overheads.get(ReadMode.VERY_FAINT);

        verifyElapsedTimes(coadds[0]*expTime + ohFaint[0]+(coadds[0]*ohFaint[1]) + dhsSecs,
                coadds[1]*expTime + ohFaint[0]+(coadds[1]*ohFaint[1]) + dhsSecs,
                coadds[0]*expTime + ohVeryFaint[0]+(coadds[0]*ohVeryFaint[1]) + dhsSecs,
                coadds[1]*expTime + ohVeryFaint[0]+(coadds[1]*ohVeryFaint[1]) + dhsSecs);
    }

    /**
     * 	GNIRS (ReadMode={faint, very faint})
	 *	    GNIRS (coadds={1,2})
	 *  	    Observe
	 *	    Observe
     */
    @Test
    public void testNested2() throws Exception {
        List<ISPNode> children = new ArrayList<ISPNode>();
        children.add(getObserveSeqComp());  // child observe 1X

        ISPSeqComponent gnirsSeq = getInstSeqComp();  // GNIRS sequence
        gnirsSeq.setChildren(new ArrayList<ISPNode>());// remove GNIRS sequence children
        ISPSeqComponent nestedSeq = addSeqComponent(gnirsSeq, getSeqCompSpType());//add a nested sequence
        nestedSeq.setChildren(children);//add the observe to the nested sequence
        addSeqComponent(gnirsSeq, SeqRepeatObserve.SP_TYPE);//add an observe to the outermost gnirs sequence


        //ISPSeqComponent seqComp = getObs().getSeqComponent();  // Root sequence
        //seqComp.setChildren(children);

        //setup static component
        double expTime=10.0;
        InstGNIRS gnirs = getInstDataObj();
        gnirs.setCoadds(1);
        gnirs.setExposureTime(expTime);
        gnirs.setReadMode(ReadMode.FAINT);
        storeStaticUpdates();


        //setup GNIRS sequence
        ISysConfig sc = createSysConfig();
        sc.putParameter(getParam(GNIRSConstants.READ_MODE_PROP,ReadMode.FAINT,ReadMode.VERY_FAINT));
        SeqConfigGNIRS gnirsDataObj = (SeqConfigGNIRS)gnirsSeq.getDataObject();
        gnirsDataObj.setSysConfig(sc);
        gnirsSeq.setDataObject(gnirsDataObj);

        //setup nested sequence
        int[] coadds={1,2};
        ISysConfig sc2 = createSysConfig();
        sc2.putParameter(getCoaddsParam(coadds[0],coadds[1]));
        SeqConfigGNIRS nestedDataObj = (SeqConfigGNIRS)nestedSeq.getDataObject();
        nestedDataObj.setSysConfig(sc2);
        nestedSeq.setDataObject(nestedDataObj);


        double[] ohFaint=overheads.get(ReadMode.FAINT);
        double[] ohVeryFaint=overheads.get(ReadMode.VERY_FAINT);

        verifyElapsedTimes(coadds[0]*expTime + ohFaint[0]+(coadds[0]*ohFaint[1]) + dhsSecs,
                coadds[1]*expTime + ohFaint[0]+(coadds[1]*ohFaint[1]) + dhsSecs,
                coadds[1]*expTime + ohFaint[0]+(coadds[1]*ohFaint[1]) + dhsSecs,
                coadds[0]*expTime + ohVeryFaint[0]+(coadds[0]*ohVeryFaint[1]) + dhsSecs,
                coadds[1]*expTime + ohVeryFaint[0]+(coadds[1]*ohVeryFaint[1]) + dhsSecs,
                coadds[1]*expTime + ohVeryFaint[0]+(coadds[1]*ohVeryFaint[1]) + dhsSecs);

    }
    /**
     * 	GNIRS (coadds={1,50})
	 *  	Observe
	 *  Observe // should use coadds 1, not 50 for this one
     */
    @Test
    public void test2Observes() throws Exception {
        ISPSeqComponent gnirsSeq = getInstSeqComp();  // GNIRS sequence
        addSeqComponent(getObs().getSeqComponent(), SeqRepeatObserve.SP_TYPE);//add an observe to the root sequence


        //ISPSeqComponent seqComp = getObs().getSeqComponent();  // Root sequence
        //seqComp.setChildren(children);

        //setup static component
        double expTime=10.0;
        InstGNIRS gnirs = getInstDataObj();
        gnirs.setCoadds(1);
        gnirs.setExposureTime(expTime);
        gnirs.setReadMode(ReadMode.FAINT);
        storeStaticUpdates();


        //setup GNIRS sequence
        ISysConfig sc = createSysConfig();
        int[] coadds={1,50};
        sc.putParameter(getCoaddsParam(coadds[0],coadds[1]));
        SeqConfigGNIRS gnirsDataObj = (SeqConfigGNIRS)gnirsSeq.getDataObject();
        gnirsDataObj.setSysConfig(sc);
        gnirsSeq.setDataObject(gnirsDataObj);

        double[] ohFaint=overheads.get(ReadMode.FAINT);


        verifyElapsedTimes(coadds[0]*expTime + ohFaint[0]+(coadds[0]*ohFaint[1]) + dhsSecs,
                coadds[1]*expTime + ohFaint[0]+(coadds[1]*ohFaint[1]) + dhsSecs,
                coadds[0]*expTime + ohFaint[0]+(coadds[0]*ohFaint[1]) + dhsSecs);
    }
}
