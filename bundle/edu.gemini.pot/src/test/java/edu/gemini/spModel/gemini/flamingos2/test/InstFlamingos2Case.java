/**
 * $Id: InstFlamingos2Case.java 40280 2011-12-30 20:59:24Z fnussber $
 */

package edu.gemini.spModel.gemini.flamingos2.test;

import edu.gemini.spModel.telescope.IssPort;
import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.obscomp.InstConstants;

public class InstFlamingos2Case extends TestCase {

    static final private double _ERROR = .00001;

    public InstFlamingos2Case(String message) {
        super(message);
    }

    // Setup some test objects.
    public void testInitial() {
        Flamingos2 inst = new Flamingos2();
        assertEquals(inst.getDisperser(), Flamingos2.Disperser.DEFAULT);
        assertEquals(inst.getFilter(), Flamingos2.Filter.DEFAULT);
        assertEquals(inst.getReadMode(), Flamingos2.ReadMode.DEFAULT);
        assertEquals(inst.getLyotWheel(), Flamingos2.LyotWheel.DEFAULT);
        assertEquals(inst.getIssPort(), IssPort.DEFAULT);
        assertEquals(inst.getFpu(), Flamingos2.FPUnit.DEFAULT);

        assertEquals(inst.getExposureTime(), inst.getReadMode().recomendedExpTimeSec(), _ERROR);
        assertEquals(inst.getCoadds(), InstConstants.DEF_COADDS);
        assertEquals(inst.getPosAngleDegrees(), InstConstants.DEF_POS_ANGLE, _ERROR);
    }


    // test get/set paramset
    public void testParamSetIO() {
        Flamingos2 inst = new Flamingos2();
        inst.setDisperser(Flamingos2.Disperser.R3000);
        inst.setFilter(Flamingos2.Filter.HK);
        inst.setLyotWheel(Flamingos2.LyotWheel.H2);
        inst.setReadMode(Flamingos2.ReadMode.MEDIUM_OBJECT_SPEC);
        inst.setIssPort(IssPort.UP_LOOKING);
        inst.setFpu(Flamingos2.FPUnit.LONGSLIT_4);
        inst.setExposureTime(300.);
        inst.setCoadds(2);
        inst.setPosAngleDegrees(90.);

        ParamSet p = inst.getParamSet(new PioXmlFactory());

        Flamingos2 copy = new Flamingos2();
        copy.setParamSet(p);

        assertEquals(copy.getDisperser(), Flamingos2.Disperser.R3000);
        assertEquals(copy.getDisperser(), inst.getDisperser());

        assertEquals(copy.getFilter(), Flamingos2.Filter.HK);
        assertEquals(copy.getFilter(), inst.getFilter());

        assertEquals(copy.getReadMode(), Flamingos2.ReadMode.MEDIUM_OBJECT_SPEC);
        assertEquals(copy.getReadMode(), inst.getReadMode());

        assertEquals(copy.getLyotWheel(), Flamingos2.LyotWheel.H2);
        assertEquals(copy.getLyotWheel(), inst.getLyotWheel());

        assertEquals(copy.getIssPort(), IssPort.UP_LOOKING);
        assertEquals(copy.getIssPort(), inst.getIssPort());

        assertEquals(copy.getFpu(), Flamingos2.FPUnit.LONGSLIT_4);
        assertEquals(copy.getFpu(), inst.getFpu());

        assertEquals(copy.getExposureTime(), 300., _ERROR);
        assertEquals(copy.getCoadds(), 2, _ERROR);
        assertEquals(copy.getPosAngleDegrees(), 90., _ERROR);
    }


    public static Test suite() {
        return new TestSuite(InstFlamingos2Case.class);
    }
}
