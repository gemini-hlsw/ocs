/* Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 *
 * $Id: ParameterCase.java 38078 2011-10-18 15:15:29Z swalker $
 */
package edu.gemini.spModel.data.config.test;

import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.DefaultConfigParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.data.config.IConfigParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Class ParameterTest provides tests for StringParameter and DefaultConfigParameter
 */
public final class ParameterCase {
    private StringParameter _a;
    private StringParameter _b;
    private StringParameter _c;
    private DefaultConfigParameter _conf1;

    private String _aName = "A";
    private String _bName = "B";
    private String _cName = "C";
    private String _aValue = "A Value";
    private String _bValue = "B Value";
    private String _cValue = "C Value";
    private String _conf1Name = "conf 1";


    // No setup needed right now.
    @Before
    public void setUp() throws Exception {
        _a = StringParameter.getInstance(_aName, _aValue);
        _b = StringParameter.getInstance(_bName, _bValue);
        _c = StringParameter.getInstance(_cName, _cValue);

        _conf1 = DefaultConfigParameter.getInstance(_conf1Name);
    }

    // Test initialization of tests
    @Test
    public void testInitial() {
        assertEquals(_aName, _a.getName());
        assertEquals(_bName, _b.getName());
        assertEquals(_cName, _c.getName());
        assertEquals(_aValue, _a.getValue());
        assertEquals(_bValue, _b.getValue());
        assertEquals(_cValue, _c.getValue());
    }

    @Test
    public void testInitialConfig() {
        assertEquals(_conf1Name, _conf1.getName());
        assertTrue(!(_conf1.getValue() == null));

        ISysConfig sc = (ISysConfig) _conf1.getValue();
        assertEquals(_conf1Name, sc.getSystemName());
    }

    @Test
    public void testConfigValue() {
        ISysConfig sc = (ISysConfig) _conf1.getValue();
        assertTrue(!(sc == null));

        assertEquals(0, sc.getParameterCount());
    }

    private void _setupConfig(DefaultConfigParameter cp) {
        assertNotNull(cp);
        ISysConfig sc = (ISysConfig) cp.getValue();
        _setupConfigValue(sc);
    }

    private void _setupConfigValue(ISysConfig sc) {
        assertNotNull(sc);
        assertEquals(0, sc.getParameterCount());
        sc.putParameter(_a);
        assertEquals(1, sc.getParameterCount());
        sc.putParameter(_b);
        assertEquals(2, sc.getParameterCount());
        sc.putParameter(_c);
        assertEquals(3, sc.getParameterCount());
    }

    @Test
    public void testConfig() {
        _setupConfig(_conf1);
    }

    @Test
    public void testConfigSetValue() {
        IConfigParameter cp = _conf1;

        assertEquals("size", 0, cp.getParameterCount());

        ISysConfig sc = new DefaultSysConfig("test");
        _setupConfigValue(sc);
        cp.setValue(sc);
        int _testConfigSize = 3;
        assertEquals("size", _testConfigSize, cp.getParameterCount());
    }

    @Test
    public void testParamCloneable() {
        StringParameter t1 = (StringParameter) _a.clone();
        assertEquals("t1=_a", t1, _a);

        t1.setValue("New t1 Value");
        assertTrue("t1=_a", !t1.equals(_a));
    }

    @Test
    public void testConfigParamCloneable() {
        DefaultConfigParameter cp1 = _conf1;
        _setupConfig(cp1);

        DefaultConfigParameter cp2 = (DefaultConfigParameter) cp1.clone();
        ISysConfig sc2 = (ISysConfig) cp2.getValue();
        assertEquals("names", cp1.getName(), cp2.getName());
        assertEquals("conf", cp1, cp2);

        sc2.removeParameter(_aName);
        assertTrue("conf2", !cp1.equals(cp2));
    }

    // Test serialization
    @Test
    public void testSerialization() throws Exception {
        assertEquals(_a, ser(_a));
    }

    // Test serialization
    @Test
    public void testSerialization2() throws Exception {
        assertEquals(_conf1, ser(_conf1));
    }
}
