//
// $Id: ParamSetTest.java 47163 2012-08-01 23:09:47Z rnorris $
//

package edu.gemini.spModel.pio.xml.test;

import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.PioPath;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.pio.xml.PioXmlUtil;
import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Test cases for the {@link Param} class.
 */
public class ParamSetTest extends TestCase {

    public ParamSetTest(String name) {
        super(name);
    }

    private PioFactory _fact;
    private ParamSet _paramSet;
    private Element _expected;

    public void setUp() {
        _fact = new PioXmlFactory();
        _paramSet = _fact.createParamSet("test");

        _expected = new DefaultElement("paramset");
        _expected.addAttribute("name", "test");
    }

    public void testCreate() throws Exception {
        assertEquals("test", _paramSet.getName());

        assertEquals(null, _paramSet.getKind());
        assertTrue(_paramSet.isEditable());
        assertTrue(_paramSet.isPublicAccess());
        assertEquals(-1, _paramSet.getSequence());
        assertEquals(0, _paramSet.getParamCount());
        assertEquals(0, _paramSet.getParamSetCount());
        assertEquals(Collections.EMPTY_LIST, _paramSet.getParams());
        assertEquals(Collections.EMPTY_LIST, _paramSet.getParamSets());
        assertEquals(null, _paramSet.getParam("xyz"));
        assertEquals(null, _paramSet.getParamSet("xyz"));

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_paramSet));
    }

    public void testAttributes() throws Exception {
        _paramSet.setKind("niri");
        _expected.addAttribute("kind", "niri");
        assertEquals("niri", _paramSet.getKind());

        _paramSet.setEditable(false);
        _expected.addAttribute("editable", "false");
        assertFalse(_paramSet.isEditable());

        _paramSet.setPublicAccess(false);
        _expected.addAttribute("access", "false");
        assertFalse(_paramSet.isPublicAccess());

        _paramSet.setSequence(0);
        _expected.addAttribute("sequence", "0");
        assertEquals(0, _paramSet.getSequence());

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_paramSet));

        // Remove the attributes now.
        _paramSet.setKind(null);
        _paramSet.setEditable(true);
        _paramSet.setPublicAccess(true);

        _expected.remove(_expected.attribute("kind"));
        _expected.remove(_expected.attribute("editable"));
        _expected.remove(_expected.attribute("access"));
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_paramSet));
    }

    public void testParamChild() throws Exception {
        // Add the first parameter
        Param p0 = _fact.createParam("paramchild0");
        p0.setValue("val0");
        _paramSet.addParam(p0);

        List<Param> params = new ArrayList<Param>();
        params.add(p0);
        assertEquals(1, _paramSet.getParamCount());
        assertEquals(p0, _paramSet.getParam("paramchild0"));
        assertEquals(params, _paramSet.getParams());

        assertEquals(0, _paramSet.getParamSetCount());
        assertEquals(Collections.EMPTY_LIST, _paramSet.getParamSets());
        assertEquals(null, _paramSet.getParamSet("paramchild0"));

        DefaultElement p0Element = new DefaultElement("param");
        p0Element.addAttribute("name", "paramchild0");
        p0Element.addAttribute("value", "val0");
        _expected.add(p0Element);
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_paramSet));

        // Add a second parameter
        Param p1 = _fact.createParam("paramchild1");
        p1.setValue("val1");
        _paramSet.addParam(p1);

        params.add(p1);
        assertEquals(2, _paramSet.getParamCount());
        assertEquals(p1, _paramSet.getParam("paramchild1"));
        assertEquals(params, _paramSet.getParams());

        DefaultElement p1Element = new DefaultElement("param");
        p1Element.addAttribute("name", "paramchild1");
        p1Element.addAttribute("value", "val1");
        _expected.add(p1Element);
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_paramSet));
    }

    public void testParamSetChild() throws Exception {
        // Add the first param set
        ParamSet ps0 = _fact.createParamSet("paramsetchild0");
        _paramSet.addParamSet(ps0);

        List<ParamSet> paramsets = new ArrayList<ParamSet>();
        paramsets.add(ps0);
        assertEquals(0, _paramSet.getParamCount());
        assertEquals(null, _paramSet.getParam("paramsetchild0"));
        assertEquals(Collections.EMPTY_LIST, _paramSet.getParams());

        assertEquals(1, _paramSet.getParamSetCount());
        assertEquals(ps0, _paramSet.getParamSet("paramsetchild0"));
        assertEquals(paramsets, _paramSet.getParamSets());

        DefaultElement ps0Element = new DefaultElement("paramset");
        ps0Element.addAttribute("name", "paramsetchild0");
        _expected.add(ps0Element);
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_paramSet));

        // Add a second param set
        ParamSet ps1 = _fact.createParamSet("paramsetchild1");
        _paramSet.addParamSet(ps1);

        paramsets.add(ps1);
        assertEquals(2, _paramSet.getParamSetCount());
        assertEquals(ps1, _paramSet.getParamSet("paramsetchild1"));
        assertEquals(paramsets, _paramSet.getParamSets());

        DefaultElement ps1Element = new DefaultElement("paramset");
        ps1Element.addAttribute("name", "paramsetchild1");
        _expected.add(ps1Element);
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_paramSet));
    }

    public void testMixedChildren() throws Exception {
        // Add a param and a paramset.
        List<Param> params = new ArrayList<Param>();
        Param p0 = _fact.createParam("paramchild0");
        p0.setValue("val0");
        _paramSet.addParam(p0);
        params.add(p0);

        List<ParamSet> paramsets = new ArrayList<ParamSet>();
        ParamSet ps0 = _fact.createParamSet("paramsetchild0");
        _paramSet.addParamSet(ps0);
        paramsets.add(ps0);

        assertEquals(1, _paramSet.getParamCount());
        assertEquals(p0, _paramSet.getParam("paramchild0"));
        assertEquals(params, _paramSet.getParams());

        assertEquals(1, _paramSet.getParamSetCount());
        assertEquals(ps0, _paramSet.getParamSet("paramsetchild0"));
        assertEquals(paramsets, _paramSet.getParamSets());

        assertEquals(2, _paramSet.getChildCount());

        DefaultElement p0Element = new DefaultElement("param");
        p0Element.addAttribute("name", "paramchild0");
        p0Element.addAttribute("value", "val0");
        _expected.add(p0Element);

        DefaultElement ps0Element = new DefaultElement("paramset");
        ps0Element.addAttribute("name", "paramsetchild0");
        _expected.add(ps0Element);
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_paramSet));

        // Add a second param and param set
        Param p1 = _fact.createParam("paramchild1");
        p1.setValue("val1");
        _paramSet.addParam(p1);
        params.add(p1);
        ParamSet ps1 = _fact.createParamSet("paramsetchild1");
        _paramSet.addParamSet(ps1);
        paramsets.add(ps1);

        assertEquals(2, _paramSet.getParamCount());
        assertEquals(p1, _paramSet.getParam("paramchild1"));
        assertEquals(params, _paramSet.getParams());
        assertEquals(2, _paramSet.getParamSetCount());
        assertEquals(ps1, _paramSet.getParamSet("paramsetchild1"));
        assertEquals(paramsets, _paramSet.getParamSets());

        assertEquals(4, _paramSet.getChildCount());

        DefaultElement p1Element = new DefaultElement("param");
        p1Element.addAttribute("name", "paramchild1");
        p1Element.addAttribute("value", "val1");
        _expected.add(p1Element);

        DefaultElement ps1Element = new DefaultElement("paramset");
        ps1Element.addAttribute("name", "paramsetchild1");
        _expected.add(ps1Element);
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_paramSet));
    }

    public void testGetMultiple() throws Exception {
        // Add a few params and param sets with the same name
        List<Param> params = new ArrayList<Param>();
        List<ParamSet> paramsets = new ArrayList<ParamSet>();
        for (int i=0; i<5; ++i) {
            Param p = _fact.createParam("child");
            p.setValue("val" + i);
            _paramSet.addParam(p);
            params.add(p);

            ParamSet ps = _fact.createParamSet("child");
            _paramSet.addParamSet(ps);
            paramsets.add(ps);

            DefaultElement pElement = new DefaultElement("param");
            pElement.addAttribute("name", "child");
            pElement.addAttribute("value", "val" + i);
            _expected.add(pElement);

            DefaultElement psElement = new DefaultElement("paramset");
            psElement.addAttribute("name", "child");
            _expected.add(psElement);
        }
        assertEquals(params,    _paramSet.getParams("child"));
        assertEquals(paramsets, _paramSet.getParamSets("child"));

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_paramSet));
    }

    public void testLookup() throws Exception {
        // One deep.
        Param p = _fact.createParam("paramchild");
        p.setValue("val");
        _paramSet.addParam(p);

        ParamSet ps = _fact.createParamSet("paramsetchild");
        _paramSet.addParamSet(ps);

        assertNull(_paramSet.lookupParam(new PioPath("paramsetchild")));
        assertNull(_paramSet.lookupParamSet(new PioPath("paramchild")));

        assertEquals(p, _paramSet.lookupParam(new PioPath("paramchild")));
        assertEquals(ps, _paramSet.lookupParamSet(new PioPath("paramsetchild")));

        // Two deep.
        Param pgrand = _fact.createParam("paramgrandchild");
        ps.addParam(pgrand);

        PioPath path = new PioPath("paramsetchild/paramgrandchild");
        assertEquals(pgrand, _paramSet.lookupParam(path));
        assertNull(_paramSet.lookupParamSet(path));

        path = new PioPath("/test/paramsetchild/paramgrandchild");
        assertEquals(path, pgrand.getPath());
    }


    public static void main(String[] args) {
        TestRunner.run(ParamSetTest.class);
    }
}
