/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file LICENSE for complete details.
 *
 * $Id: CalUnitCase.java 38751 2011-11-16 19:37:18Z swalker $
 */
package edu.gemini.spModel.gemini.calunit.test;

import edu.gemini.spModel.gemini.calunit.CalUnitConstants;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Diffuser;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Filter;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Lamp;
import edu.gemini.spModel.gemini.calunit.CalUnitParams.Shutter;
import edu.gemini.spModel.gemini.seqcomp.SeqRepeatFlatObs;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Class CalUnitTests runs tests on the calunit iterator and data
 * structures.
 */
public final class CalUnitCase {
    private SeqRepeatFlatObs _t1;

    @Before
    public void setUp() throws Exception {
        _t1 = new SeqRepeatFlatObs();
    }

    private Lamp getLamp0(SeqRepeatFlatObs o) { return o.getLamps().iterator().next(); }

    @Test
    public void testLampChanges() {
        Lamp l = getLamp0(_t1);
        assertEquals(Lamp.DEFAULT, l);

        _t1.setLamp(Lamp.QUARTZ_5W);
        l = getLamp0(_t1);
        assertEquals(Lamp.QUARTZ_5W, l);

        _t1.setLamp(Lamp.getLamp(Lamp.QUARTZ_5W.name()));
        l = getLamp0(_t1);
        assertEquals(Lamp.QUARTZ_5W, l);

        String s = getLamp0(_t1).name();
        assertEquals(Lamp.QUARTZ_5W.name(), s);
    }

    @Test
    public void testShutterChanges() {
        Shutter l = _t1.getShutter();
        assertEquals(Shutter.OPEN, l);

        _t1.setShutter(Shutter.CLOSED);
        l = _t1.getShutter();
        assertEquals(Shutter.CLOSED, l);

        _t1.setShutter(Shutter.getShutter(Shutter.CLOSED.name()));
        l = _t1.getShutter();
        assertEquals(Shutter.CLOSED, l);

        String s = _t1.getShutter().name();
        assertEquals(Shutter.CLOSED.name(), s);
    }

    @Test
    public void testFilterChanges() {
        Filter l = _t1.getFilter();
        assertEquals(Filter.DEFAULT, l);

        _t1.setFilter(Filter.ND_10);
        l = _t1.getFilter();
        assertEquals(Filter.ND_10, l);

        _t1.setFilter(Filter.getFilter(Filter.NONE.name()));
        l = _t1.getFilter();
        assertEquals(Filter.NONE, l);

        String s = _t1.getFilter().name();
        assertEquals(Filter.NONE.name(), s);
    }

    @Test
    public void testDiffuserChanges() {
        Diffuser l = _t1.getDiffuser();
        assertEquals(Diffuser.DEFAULT, l);

        _t1.setDiffuser(Diffuser.VISIBLE);
        l = _t1.getDiffuser();
        assertEquals(Diffuser.VISIBLE, l);

        _t1.setDiffuser(Diffuser.getDiffuser(Diffuser.IR.name()));
        l = _t1.getDiffuser();
        assertEquals(Diffuser.IR, l);

        String s = _t1.getDiffuser().name();
        assertEquals(Diffuser.IR.name(), s);
    }

    @Test
    public void testGetParamSet() {
        PioFactory factory = new PioXmlFactory();
        ParamSet p = _t1.getParamSet(factory);

        String v = Pio.getValue(p, CalUnitConstants.LAMP_PROP);
        assertEquals(Lamp.DEFAULT.name(), v);
        v = Pio.getValue(p, CalUnitConstants.FILTER_PROP);
        assertEquals(Filter.DEFAULT.name(), v);
        v = Pio.getValue(p, CalUnitConstants.DIFFUSER_PROP);
        assertEquals(Diffuser.DEFAULT.name(), v);

        v = Pio.getValue(p, InstConstants.COADDS_PROP);
        assertEquals(String.valueOf(InstConstants.DEF_COADDS), v);
        v = Pio.getValue(p, InstConstants.EXPOSURE_TIME_PROP);
        assertEquals(String.valueOf(InstConstants.DEF_EXPOSURE_TIME), v);
        v = Pio.getValue(p, InstConstants.REPEAT_COUNT_PROP);
        assertEquals(String.valueOf(InstConstants.DEF_REPEAT_COUNT), v);
    }

    @Test
    public void testSetParamSet() {
        PioFactory factory = new PioXmlFactory();
        ParamSet p = factory.createParamSet("");

        Pio.addParam(factory, p, CalUnitConstants.LAMP_PROP, Lamp.QUARTZ_5W.name());
        _t1.setParamSet(p);

        // Now check
        p = _t1.getParamSet(new PioXmlFactory());
        String v = Pio.getValue(p, CalUnitConstants.LAMP_PROP);
        assertEquals(Lamp.QUARTZ_5W.name(), v);
    }

    @Test
    public void testSerialization() throws Exception {
        final SeqRepeatFlatObs outObject = new SeqRepeatFlatObs();
        outObject.setLamp(Lamp.QUARTZ_5W);
        outObject.setDiffuser(Diffuser.VISIBLE);
        outObject.setFilter(Filter.ND_10);

        final SeqRepeatFlatObs inObject = ser(outObject);
        assertSame(Lamp.QUARTZ_5W, getLamp0(inObject));
        assertSame(Filter.ND_10, inObject.getFilter());
        assertSame(Diffuser.VISIBLE, inObject.getDiffuser());
    }
}
