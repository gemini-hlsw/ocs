//
// $Id: PioNodeImpl.java 4937 2004-08-14 21:35:20Z shane $
//
package edu.gemini.spModel.pio.xml;

import org.dom4j.Attribute;
import org.dom4j.Element;
import edu.gemini.spModel.pio.PioNode;
import edu.gemini.spModel.pio.PioPath;
import edu.gemini.spModel.pio.PioNodeParent;


/**
 * Base class for PIO object implementation classes.
 */
abstract class PioNodeImpl implements PioNode {
    private PioNodeElement _element;

    protected PioNodeImpl(PioNodeElement element) {
        _element = element;
    }

    /**
     * Gets the element which backs this object.  All PIO objects are backed
     * by a DOM4J Element.
     */
    protected PioNodeElement getElement() {
        return _element;
    }

    public PioNodeParent getParent() {
        Element parentElement = _element.getParent();
        if (!(parentElement instanceof PioNodeElement)) return null;
        return (PioNodeParent) ((PioNodeElement) parentElement).getPioNode();
    }

    public void detach() {
        Element parentElement = _element.getParent();
        if (!(parentElement instanceof PioNodeElement)) return;
        parentElement.remove(_element);
    }

    /**
     * Gets the attribute with the given <code>name</code> as a String, if it
     * exists.
     *
     * @return the attribute value as a String if it exists, <code>null</code>
     * otherwise
     */
    protected String getAttribute(String name) {
        return _element.attributeValue(name);
    }

    /**
     * Gets the attribute with the given <code>name</code> as a boolean,
     * returning <code>def</code> if it does not exist or cannot be parsed
     * into a boolean.
     */
    protected boolean getAttributeAsBoolean(String name, boolean def) {
        String valStr = getAttribute(name);
        if ("true".equalsIgnoreCase(valStr)) return true;
        if ("false".equalsIgnoreCase(valStr)) return false;
        return def;
    }

    /**
     * Gets the attribute with the given <code>name</code> as an int,
     * returning <code>def</code> if it does not exist or cannot be parsed
     * into an int.
     */
    protected int getAttributeAsInt(String name, int def) {
        String valStr = getAttribute(name);
        if (valStr == null) return def;

        int res = def;
        try {
            res = Integer.parseInt(valStr);
        } catch (NumberFormatException ex) {
        }
        return res;
    }

    /**
     * Sets the attribute with the given <code>name</code> to the given
     * <code>value</code>, adding it if necessary.  If <code>value</code> is
     * <code>null</code>, the attribute is removed.
     *
     * @param name name of the attribute to set/remove; may not be
     * <code>null</code>
     * @param value new value of the attribute
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    protected void setAttribute(String name, String value) {
        if (name == null) throw new NullPointerException();

        if (value == null) {
            Attribute attr = _element.attribute(name);
            if (attr != null) _element.remove(attr);
        } else {
            _element.addAttribute(name, value);
        }
    }

    /**
     * Sets the attribute with the given <code>name</code> to the String
     * representing the given boolean <code>value</code>.
     *
     * @param name name of the attribute to set
     * @param value new value of the attribute
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    protected void setAttribute(String name, boolean value) {
        if (name == null) throw new NullPointerException();
        _element.addAttribute(name, Boolean.toString(value));
    }

    /**
     * Sets the attribute with the given <code>name</code> to the String
     * representing the given int <code>value</code>.
     *
     * @param name name of the attribute to set
     * @param value new value of the attribute
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    protected void setAttribute(String name, int value) {
        if (name == null) throw new NullPointerException();
        _element.addAttribute(name, Integer.toString(value));
    }

    /**
     * Gets an XPath string representing the given <code>path</code>.  This
     * xpath may be used to find the corresponding Element.
     */
    protected static String getXPath(PioPath path) {
        StringBuffer buf = new StringBuffer();
        if (path.isAbsolute()) {
            buf.append("/*/");
        }
        String[] names = path.split();
        for (int i=0; i<names.length-1; ++i) {
            _appendPathSegement(names[i], buf).append("/");
        }
        if (names.length > 0) {
            _appendPathSegement(names[names.length-1], buf);
        }
        return buf.toString();
    }

    /**
     * Gets the most remote PioNode (the oldest ancestor) that contains this
     * PioNode.  Note, if the PioNode is not contained in any other, then
     * <code>this</code> is returned.
     *
     * @return root of the PioNode tree that contains this node
     */
    protected PioNode getRootNode() {
        PioNode node   = this;
        PioNode parent = node.getParent();
        while (parent != null) {
            node   = parent;
            parent = node.getParent();
        }
        return node;
    }

    private static StringBuffer _appendPathSegement(String name, StringBuffer buf) {
        if (PioPath.anonymousContainer.equals(name)) {
            buf.append(ContainerElement.NAME).append("[not(@name)]");
        } else {
            buf.append("*[@name='").append(name).append("']");
        }
        return buf;
    }
}
