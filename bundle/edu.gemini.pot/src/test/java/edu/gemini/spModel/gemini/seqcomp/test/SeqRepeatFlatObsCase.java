/*
 * Copyright 2003 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SeqRepeatFlatObsCase.java 38751 2011-11-16 19:37:18Z swalker $
 */
package edu.gemini.spModel.gemini.seqcomp.test;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.*;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatFlatObs;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.xml.PioXmlFactory;

public class SeqRepeatFlatObsCase extends TestCase {

    public SeqRepeatFlatObsCase(String message) {
        super(message);
    }

    public void testInit() {
        SeqRepeatFlatObs s = new SeqRepeatFlatObs();
        assertEquals(s.getLamps().iterator().next(), Lamp.DEFAULT);
        assertEquals(s.getFilter(), Filter.DEFAULT);
        assertEquals(s.getDiffuser(), Diffuser.DEFAULT);
        assertEquals(s.getObserveType(), InstConstants.FLAT_OBSERVE_TYPE);
        assertEquals(s.getShutter(), Shutter.DEFAULT);
        assertTrue(!s.isArc());
    }

    public void testIsArc() {
        SeqRepeatFlatObs s = new SeqRepeatFlatObs();

        s.setLamp(Lamp.IR_GREY_BODY_HIGH);
        assertTrue(!s.isArc());

        s.setLamp(Lamp.IR_GREY_BODY_LOW);
        assertTrue(!s.isArc());

        s.setLamp(Lamp.QUARTZ_5W);
        assertTrue(!s.isArc());

        s.setLamp(Lamp.AR_ARC);
        assertTrue(s.isArc());

        s.setLamp(Lamp.THAR_ARC);
        assertTrue(s.isArc());

        s.setLamp(Lamp.CUAR_ARC);
        assertTrue(s.isArc());
    }

    public void testParamSetIO() {
        SeqRepeatFlatObs s = new SeqRepeatFlatObs();
        s.setLamp(Lamp.AR_ARC);
        s.setFilter(Filter.GMOS);
        s.setDiffuser(Diffuser.VISIBLE);
        s.setShutter(Shutter.CLOSED);
        s.setExposureTime(123.);
        s.setCoaddsCount(4);
        s.setStepCount(5);

        ParamSet p = s.getParamSet(new PioXmlFactory());
        SeqRepeatFlatObs s2 = new SeqRepeatFlatObs();
        s2.setParamSet(p);

        assertTrue(s.getLamps().iterator().next() == s2.getLamps().iterator().next());
        assertTrue(s.getFilter() == s2.getFilter());
        assertTrue(s.getDiffuser() == s2.getDiffuser());
        assertTrue(s.getShutter() == s2.getShutter());
        assertTrue(s.getExposureTime() == s2.getExposureTime());
        assertTrue(s.getCoaddsCount() == s2.getCoaddsCount());
        assertTrue(s.getStepCount() == s2.getStepCount());
    }

    public static Test suite() {
        return new TestSuite(SeqRepeatFlatObsCase.class);
    }
}
