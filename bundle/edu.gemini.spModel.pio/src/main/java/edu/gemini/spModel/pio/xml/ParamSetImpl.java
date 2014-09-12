//
// $Id: ParamSetImpl.java 4995 2004-08-19 21:43:46Z shane $
//
package edu.gemini.spModel.pio.xml;

import java.util.*;

import edu.gemini.spModel.pio.*;
import org.dom4j.Element;

final class ParamSetImpl extends PioNodeParentImpl implements ParamSet {
    private static final String ACCESS_ATTR   = "access";
    private static final String EDITABLE_ATTR = "editable";
    private static final String ID_ATTR       = "id";
    private static final String KIND_ATTR     = "kind";
    private static final String REF_ATTR      = "ref";
    private static final String SEQUENCE_ATTR = "sequence";

    ParamSetElement getParamSetElement() {
        return (ParamSetElement) getElement();
    }

    ParamSetImpl(ParamSetElement element) {
        super(element);
    }

    public void accept(PioVisitor visitor) {
        visitor.visitParamSet(this);
    }

    public String getKind() {
        return getAttribute(KIND_ATTR);
    }

    public void setKind(String kind) {
        setAttribute(KIND_ATTR, kind);
    }

    public boolean isEditable() {
        return getAttributeAsBoolean(EDITABLE_ATTR, true);
    }

    public void setEditable(boolean editable) {
        if (editable) {
            setAttribute(EDITABLE_ATTR, null); // remove the setting
        } else {
            setAttribute(EDITABLE_ATTR, editable);
        }
    }

    public boolean isPublicAccess() {
        return getAttributeAsBoolean(ACCESS_ATTR, true);
    }

    public void setPublicAccess(boolean publicAccess) {
        if (publicAccess) {
            setAttribute(ACCESS_ATTR, null); // remove the setting
        } else {
            setAttribute(ACCESS_ATTR, publicAccess);
        }
    }

    public int getSequence() {
        return getAttributeAsInt(SEQUENCE_ATTR, -1);
    }

    public void setSequence(int sequence) {
        setAttribute(SEQUENCE_ATTR, sequence);
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


    // -----------------------------------------------------------------------
    // Handle IDs and references
    // -----------------------------------------------------------------------

    public String getId() {
        return getAttribute(ID_ATTR);
    }

    public void setId(String id) {
        setAttribute(ID_ATTR, id);
        setAttribute(REF_ATTR, null);
    }

    public List getReferences() {
        // Get this object's id, if there is one.  Otherwise, there are no
        // references.
        String id = getId();
        if (id == null) return Collections.EMPTY_LIST;

        // Create the XPath expression that will return the referring
        // ParamSetElements.
        String xpathStr = "//paramset[@ref='" + id + "']";

        // This xpath only works for some reason if the referent and reference
        // are part of a dom4j Document.  So if they aren't already part of a
        // Document, temporarily add them to one.  This is a bit of a hack I
        // suppose.
        Element e = getElement();
        Element root = null;
        org.dom4j.Document doc = e.getDocument();
        if (doc == null) {
            root = ((PioNodeImpl) getRootNode()).getElement();
            doc = PioXmlDocumentFactory.INSTANCE.createDocument(root);
        }

        try {
            List elementList = e.selectNodes(xpathStr);
            if ((elementList == null) || (elementList.size() == 0)) {
                return Collections.EMPTY_LIST;
            }

            // Convert the List of ParamSetElement into ParamSets
            List res = new ArrayList(elementList.size());
            for (Iterator it=elementList.iterator(); it.hasNext(); ) {
                res.add(((ParamSetElement) it.next()).getParamSet());
            }
            return res;

        } finally {
            // If we temporarily wrapped the root in a dom4j document, then
            // take it back out.
            if (root != null) doc.remove(root);
        }
    }

    public String getReferenceId() {
        return getAttribute(REF_ATTR);
    }

    public void setReferenceId(String id) {
        setAttribute(REF_ATTR, id);

        // If this ParamSet had an ID, remove it now.
        setAttribute(ID_ATTR, null);

        // If this ParamSet had children, remove them.
        Element e = getElement();
        List childElements = e.elements();
        if ((childElements == null) || (childElements.size() == 0)) return;
        for (Iterator it=childElements.iterator(); it.hasNext(); ) {
            Element child = (Element) it.next();
            e.remove(child);
        }
    }

    public ParamSet getReferent() {
        // Get the reference id, if there is one.  Otherwise, there is no
        // referent.
        String refid = getReferenceId();
        if (refid == null) return null;

        // Create the XPath expression that will extract the ParamSet with
        // this id.
        String xpathStr = "//paramset[@id='" + refid + "']";

        // This xpath only works for some reason if the referent and reference
        // are part of a dom4j Document.  So if they aren't already part of a
        // Document, temporarily add them to one.  This is a bit of a hack I
        // suppose.
        Element e = getElement();
        Element root = null;
        org.dom4j.Document doc = e.getDocument();
        if (doc == null) {
            root = ((PioNodeImpl) getRootNode()).getElement();
            doc = PioXmlDocumentFactory.INSTANCE.createDocument(root);
        }

        // Lookup the referent.
        try {
            ParamSetElement res = (ParamSetElement) e.selectSingleNode(xpathStr);
            if (res == null) return null;
            return res.getParamSet();
        } finally {
            // If we temporarily wrapped the root in a dom4j document, then
            // take it back out.
            if (root != null) doc.remove(root);
        }
    }

    /**
     * A helper method that does one of the following:
     *
     * <ul>
     * <li>if there is no {@link #getReferenceId reference id}, then returns
     * <code>null</code></li>
     * <li>if there is a reference id, and the ParamSet that it refers to is
     * not found, throws a {@link PioReferenceException}</li>
     * <li>if there is a reference id, and the ParamSet that it refers to is
     * found, then it is returned</li>
     * </ul>
     */
    private ParamSetImpl _lookupReferent() {
        String refId = getReferenceId();
        if (refId == null) return null;

        ParamSetImpl referent = (ParamSetImpl) getReferent();
        if (referent == null) {
            throw new PioReferenceException("Could not find '" + refId + "'");
        }
        return referent;
    }

    // ------------------------------------------------------------------------
    // Override the implementation of the methods of PioNodeParent to handle
    // references.
    // ------------------------------------------------------------------------

    public int getChildCount() {
        int count;
        ParamSet referent = _lookupReferent();
        if (referent == null) {
            count = super.getChildCount();
        } else {
            count = referent.getChildCount();
        }
        return count;
    }

    protected int getChildCount(String elementName) {
        int count;
        ParamSetImpl referent = _lookupReferent();
        if (referent == null) {
            count = super.getChildCount(elementName);
        } else {
            count = referent.getChildCount(elementName);
        }
        return count;
    }

    public List getChildren() {
        List children;
        ParamSet referent = _lookupReferent();
        if (referent == null) {
            children = super.getChildren();
        } else {
            children = referent.getChildren();
        }
        return children;
    }

    protected List getChildren(String elementName) {
        List children;
        ParamSetImpl referent = _lookupReferent();
        if (referent == null) {
            children = super.getChildren(elementName);
        } else {
            children = referent.getChildren(elementName);
        }
        return children;
    }

    protected List getChildren(String elementName, String name) {
        List children;
        ParamSetImpl referent = _lookupReferent();
        if (referent == null) {
            children = super.getChildren(elementName, name);
        } else {
            children = referent.getChildren(elementName, name);
        }
        return children;
    }

    public PioNamedNode getChild(String name) {
        PioNamedNode child;
        ParamSet referent = _lookupReferent();
        if (referent == null) {
            child = super.getChild(name);
        } else {
            child = referent.getChild(name);
        }
        return child;
    }

    protected PioNamedNode getChild(String elementName, String name) {
        PioNamedNode child;
        ParamSetImpl referent = _lookupReferent();
        if (referent == null) {
            child = super.getChild(elementName, name);
        } else {
            child = referent.getChild(elementName, name);
        }
        return child;
    }

    public boolean removeChild(PioNode child) {
        boolean res;
        ParamSet referent = _lookupReferent();
        if (referent == null) {
            res = super.removeChild(child);
        } else {
            res = referent.removeChild(child);
        }
        return res;
    }

    public PioNamedNode removeChild(String name) {
        PioNamedNode child;
        ParamSet referent = _lookupReferent();
        if (referent == null) {
            child = super.removeChild(name);
        } else {
            child = referent.removeChild(name);
        }
        return child;
    }

    protected void addChild(PioNodeImpl child) {
        ParamSetImpl referent = _lookupReferent();
        if (referent == null) {
            super.addChild(child);
        } else {
            referent.addChild(child);
        }
    }


    // ------------------------------------------------------------------------
    // Implements the ParamSet method that deal with specific Param and
    // ParamSet child types.
    // ------------------------------------------------------------------------

    public int getParamCount() {
        return getChildCount(ParamElement.NAME);
    }

    public int getParamSetCount() {
        return getChildCount(ParamSetElement.NAME);
    }



    public List getParams() {
        return getChildren(ParamElement.NAME);
    }

    public List getParamSets() {
        return getChildren(ParamSetElement.NAME);
    }



    public List getParams(String name) {
        return getChildren(ParamElement.NAME,  name);
    }

    public List getParamSets(String name) {
        return getChildren(ParamSetElement.NAME,  name);
    }



    public void addParam(Param child) {
        addChild((PioNodeImpl) child);
    }

    public void addParamSet(ParamSet child) {
        addChild((PioNodeImpl) child);
    }



    public Param getParam(String name) {
        return (Param) getChild(ParamElement.NAME, name);
    }

    public ParamSet getParamSet(String name) {
        return (ParamSet) getChild(ParamSetElement.NAME,  name);
    }



    public Param lookupParam(PioPath path) {
        PioNode child = lookupNode(path);
        return (child instanceof Param) ? (Param) child : null;
    }

    public ParamSet lookupParamSet(PioPath path) {
        PioNode child = lookupNode(path);
        return (child instanceof ParamSet) ? (ParamSet) child : null;
    }
}
