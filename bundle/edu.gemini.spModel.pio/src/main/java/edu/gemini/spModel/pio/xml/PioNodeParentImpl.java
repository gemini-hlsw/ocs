//
// $Id: PioNodeParentImpl.java 6549 2005-08-17 16:13:16Z shane $
//
package edu.gemini.spModel.pio.xml;

import edu.gemini.spModel.pio.PioNamedNode;
import edu.gemini.spModel.pio.PioNode;
import edu.gemini.spModel.pio.PioNodeParent;
import edu.gemini.spModel.pio.PioPath;
import org.dom4j.Element;
import org.dom4j.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * Base class for PIO object implementation classes.
 */
abstract class PioNodeParentImpl extends PioNodeImpl implements PioNodeParent {

    protected PioNodeParentImpl(PioNodeElement element) {
        super(element);
    }

    /**
     * Gets the {@link PioNode}(s) that match the given xpath.  This method
     * retrieves the backing JDOM Element for this node and uses it as the
     * context for the xpath query.  The results of the query (which are
     * themselves Elements) are used to obtain the corresponding
     * {@link PioNode}s.
     *
     * @param xpath X Path expression to use in locating nodes
     *
     * @return List of {@link PioNodeImpl} subclasses whose corresponding
     * JDOM Elements match the given xpath; if nothing matches an empty
     * List
     */
    protected List<PioNode> selectNodesByXpath(String xpath) {
        final List nodes = getElement().selectNodes(xpath);
        if ((nodes == null) || (nodes.size() == 0)) return Collections.EMPTY_LIST;

        List<PioNode> res = new ArrayList<>();
        for (Iterator it=nodes.iterator(); it.hasNext(); ) {
            Object el = it.next();
            if (el instanceof PioNodeElement) {
                res.add(((PioNodeElement) el).getPioNode());
            }
        }
        return res;
    }

    /**
     * Gets the number that matches the given xpath. This method retrieves the
     * backing JDOM Element for this node and uses it as the context for the
     * xpath query.  The results of the query is expected to be a Number, whose
     * int value is returned (or else a ClassCastException will be thrown).
     *
     * @param xpath X Path expression to use
     */
    protected int selectCountByXpath(String xpath) {
        Number count = (Number) getElement().selectObject(xpath);
        return count.intValue();
    }

    /**
     * Gets the first (or only) {@link PioNode} that matches the given xpath.
     * This method retrieves the backing JDOM Element for this node and uses it
     * as the context for the xpath query.  The results of the query (which are
     * themselves Elements) are used to obtain the corresponding
     * {@link PioNode}.
     *
     * @param xpath X Path expression to use in locating nodes
     *
     * @return {@link PioNodeImpl} subclass whose corresponding JDOM Element
     * matches the given xpath; <code>null</code> if nothing matches
     */
    protected PioNode selectSingleNodeByXpath(String xpath) {
        Node node = getElement().selectSingleNode(xpath);
        if (node instanceof PioNodeElement) {
            return ((PioNodeElement) node).getPioNode();
        }
        return null;
    }


    public int getChildCount() {
        // Unfortunately this doesn't work.  It includes things that are not
        // Elements, like org.dom4j.Namespace.
        //Number count = (Number) getElement().selectObject("count(*)");
        //return count.intValue();

        List elements = getElement().elements();
        int count = 0;
        for (Iterator it=elements.iterator(); it.hasNext(); ) {
            Element e = (Element) it.next();
            if (e instanceof PioNodeElement) ++count;
        }

        return count;
    }

    public List<PioNode> getChildren() {
        return selectNodesByXpath("*");
    }

    public PioNamedNode getChild(String name) {
        return (PioNamedNode) selectSingleNodeByXpath("*[@name='" + name + "']");
    }

    public PioNode lookupNode(PioPath path) {
        // Get the context Element for this path.  The context will be the
        // Element associated with this node if the path is relative.
        // Otherwise it is the root of the tree containing this node.
        PioNodeElement ctx = getElement();
        if (path.isAbsolute()) {
            ctx = ((PioNodeImpl) getRootNode()).getElement();
        }

        // First try a straightforward xpath search.  This might fail though,
        // because of ParamSets that reference other ParamSets.  If such a
        // ParamSet is included in the path, then the xpath search will fail
        // because in the XML, a ParamSet that references another ParamSets
        // does not have any children.
        String xpath = getXPath(path);
        Element res = (Element) ctx.selectSingleNode(xpath);
        if ((res != null) && (res instanceof PioNodeElement)) {
            return ((PioNodeElement) res).getPioNode();
        }

        // Okay, try a more straightforward search.  This will resolve
        // references.
        PioNodeParent parent = (PioNodeParent) ctx.getPioNode();
        String[] names = path.split();
        return _lookupNode(parent, names, 0);
    }

    private PioNode _lookupNode(PioNode parent, String[] names, int index) {
        if (index >= names.length) return parent; // resolved the last name
        if (!(parent instanceof PioNodeParentImpl)) {
            // parent is null or not a PioNodeParent -- either way we didn't
            // find the node since there are more path elements left
            return null;
        }

        PioNodeParentImpl pimpl = (PioNodeParentImpl) parent;

        String name = names[index];
        if (!PioPath.anonymousContainer.equals(name)) {
            PioNode child = pimpl.getChild(name);
            return _lookupNode(child, names, ++index);
        }

        // We have an anonymous Container.  Find all the containers in this
        // parent that don't have names, and search each one of them.
        String xpath = ContainerElement.NAME + "[not(@name)]";
        List anonymousContainerList = pimpl.selectNodesByXpath(xpath);
        if ((anonymousContainerList == null) || (anonymousContainerList.size() == 0)) {
            return null; // no anonymous containers in this parent
        }

        ++index;
        for (Iterator it=anonymousContainerList.iterator(); it.hasNext(); ) {
            PioNode child = (PioNode) it.next();
            PioNode res   = _lookupNode(child, names, index);
            if (res != null) return res;
        }

        // Node not found
        return null;
    }

    public boolean removeChild(PioNode child) {
        Element childElement = ((PioNodeImpl) child).getElement();
        return getElement().remove(childElement);
    }

    public PioNamedNode removeChild(String name) {
        PioNamedNode child = getChild(name);
        if (child == null) return null;

        Element childElement = ((PioNodeImpl) child).getElement();
        childElement.detach();

        return child;
    }


    /**
     * Gets the count of a particular type of child.
     *
     * @param elementName name of the element for the children to count, for
     * example <code>paramset</code> or <code>param</code>
     *
     * @return count of the subset of all the children of this node that are of
     * the type indicated by <code>elementName</code>
     */
    protected int getChildCount(String elementName) {
        return selectCountByXpath("count("+ elementName + ")");
    }

    /**
     * Gets the children of a particular type of child.
     *
     * @param elementName name of the element for the children to count, for
     * example <code>paramset</code> or <code>param</code>
     *
     * @return subset of all the children of this node that are of the type
     * indicated by <code>elementName</code>
     */
    protected List<PioNode> getChildren(String elementName) {
        return selectNodesByXpath(elementName);
    }

    /**
     * Gets all the children of this element of the particular type indicated
     * by the <code>elementName</code>, which have an name attribute with value
     * <code>name</code>.
     *
     * @param elementName name of the element we're interested in, for example
     * <code>param</code> or </code>paramset</code>
     * @param name name of the {@link PioNamedNode} of interest
     *
     * @param name value of the <code>name</code> name attribute of the element
     * whose node implementation will be returned
     *
     * @return the indicated node, or an empty list if there is no child
     * with element type <code>elementName</code> and name attribute
     * <code>name</code>
     */
    protected List<PioNode> getChildren(String elementName, String name) {
        return selectNodesByXpath(elementName + "[@name='" + name + "']");
    }

    /**
     * Gets the child of this element of the particular type indicated by the
     * <code>elementName</code>, which has an name attribute with value
     * <code>name</code>.
     *
     * @param elementName name of the element we're interested in, for example
     * <code>param</code> or </code>paramset</code>
     *
     * @param name value of the <code>name</code> name attribute of the element
     * whose node implementation will be returned
     *
     * @return the indicated node, or <code>null</code> if there is no child
     * with element type <code>elementName</code> and name attribute
     * <code>name</code>
     */
    protected PioNamedNode getChild(String elementName, String name) {
        return (PioNamedNode) selectSingleNodeByXpath(elementName + "[@name='" +
                                                    name + "']");
    }

    /**
     * Adds the given child to this element, removing it from where ever it may
     * currently be rooted. If child is null, nothing is added.
     *
     * @param child node to be added to this parent
     *
     * @throws IllegalArgumentException if child is an ancestor of this node
     */
    protected void addChild(PioNodeImpl child) {
        if (child == null) {
            return;
        }

        // Make sure that child is not an ancestor of this node
        Element childElement = child.getElement();
        Element e = getElement();
        while (e != null) {
            if (e == childElement) {
                throw new IllegalArgumentException("Inserting an ancestor node as a child!");
            }
            e = e.getParent();
        }

        // Get the element associated with the child, and detatch it in case
        // it is already contained in another part of the tree.
        childElement.detach();

        // Now add it to our element.
        getElement().add(childElement);
    }

}
