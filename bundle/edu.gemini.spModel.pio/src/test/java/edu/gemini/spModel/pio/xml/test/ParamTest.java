//
// $Id: ParamTest.java 47163 2012-08-01 23:09:47Z rnorris $
//

package edu.gemini.spModel.pio.xml.test;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.PioPath;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.pio.xml.PioXmlUtil;
import org.dom4j.tree.DefaultElement;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.List;
import java.util.ArrayList;

/**
 * Test cases for the {@link Param} class.
 */
public class ParamTest extends TestCase {

    public ParamTest(String name) {
        super(name);
    }

    private PioFactory _fact;
    private Param _param;
    private Element _expected;

    public void setUp() {
        _fact = new PioXmlFactory();
        _param = _fact.createParam("testparam");

        _expected = new DefaultElement("param");
        _expected.addAttribute("name", "testparam");
    }



    public void testCreate() throws Exception {
        assertEquals("testparam", _param.getName());
        assertEquals(0, _param.getValueCount());
        assertEquals(null, _param.getValue());
        assertEquals(null, _param.getUnits());

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));
    }

    public void testSetName() throws Exception {
        _param.setName("x");
        assertEquals("x", _param.getName());

        try {
            _param.setName(null);
            fail("set name to null");
        } catch (NullPointerException ex) {
            // okay
        }
    }

    public void testSetUnits() throws Exception {
        _param.setUnits("x");
        assertEquals("x", _param.getUnits());

        _expected.addAttribute("units", "x");
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));

        _param.setUnits(null);
        assertNull(_param.getUnits());

        Attribute attr = _expected.attribute("units");
        _expected.remove(attr);
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));
    }

    public void testOneValue() throws Exception {
        // Set a single value.
        _param.setValue("x");
        assertEquals("x", _param.getValue());
        assertEquals(1, _param.getValueCount());

        List values = _param.getValues();
        assertNotNull(values);
        assertEquals(1, values.size());
        assertEquals("x", values.get(0));

        _expected.addAttribute("value", "x");
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));

        // Remove the value
        _param.setValue(null);
        assertNull(_param.getValue());
        assertEquals(0, _param.getValueCount());

        values = _param.getValues();
        assertNotNull(values);
        assertEquals(0, values.size());

        Attribute attr = _expected.attribute("value");
        _expected.remove(attr);
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));
    }

    public void testMultipleValues() throws Exception {
        List xvalues = new ArrayList();
        xvalues.add("a");
        xvalues.add("b");
        xvalues.add("c");

        // Set the value list
        _param.setValues(xvalues);
        assertEquals("a", _param.getValue()); // only the first value
        assertEquals(3, _param.getValueCount());

        List avalues = _param.getValues();
        assertNotNull(avalues);
        assertEquals(3, avalues.size());
        assertEquals("a", avalues.get(0));
        assertEquals("b", avalues.get(1));
        assertEquals("c", avalues.get(2));
        assertEquals("a", _param.getValue(0));
        assertEquals("b", _param.getValue(1));
        assertEquals("c", _param.getValue(2));

        Element valA = new DefaultElement("value");
        valA.addAttribute("sequence", "0");
        valA.setText("a");
        _expected.add(valA);

        Element valB = new DefaultElement("value");
        valB.addAttribute("sequence", "1");
        valB.setText("b");
        _expected.add(valB);

        Element valC = new DefaultElement("value");
        valC.addAttribute("sequence", "2");
        valC.setText("c");
        _expected.add(valC);

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));

        // Add another value.
        _param.addValue("d");
        assertEquals(4, _param.getValueCount());
        avalues = _param.getValues();
        assertNotNull(avalues);
        assertEquals(4, avalues.size());
        assertEquals("d", avalues.get(3));
        assertEquals("d", _param.getValue(3));

        Element valD = new DefaultElement("value");
        valD.addAttribute("sequence", "3");
        valD.setText("d");
        _expected.add(valD);

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));

        // Change just the last value.
        _param.setValue(3, "e");
        valD.setText("e");
        assertEquals("e", _param.getValue(3));
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));

        // Clear all the values.
        _param.clearValues();
        assertEquals(0, _param.getValueCount());
        _expected.remove(valA);
        _expected.remove(valB);
        _expected.remove(valC);
        _expected.remove(valD);
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));
    }

    public void testSingleMultiLineAttribute() throws Exception {
        // Set the multi-line element
        _param.setValue("line0\nline1\n");
        assertEquals("line0\nline1\n", _param.getValue());
        assertEquals(1, _param.getValueCount());

        // Should be stored as a child element.
        Element val = new DefaultElement("value");
        val.setText("line0\nline1\n");
        _expected.add(val);

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));

        // Now set the value as a single value element
        _param.setValue("line0");
        assertEquals("line0", _param.getValue());
        assertEquals(1, _param.getValueCount());

        _expected.remove(val);
        _expected.addAttribute("value", "line0");
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));
    }

    public void testMultipleValueToSingleValue() throws Exception {
        List xvalues = new ArrayList();
        xvalues.add("a");
        xvalues.add("b");

        // Set the value list
        _param.setValues(xvalues);

        Element valA = new DefaultElement("value");
        valA.addAttribute("sequence", "0");
        valA.setText("a");
        _expected.add(valA);

        Element valB = new DefaultElement("value");
        valB.addAttribute("sequence", "1");
        valB.setText("b");
        _expected.add(valB);

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));

        // Now set just one value.
        _param.setValue("z");
        assertEquals(1, _param.getValueCount());
        assertEquals("z", _param.getValue());
        _expected.remove(valA);
        _expected.remove(valB);
        _expected.addAttribute("value", "z");
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_param));
    }

    public void testGetPath() throws Exception {
        PioPath xpath = new PioPath("/testparam");
        PioPath apath = _param.getPath();
        assertEquals(xpath, apath);
    }

    public static void main(String[] args) {
        TestRunner.run(ParamTest.class);
    }
}
