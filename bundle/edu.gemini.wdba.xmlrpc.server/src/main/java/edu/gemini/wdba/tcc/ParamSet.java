//
// $Id: ParamSet.java 756 2007-01-08 18:01:24Z gillies $
//
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
    static final protected String PARAM = "param";
    static final protected String PARAMSET = "paramset";
    static final protected String TYPE = "type";
    static final protected String VALUE = "value";

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
    public String getParamSetName() {
        return _name;
    }

    /**
     * Return the number of children of this paramset.
     */
    public int getParameterCount() {
        return nodeCount();
    }

    /**
     * Does the paramset contain a param with the paramName.
     */
    public boolean containsParameter(String paramName) {
        return _findNamedElement(PARAM, paramName) != null;
    }

    // Internal method to search for an element.
    private Element _findNamedElement(String elementName, String paramName) {
        List plist = elements(elementName);
        for (int i = 0, size = plist.size(); i < size; i++) {
            Element e = (Element) plist.get(i);
            if (e.attributeValue(NAME).equals(paramName)) return e;
        }
        return null;
    }


    /**
     * Return the value for the parameter with the given name.
     */
    public String getParameterValue(String name) {
        List<String> values = getParameterValues(name); // not very efficient...
        if (values.size() == 0) return null;
        return values.get(0);
    }

    public List<String> getParameterValues(String name) {
        Element e = _findNamedElement(PARAM, name);
        if (e == null) return Collections.emptyList();

        @SuppressWarnings({"unchecked"}) List<Element> values = (List<Element>) e.elements(VALUE);
        if (values == null) {
            // No nested "value" elements, check for the value attribute.
            String val = e.attributeValue(VALUE);
            if (val == null) return Collections.emptyList();
            return Collections.singletonList(val);
        }

        // Sort the values, just in case
        Collections.sort(values, new Comparator<Element>() {
            public int compare(Element e1, Element e2) {
                String seqStr1 = e1.attributeValue(TccNames.SEQUENCE);
                String seqStr2 = e2.attributeValue(TccNames.SEQUENCE);
                if (seqStr1 == null) seqStr1 = "";
                if (seqStr2 == null) seqStr2 = "";

                int s1 = 0;
                int s2 = 0;
                try {
                    s1 = Integer.parseInt(seqStr1);
                    s2 = Integer.parseInt(seqStr2);
                } catch (NumberFormatException ex) {
                    // ignore
                }
                return s1 - s2;
            }

        });

        // Fish out the included text.
        List<String> res = new ArrayList<String>();
        for (Element valueElement : values) {
            res.add(valueElement.getText());
        }
        return res;
    }

    /**
     * Return a <code>ParamSet</code> with the given name or null if it
     * doesn't exist.
     */
    public ParamSet getParamSet(String name) {
        return (ParamSet) _findNamedElement(PARAMSET, name);
    }

    public Iterator paramSetIterator() {
        return elementIterator(PARAMSET);
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

    public void putParamSet(ParamSet paramSet) {
        Element e = _findNamedElement(PARAMSET, paramSet.getParamSetName());
        if (e != null) {
            remove(e);
        }
        // This is required because a new ParamSet has the "system" name.
        paramSet.setName(PARAMSET);
        add(paramSet);
    }

    public boolean removeParameter(String paramName) {
        if (paramName == null) {
            throw new IllegalArgumentException("'param name' may not be null");
        }
        Element e = _findNamedElement(PARAM, paramName);
        return (e != null) ? remove(e) : false;
    }

    /**
     * Return all of the parameter names as a <code>{@link Set}</code>.
     *
     * @return <code>Collections.EMPTY_SET</code> is returned if there are
     * no parameters else, the set of parameter names as Strings are returned.
     */
    public Set getParameterNames() {
        List elems = elements();
        int size = elems.size();
        if (size == 0) return Collections.EMPTY_SET;

        // There are members so iterate
        Set s = new HashSet(size);
        for (int i = 0; i < size; i++) {
            Element e = (Element) elems.get(i);
            s.add(e.attributeValue(NAME));
        }
        return s;
    }

    /**
     * Remove all parameters in this configuration.
     */
    public void removeParameters() {
        clearContent();
    }

}

