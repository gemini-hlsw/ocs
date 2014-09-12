//
// $Id: ParamImpl.java 7020 2006-05-10 15:06:05Z gillies $
//
package edu.gemini.spModel.pio.xml;

import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.PioPath;
import edu.gemini.spModel.pio.PioVisitor;
import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of the {@link Param} interface.
 */
final class ParamImpl extends PioNodeImpl implements Param {
    private static final String UNITS_ATTR = "units";
    private static final String VALUE_ATTR = "value";

    ParamImpl(ParamElement element) {
        super(element);
    }

    ParamElement getParamElement() {
        return (ParamElement) getElement();
    }

    public void accept(PioVisitor visitor) {
        visitor.visitParam(this);
    }

    public String getUnits() {
        return getAttribute(UNITS_ATTR);
    }

    public void setUnits(String units) {
        setAttribute(UNITS_ATTR, units);
    }

    private String _getValueAttrText() {
        return getElement().attributeValue(VALUE_ATTR);
    }

    public String getValue() {
        String res = _getValueAttrText();
        if (res == null) res = _getValueElementTextAt(0);
        return res;
    }

    public void setValue(String value) {
        clearValues();
        if (value == null) return;

        // Use a value element for multi-line or long values, otherwise attributes
        if ((value.indexOf('\n') != -1) || (value.length() > 128)) {
            getElement().addElement(ValueElement.NAME).addText(value);
        } else {
            getElement().addAttribute(VALUE_ATTR, value);
        }
    }


    private Element _getValueElementAt(int i) {
        if (i < 0) throw new IllegalArgumentException("i = " + i);

        List elements = getElement().elements(ValueElement.NAME);
        if ((elements == null) || (elements.size() == 0)) return null;

        // Special case.  If there is exactly one value, but it is stored as
        // an element instead of an attribute because it is multi-line, then
        // the nested value element will not have a sequence number.
        if ((i == 0) && (elements.size() == 1)) {
            return (ValueElement) elements.get(0);
        }

        // Try the i'th element.  This will work assuming the value elements
        // are ordered in the XML file.
        if (i < elements.size()) {
            ValueElement element = (ValueElement) elements.get(i);
            if (i == element.getSequence()) {
                return element;
            }
        }

        // Iterate through the list looking for the one with sequence number i.
        for (Iterator it=elements.iterator(); it.hasNext(); ) {
            ValueElement element = (ValueElement) it.next();
            if (i == element.getSequence()) {
                return element;
            }
        }
        return null;
    }

    private String _getValueElementTextAt(int i) {
        Element e = _getValueElementAt(i);
        if (e == null) return null;
        return e.getText();
    }

    public int getValueCount() {
        List elements = getElement().elements(ValueElement.NAME);
        if ((elements == null) || (elements.size() == 0)) {
            return (_getValueAttrText() == null) ? 0 : 1;
        }
        return elements.size();
    }

    public void clearValues() {
        Element e = getElement();

        // Remove the "value" attribute.
        Attribute a = e.attribute(VALUE_ATTR);
        if (a != null) e.remove(a);

        // Remove any nested "value" elements.
        List elements = e.elements(ValueElement.NAME);
        if (elements != null) {
            for (Iterator it=elements.iterator(); it.hasNext(); ) {
                e.remove((Element) it.next());
            }
        }
    }

    public void addValue(String value) {
        Element e = getElement();

        int valueCount = getValueCount();

        // Add the value as a simple attribute, if it is the only one and it
        // isn't multi-lined.
        if ((valueCount == 0) && (value.indexOf('\n') == -1)) {
            setAttribute(VALUE_ATTR, value);
            return;
        }

        // If there is exactly one attribute
        if (valueCount == 1) {
            // If the one value is an attribute, move it to be a nested
            // element.
            Attribute a = e.attribute(VALUE_ATTR);
            if (a != null) {
                String seq0Value = a.getValue();
                e.remove(a);

                ValueElement ve = new ValueElement();
                ve.setSequence(0);
                ve.setText(seq0Value);
                e.add(ve);
            }
        }

        // Add a new nested element for this value.
        ValueElement ve = new ValueElement();
        ve.setSequence(valueCount);
        ve.setText(value);
        e.add(ve);
    }

    public String getValue(int index) {
        String res = _getValueElementTextAt(index);
        if ((res == null) && (index == 0)) {
            res = _getValueAttrText();
        }
        if (res == null) {
            throw new IndexOutOfBoundsException("index = " + index);
        }
        return res;
    }

    public void setValue(int index, String value) {
        Element e = _getValueElementAt(index);
        if (e == null) {
            // Handle the special case of changing the "value" attribute.
            if (index == 0) {
                Attribute a = e.attribute(VALUE_ATTR);
                if (a != null) {
                    a.setValue(value);
                    return;
                }
            }
            throw new IndexOutOfBoundsException("index = " + index);
        }

        e.setText(value);
    }


    public List getValues() {
        Element e = getElement();
        List valueElements = e.elements(ValueElement.NAME);
        if ((valueElements == null) || (valueElements.size() == 0)) {
            // There were no nested <value> elements, so just return the one
            // "value" attribute.
            String value = _getValueAttrText();
            if (value == null) return Collections.EMPTY_LIST;

            List res = new ArrayList();
            res.add(value);
            return res;
        }
        ValueElement.sortBySequence(valueElements);

        List res = new ArrayList();
        for (Iterator it=valueElements.iterator(); it.hasNext(); ) {
            res.add(((Element) it.next()).getText());
        }
        return res;
    }

    public void setValues(List values) {
        clearValues();
        if (values == null) return;

        if (values.size() == 1) {
            setValue((String) values.get(0));
            return;
        }

        Element e = getElement();
        int i=0 ;
        for (Iterator it=values.iterator(); it.hasNext(); ) {
            String value = (String) it.next();

            ValueElement ve = new ValueElement();
            ve.setSequence(i++);
            ve.setText(value);

            e.add(ve);
        }
    }

    public PioPath getPath() {
        return PioNamedNodeUtil.getPath(this, getName());
    }

    public String getName() {
        return PioNamedNodeUtil.getName(this);
    }

    public void setName(String name) {
        if (name == null) throw new NullPointerException("name may not be null");
        PioNamedNodeUtil.setName(this, name);
    }
}
