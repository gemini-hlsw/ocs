package edu.gemini.epics.acm.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Node;

import static org.junit.Assert.*;

import edu.gemini.epics.acm.CaConfigFileConverter;

public final class CaConfigFileConverterTest {

    private static final String CONFIG_FILE = "/TestConfig.ca";
    private static final String CONTROL_FILE = "/TestConfig.xml";

    @Test
    public void testFileConversion() throws Exception {
        File testFile = File.createTempFile("test", ".xml");

        CaConfigFileConverter.convert(new BufferedReader(new FileReader(this
                .getClass().getResource(CONFIG_FILE).getPath())),
                new BufferedWriter(new FileWriter(testFile)));

        XMLUnit.setIgnoreWhitespace(true);

        Diff diff = new Diff(new BufferedReader(new FileReader(this.getClass()
                .getResource(CONTROL_FILE).getPath())), new BufferedReader(
                new FileReader(testFile)));
        diff.overrideElementQualifier(new ElementNameAndAttributeQualifier(
                "name"));
        diff.overrideDifferenceListener(new DifferenceListener() {

            @Override
            public void skippedComparison(Node control, Node test) {
                // TODO Auto-generated method stub

            }

            @Override
            public int differenceFound(Difference difference) {
                Node parentNode = difference.getControlNodeDetail().getNode()
                        .getParentNode();

                if (parentNode != null && parentNode.getLocalName() != null
                        && parentNode.getLocalName().equals("type")) {
                    return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                } else {
                    return RETURN_ACCEPT_DIFFERENCE;
                }
            }
        });

        assertTrue("Result file differs from control file: " + diff.toString(),
                diff.similar());
    }

}
