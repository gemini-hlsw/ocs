//
// $Id: ValueElement.java 4888 2004-08-02 22:56:25Z shane $
//
package edu.gemini.spModel.pio.xml;

import org.dom4j.tree.DefaultElement;
import org.dom4j.DocumentFactory;
import edu.gemini.spModel.pio.xml.PioXmlDocumentFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Collections;

class ValueElement extends DefaultElement {
    static final String NAME = "value";

    public static final Comparator SEQUENCE_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            ValueElement ve1 = (ValueElement) o1;
            ValueElement ve2 = (ValueElement) o2;
            return ve1.getSequence() - ve2.getSequence();
        }
    };

    public static void sortBySequence(List valueElementList) {
        Collections.sort(valueElementList, SEQUENCE_COMPARATOR);
    }

    public ValueElement() {
        super(NAME);
    }

    /*
    public ValueElement(QName qname) {
        super(qname);
    }

    public ValueElement(QName qname, int attributeCount) {
        super(qname, attributeCount);
    }

    public ValueElement(String name) {
        super(name);
    }

    public ValueElement(String name, Namespace namespace) {
        super(name, namespace);
    }
    */

    public int getSequence() {
        String valueStr = attributeValue("sequence");
        if (valueStr == null) return -1;
        try {
            return Integer.parseInt(valueStr);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public void setSequence(int sequence) {
        addAttribute("sequence", String.valueOf(sequence));
    }

    protected DocumentFactory getDocumentFactory() {
        return PioXmlDocumentFactory.INSTANCE;
    }
}
