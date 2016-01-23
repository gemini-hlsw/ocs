package edu.gemini.wdba.tcc;

import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;

import java.util.*;

/**
 * A straight-forward implementation of the paramset XML structure used
 * for the tcc file and the sequence file.
 *
 * <p><b>Note that this implementation is not synchronized.</b>
 */
public class ParamSet extends DefaultElement {
    private String _name;
    private String _type;

    static final protected String NAME = "name";
    private static final String PARAM = "param";
    static final String PARAMSET = "paramset";
    static final String TYPE = "type";
    static final String VALUE = "value";

    /**
     * Create the param set with the value for the name attribute.
     */
    public ParamSet(String name) {
        super(PARAMSET);
        _createParamset(name, null);
    }

    /**
     * Create the param set with the value attribute and the type attribute
     * @param name the name of the paramset
     * @param type the type of paramset
     */
    public ParamSet(String name, String type) {
        super(PARAMSET);
        _createParamset(name, type);
    }

    private void _createParamset(String name, String type) {
        _name = name;
        add(DocumentHelper.createAttribute(this, NAME, name));
        if (type != null) {
            _type = type;
            add(DocumentHelper.createAttribute(this, TYPE, type));
        }
    }

    /**
     * Return the name of the param set.
     */
    private String getParamSetName() {
        return _name;
    }

    // Internal method to search for an element.
    private Element _findNamedElement(String elementName, String paramName) {
        List<?> plist = elements(elementName);
        for (Object aPlist : plist) {
            Element e = (Element) aPlist;
            if (e.attributeValue(NAME).equals(paramName)) return e;
        }
        return null;
    }

    public void putParameter(String paramName, String value) {
        Element e = _findNamedElement(PARAM, paramName);
        if (e == null) {
            e = addElement(PARAM);
            e.addAttribute(NAME, paramName);
        } else {
            // Remove any existing value elements.
            @SuppressWarnings({"unchecked"}) List<Element> values = (List<Element>) e.elements(VALUE);
            if ((values != null) && (values.size() > 0)) {
                for (Element valueElement : values) {
                    e.remove(valueElement);
                }
            }
        }

        // Set the "value" attribute.
        e.addAttribute(VALUE, value);
    }

    public void putParameter(String paramName, List<String> values) {
        // Handle an empty value list
        if ((values == null) || (values.size() == 0)) {
            putParameter(paramName, "");
            return;
        }

        // Handle a single value.
        if (values.size() == 1) {
            putParameter(paramName, values.get(0));
            return;
        }

        // Find or create the named parameter.
        Element e = _findNamedElement(PARAM, paramName);
        if (e == null) {
            e = addElement(PARAM);
            e.addAttribute(NAME, paramName);
        } else {
            // Remove any existing value attribute.
            Attribute attr = e.attribute(VALUE);
            if (attr != null) e.remove(attr);
        }

        // Add the values.
        int sequence = 0;
        for (String value : values) {
            Element valueElement = e.addElement(VALUE);
            valueElement.addAttribute(TccNames.SEQUENCE, String.valueOf(sequence++));
            valueElement.addText(value);
        }
    }

    void putParamSet(ParamSet paramSet) {
        Element e = _findNamedElement(PARAMSET, paramSet.getParamSetName());
        if (e != null) {
            remove(e);
        }
        // This is required because a new ParamSet has the "system" name.
        paramSet.setName(PARAMSET);
        add(paramSet);
    }

}

