//
// $Id: ParamSetRefTest.java 47163 2012-08-01 23:09:47Z rnorris $
//

package edu.gemini.spModel.pio.xml.test;

import edu.gemini.spModel.pio.*;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.pio.xml.PioXmlUtil;
import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;

import java.util.List;

/**
 * Test cases for the {@link Param} class.
 */
public class ParamSetRefTest extends TestCase {

    public ParamSetRefTest(String name) {
        super(name);
    }

    private PioFactory _fact;
    private ParamSet _containerParamSet;
    private ParamSet _referentParamSet;
    private ParamSet _referenceParamSet;

    private Element _expected;
    private Element _referentElement;
    private Element _referenceElement;

    public void setUp() {
        // Create the container paramset.
        _fact = new PioXmlFactory();
        _containerParamSet = _fact.createParamSet("container");

        _expected = new DefaultElement("paramset");
        _expected.addAttribute("name", "container");

        // Create the referent paramset with id = "paramid"
        _referentParamSet = _fact.createParamSet("referent");
        _referentParamSet.setId("paramid");
        _containerParamSet.addParamSet(_referentParamSet);

        _referentElement = new DefaultElement("paramset");
        _referentElement.addAttribute("name", "referent");
        _referentElement.addAttribute("id", "paramid");
        _expected.add(_referentElement);

        // Create the reference paramset with refid = "paramid"
        _referenceParamSet = _fact.createParamSet("reference");
        _referenceParamSet.setReferenceId("paramid");
        _containerParamSet.addParamSet(_referenceParamSet);

        _referenceElement = new DefaultElement("paramset");
        _referenceElement.addAttribute("name", "reference");
        _referenceElement.addAttribute("ref", "paramid");
        _expected.add(_referenceElement);

        // Just make sure it is set up correctly.
        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_containerParamSet));
    }

    public void testResolve() throws Exception {

        // Just try to resolve the reference.
        assertEquals(_referentParamSet, _referenceParamSet.getReferent());

        // Find the references to the referent.
        List references = _referentParamSet.getReferences();
        assertEquals(1, references.size());
        assertEquals(_referenceParamSet, references.get(0));
    }

    public void testReadReferentChildren() {

        // Add a child to the referent.
        Param p0 = _fact.createParam("paramchild0");
        p0.setValue("val0");
        _referentParamSet.addParam(p0);

        DefaultElement p0Element = new DefaultElement("param");
        p0Element.addAttribute("name", "paramchild0");
        p0Element.addAttribute("value", "val0");
        _referentElement.add(p0Element);

        // Make sure it is visible from the reference param set.
        assertEquals(p0, _referenceParamSet.getParam("paramchild0"));
        assertEquals(1, _referenceParamSet.getParamCount());

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_containerParamSet));
    }

    public void testAddReferentChildren() {

        // Add a child to the reference -- which should put the child in the
        // referent.
        Param p0 = _fact.createParam("paramchild0");
        p0.setValue("val0");
        _referenceParamSet.addParam(p0);

        DefaultElement p0Element = new DefaultElement("param");
        p0Element.addAttribute("name", "paramchild0");
        p0Element.addAttribute("value", "val0");
        _referentElement.add(p0Element); // <-- note this is the referent

        // Make sure it is visible from the reference param set.
        assertEquals(p0, _referentParamSet.getParam("paramchild0"));
        assertEquals(1, _referentParamSet.getParamCount());

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_containerParamSet));
    }

    public void testReferentConversion() {

        // Add a child to the referent.
        Param p0 = _fact.createParam("paramchild0");
        p0.setValue("val0");
        _referentParamSet.addParam(p0);
        assertEquals(p0, _referenceParamSet.getParam("paramchild0"));

        // Now set a reference id on the referent.  That will cause it to
        // loose all of its children.
        _referentParamSet.setReferenceId("unknownparamset");

        _referentElement.addAttribute("ref", "unknownparamset");
        _referentElement.remove(_referentElement.attribute("id"));

        PioTestUtil.assertEquals(_expected, PioXmlUtil.toElement(_containerParamSet));

        try {
            _referentParamSet.getChildCount();
            fail("should have thrown an exception");
        } catch (PioReferenceException ex) {
            // okay
        }

        try {
            _referenceParamSet.getChildCount();
            fail("should have thrown an exception");
        } catch (PioReferenceException ex) {
            // okay
        }
    }

    public static void main(String[] args) {
        TestRunner.run(ParamSetRefTest.class);
    }
}
